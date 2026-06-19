# RAG (Retrieval-Augmented Generation) Tizimi

**Loyiha:** QalqonAI (`api.ailawyer.uz`)  
**Texnologiya:** Spring Boot 3, Java 17, Hibernate 6, PostgreSQL + pgvector, Apache POI, Gemini API  
**Maqsad:** Huquqiy hujjatlarni vektor bazaga saqlab, foydalanuvchi savoliga mos moddalarni topish va AI javobiga kontekst sifatida berish.

---

## Qisqacha xulosa

QalqonAI uchun **RAG arxitekturasi** 5 bosqichda qurildi va **to'liq ishga tayyor**.

| Bosqich | Nomi | Holat |
|---------|------|-------|
| **1** | Ma'lumotlar qatlami | ✅ Bajarildi |
| **2** | Hujjat yuklash va parsing | ✅ Bajarildi |
| **3** | Aqlli yangilash (hash diff) | ✅ Bajarildi |
| **4** | Gemini embedding | ✅ Bajarildi |
| **5** | Vektor qidiruv + Hybrid RAG | ✅ Bajarildi |

**Hozirgi imkoniyatlar:**

- Admin `.docx` hujjat yuklaydi → modda/band bo'laklariga ajratiladi
- Hash diff faqat o'zgargan chunklarni yangilaydi
- Fonda Gemini `text-embedding-004` orqali 768 o'lchamli vektorlar yaratiladi
- Foydalanuvchi AI chat savolida **HNSW + cosine qidiruv** orqali eng yaqin 5 ta modda topiladi
- Topilgan matnlar **Hybrid Mode** system prompt orqali Gemini chat javobiga uzatiladi

---

## Umumiy oqim (1–5 bosqich)

```
Admin .docx yuklaydi (POST /upload)
        │
        ├─► docNumber YANGI?  → parse → barcha chunklar save (embedding=null)
        │
        └─► docNumber MAVJUD? → hash diff → faqat o'zgargan chunklar yangilanadi
        │
        ▼
HTTP 200 javob (sinxron tugaydi)
        │
        ▼  @Async — embeddingExecutor
DocumentEmbeddingProcessor.processEmbeddingsForDocument()
        │
        ├─► embedding IS NULL chunklarni topish
        ├─► har biri uchun GeminiEmbeddingService.getEmbedding()
        ├─► law_chunks.embedding ga yozish (vector 768)
        └─► 500ms kechikish (rate limit himoyasi)
        │
        ▼  Foydalanuvchi AI chat xabar yuboradi
AiMessageService.sendUserMessage()
        │
        ▼
GeminiAiProviderImpl.generate()
        │
        ├─► LegalSearchService.searchRelevantContext(savol, 5)
        │       ├─► savolni vektorlaydi (Gemini embedding)
        │       ├─► pgvector cosine qidiruv (HNSW indeks)
        │       └─► faqat ACTIVE / PARTIALLY_AMENDED hujjatlar
        │
        ├─► top-5 chunk → kontekst matni
        ├─► Hybrid system prompt (o'zbekcha) yig'iladi
        └─► Gemini chat API → javob (ragUsed=true agar chunk topilsa)
```

---

## Dizayn qarorlari

| # | Qaror | Tavsif |
|---|-------|--------|
| 1 | Fayl formati `.docx` | PDF emas; Apache POI (`poi-ooxml`) bilan parsing |
| 2 | Ierarxik parsing | CODE/LAW → `-modda`; qarorlar → `-bob` + `-band` |
| 3 | Hash-based diffing | Faqat o'zgargan chunklar yangilanadi; embedding saqlanadi |
| 4 | Async embedding | HTTP javob embedding tugashini kutmaydi |
| 5 | Embedding model | Gemini `text-embedding-004`, 768 o'lcham, pgvector |
| 6 | HNSW indeks | ENN (Exact Nearest Neighbor) bottleneck oldini olish |
| 7 | Hybrid RAG | Avval bazadagi matn, yetmasa umumiy bilim + ogohlantirish |
| 8 | Status filtri | Qidiruvda faqat `ACTIVE` va `PARTIALLY_AMENDED` hujjatlar |

---

# 1-BOSQICH: Ma'lumotlar qatlami ✅

## Dependencylar

```xml
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
    <version>0.1.6</version>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-vector</artifactId>
    <version>6.5.3.Final</version>
</dependency>
```

## Enumlar

- **`DocumentStatus`:** `ACTIVE`, `PARTIALLY_AMENDED`, `SUPERSEDED`, `REVOKED`
- **`DocumentType`:** `CODE`, `LAW`, `PRESIDENTIAL_DECREE`, `PRESIDENTIAL_RESOLUTION`, `CABINET_RESOLUTION`, `MINISTRY_ORDER`, `OTHER`

## Jadvallar

### `legal_documents`

Hujjat metadata: `id`, `type`, `doc_number`, `doc_date`, `title`, `status`, `superseded_by_id`, `created_at`, `updated_at`

### `law_chunks`

Chunk + vektor: `id`, `document_id`, `article_ref`, `content`, `text_hash`, `embedding vector(768)`

```java
@Column(name = "embedding")
@JdbcTypeCode(SqlTypes.VECTOR)
@Array(length = 768)
private float[] embedding;
```

## PostgreSQL

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

## Repositorylar

| Repository | Asosiy metodlar |
|------------|-----------------|
| `LegalDocumentRepository` | `findByDocNumber`, `findAllByStatus` |
| `LawChunkRepository` | `findAllByDocumentIdOrderByArticleRefAsc`, `findByDocumentIdAndArticleRef`, `findAllByDocumentIdAndEmbeddingIsNull`, `deleteAllByDocumentId`, `findSimilarChunks` *(5-bosqich)* |

---

# 2-BOSQICH: Hujjat yuklash va parsing ✅

## Dependency

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

## Asosiy komponentlar

| Komponent | Vazifa |
|-----------|--------|
| `LegalDocumentParsingService` | `.docx` ierarxik parsing, SHA-256 |
| `LegalDocumentAdminController` | `POST /api/v1/admin/legal-documents/upload` |
| `LegalDocumentUploadDTO` | Upload form maydonlari |

## REST API

```
POST /api/v1/admin/legal-documents/upload
Content-Type: multipart/form-data
Ruxsat: ROLE_ADMIN, ROLE_SUPERADMIN
```

Parametrlar: `file`, `type`, `docNumber`, `docDate`, `title`, `supersededById`

## Ierarxik parsing

| Hujjat turi | Bo'linish | `articleRef` misoli |
|-------------|-----------|---------------------|
| `CODE`, `LAW` | `-modda` | `15-modda` |
| Qaror/farmon | `-BOB` + `-band` | `1-bob, 3-band` |

Bob konteksti chunk boshiga qo'shiladi. Jadvallar Markdown formatida saqlanadi.

---

# 3-BOSQICH: Hash-Based Diffing ✅

## Komponent

`LegalDocumentDiffService.processDocumentUpdate(existingDoc, newlyParsedChunks)`

| Holat | Harakat |
|-------|---------|
| Hash bir xil | Tegilmaydi (embedding saqlanadi) |
| Hash farqli | `content` yangilanadi, `embedding = null` |
| Yangi `articleRef` | Yangi chunk qo'shiladi |
| Eski mapda qolgan | O'chiriladi |

O'zgarish bo'lsa → `DocumentStatus.PARTIALLY_AMENDED`

Bir xil `docNumber` bilan qayta yuklash xato emas — aqlli yangilash rejimida ishlaydi.

---

# 4-BOSQICH: Gemini Embedding ✅

## Konfiguratsiya (`application.properties`)

```properties
gemini.api.key=YOUR_API_KEY
gemini.embedding.model=text-embedding-004
gemini.embedding.delay-ms=500
```

| Property | Default | Tavsif |
|----------|---------|--------|
| `gemini.api.key` | — | Gemini API kaliti (chat va embedding uchun umumiy) |
| `gemini.embedding.model` | `text-embedding-004` | Embedding model nomi |
| `gemini.embedding.delay-ms` | `500` | So'rovlar orasidagi kechikish (ms) |

## Yangi fayllar

| Fayl | Vazifa |
|------|--------|
| `service/GeminiEmbeddingService.java` | Gemini API chaqiruvi, `float[]` qaytarish |
| `service/DocumentEmbeddingProcessor.java` | `@Async` batch embedding |
| `exps/GeminiApiException.java` | Gemini API xatolari |

## O'zgartirilgan fayllar

| Fayl | O'zgarish |
|------|-----------|
| `LegalDocumentService.upload()` | Javobdan oldin `processEmbeddingsForDocument()` trigger |
| `LawChunkRepository` | `findAllByDocumentIdAndEmbeddingIsNull()` |
| `AsyncConfig` | `embeddingExecutor` thread pool |
| `application.properties` | Embedding propertylar |

## `GeminiEmbeddingService`

```java
public float[] getEmbedding(String text)
```

```
POST https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key={API_KEY}
```

## `DocumentEmbeddingProcessor`

```java
@Async("embeddingExecutor")
public void processEmbeddingsForDocument(UUID documentId)
```

Algoritm: `embedding IS NULL` chunklar → Gemini → save → 500ms delay.

## Async konfiguratsiya

| Bean | Thread prefix | Vazifa |
|------|---------------|--------|
| `notificationExecutor` | `notification-` | FCM push |
| `auditExecutor` | `audit-` | Audit log |
| `embeddingExecutor` | `embedding-` | Gemini embedding |

---

# 5-BOSQICH: Vektor qidiruv va Hybrid RAG ✅

Bu bosqichda RAG tizimining **qidiruv (retrieval)** va **AI kontekst (injection)** qatlamlari qo'shildi.

## 5.1. Maqsad

1. PostgreSQL da **HNSW indeks** orqali vektor qidiruvni tezlashtirish
2. Foydalanuvchi savolini vektorlab, **cosine similarity** bilan eng yaqin moddalarni topish
3. Faqat **`ACTIVE`** va **`PARTIALLY_AMENDED`** hujjatlardan qidirish
4. Topilgan matnlarni Gemini chat **Hybrid Mode** system promptiga qo'shish

---

## 5.2. Yangi fayllar

| Fayl | Paket | Vazifa |
|------|-------|--------|
| `RagDatabaseConfig.java` | `config` | HNSW indeksni ilova ishga tushganda yaratish |
| `LawChunkRepositoryCustom.java` | `repository` | Custom fragment interfeysi (`findSimilarChunks`) |
| `LawChunkRepositoryImpl.java` | `repository` | Native SQL cosine qidiruv (pgvector binding) |
| `PgVectorUtils.java` | `util` | `float[]` → PostgreSQL vector literal `[0.1,0.2,...]` |
| `LegalSearchService.java` | `service` | Savol embedding + qidiruv orkestratsiyasi |
| `LegalSearchServiceTest.java` | `test/service` | Unit testlar |

## 5.3. O'zgartirilgan fayllar

| Fayl | O'zgarish |
|------|-----------|
| `LawChunkRepository.java` | `LawChunkRepositoryCustom` dan meros; `findSimilarChunks()` qo'shildi |
| `GeminiAiProviderImpl.java` | `LegalSearchService` inject; Hybrid RAG system prompt; `ragUsed` flag |
| `GeminiAiProviderImplIntegrationTest.java` | `@MockBean LegalSearchService` — embedding/qidiruvni mock qiladi |

---

## 5.4. HNSW indeks — `RagDatabaseConfig`

Ilova ishga tushganda `@PostConstruct` + `JdbcTemplate` orqali xavfsiz bajariladi (Flyway migratsiyasi talab qilinmaydi):

```sql
CREATE INDEX IF NOT EXISTS law_chunks_embedding_hnsw_idx
ON law_chunks USING hnsw (embedding vector_cosine_ops);
```

| Xususiyat | Tavsif |
|-----------|--------|
| Indeks nomi | `law_chunks_embedding_hnsw_idx` |
| Algoritm | HNSW (Hierarchical Navigable Small World) |
| Masofa metrikasi | `vector_cosine_ops` (cosine distance `<=>`) |
| Maqsad | Katta hajmdagi vektor bazada ENN bottleneck oldini olish |
| Xato holati | pgvector yo'q yoki jadval mavjud emas → `log.warn`, ilova ishga tushadi |

---

## 5.5. Native cosine qidiruv — `LawChunkRepository`

### Metod imzosi

```java
List<LawChunkEntity> findSimilarChunks(float[] queryVector, int topK);
```

### SQL (native query)

```sql
SELECT c.*
FROM law_chunks c
JOIN legal_documents d ON c.document_id = d.id
WHERE d.status IN ('ACTIVE', 'PARTIALLY_AMENDED')
  AND c.embedding IS NOT NULL
ORDER BY c.embedding <=> CAST(:queryVector AS vector)
LIMIT :topK
```

| Filtr | Sabab |
|-------|-------|
| `d.status IN ('ACTIVE', 'PARTIALLY_AMENDED')` | Faqat amalda yoki qisman o'zgartirilgan hujjatlar |
| `c.embedding IS NOT NULL` | Embedding hali yaratilmagan chunklar qidiruvdan chiqariladi |
| `ORDER BY ... <=>` | pgvector **cosine distance** (kichikroq = o'xshashroq) |
| `LIMIT :topK` | Eng yaqin N ta natija |

### pgvector parametr bog'lanishi

Spring Data `@Query` bilan `float[]` parametrini to'g'ridan-to'g'ri PostgreSQL `vector` tipiga bog'lay olmaydi. Shuning uchun **Spring Data custom fragment** ishlatiladi:

```
LawChunkRepository
    └── extends LawChunkRepositoryCustom
            └── LawChunkRepositoryImpl  (EntityManager + native SQL)
                    └── PgVectorUtils.toVectorLiteral(float[])
```

`PgVectorUtils` vektorni `[0.012,-0.034,...]` formatiga aylantiradi, so'ngra `CAST(:queryVector AS vector)` ishlaydi.

---

## 5.6. Qidiruv servisi — `LegalSearchService`

```java
@Service
public class LegalSearchService {

    public List<LawChunkEntity> searchRelevantContext(String userQuestion, int topK);
}
```

### Algoritm

```
1. userQuestion null/bo'sh?  → Collections.emptyList()
2. vector = geminiEmbeddingService.getEmbedding(userQuestion)
3. vector null/bo'sh?        → Collections.emptyList()
4. return lawChunkRepository.findSimilarChunks(vector, topK)
```

| Kirish | Chiqish |
|--------|---------|
| Foydalanuvchi savoli (matn) | Eng yaqin `topK` ta `LawChunkEntity` ro'yxati |
| `topK = 5` (AI chatda) | Hybrid prompt uchun 5 ta modda |

---

## 5.7. Hybrid RAG — `GeminiAiProviderImpl`

Har bir `AiProvider.generate()` chaqiruvida quyidagi ketma-ketlik bajariladi:

```
1. extractUserMessage(request)
       → request.prompt yoki history dagi oxirgi user xabari

2. legalSearchService.searchRelevantContext(userMessage, 5)
       → top-5 LawChunkEntity

3. buildContextString(chunks)
       → "[{Hujjat sarlavhasi}, {Modda}]: {Matn}\n"

4. buildHybridSystemPrompt(contextString)
       → o'zbekcha Hybrid Mode system prompt

5. buildRequestBody(request, hybridSystemPrompt)
       → Gemini API systemInstruction ga yuboriladi

6. aiResponse.setRagUsed(!ragChunks.isEmpty())
```

### Kontekst formati

```
[O'zbekiston Respublikasi Jinoyat kodeksi, 104-modda]: Modda matni...
[VMQ-370, 12-modda]: Boshqa modda matni...
```

Hujjat sarlavhasi `LegalDocumentRepository.findAllById()` orqali batch yuklanadi.

### Hybrid system prompt (o'zbekcha)

```
Sen QalqonAI - O'zbekiston yuridik maslahatchisisan. Foydalanuvchi savoliga birinchi navbatda mana bu matnlarga tayanib javob ber:
{KontekstMatni}

Agar bu matnlarda savolga to'liq javob bo'lmasa, o'zingning umumiy huquqiy bilimlaring asosida javob ber, lekin javobingning eng oxirida albatta ushbu ogohlantirishni qo'shib qo'y: 'Bu ma'lumot umumiy xarakterga ega, aniq yuridik harakat qilishdan oldin advokat bilan maslahatlashing yoki tizimga yangi qonunlar yuklanishini kuting.'
```

| Hybrid Mode xususiyati | Tavsif |
|------------------------|--------|
| **Retrieval-first** | Avval bazadagi qonun matnlariga tayanadi |
| **Fallback** | Matnda javob yo'q bo'lsa, umumiy huquqiy bilim ishlatiladi |
| **Majburiy disclaimer** | Fallback holatda javob oxirida ogohlantirish qo'shiladi |
| **Kontekst bo'sh bo'lsa ham** | Hybrid prompt yuboriladi (faqat ogohlantirish bilan fallback) |

### `AiResponse.ragUsed`

| Qiymat | Ma'nosi |
|--------|---------|
| `true` | Kamida 1 ta chunk topildi va kontekstga qo'shildi |
| `false` | Hech qanday chunk topilmadi (embedding yo'q yoki bazada mos matn yo'q) |

---

## 5.8. AI chat oqimi (5-bosqich bilan)

```
Foydalanuvchi → POST /api/v1/ai-chats/{id}/messages
        │
        ▼
AiMessageService.sendUserMessage()
        ├─► user xabar saqlanadi
        ├─► oxirgi 6 ta xabar history yig'iladi
        └─► aiProvider.generate(AiRequest)
                │
                ▼
        GeminiAiProviderImpl.generate()
                ├─► LegalSearchService (embedding + qidiruv)
                ├─► Hybrid system prompt
                └─► Gemini REST API
                        │
                        ▼
                AI javob saqlanadi (ragUsed metadata bilan)
```

---

## 5.9. Unit testlar (5-bosqich)

### `LegalSearchServiceTest`

| Test | Tekshiruv |
|------|-----------|
| `searchRelevantContext_returnsEmptyWhenQuestionBlank` | Bo'sh savol → embedding chaqirilmaydi |
| `searchRelevantContext_returnsEmptyWhenEmbeddingEmpty` | Bo'sh vektor → repository chaqirilmaydi |
| `searchRelevantContext_delegatesToRepositoryWhenEmbeddingPresent` | Vektor bor → `findSimilarChunks(vector, topK)` |
| `searchRelevantContext_returnsEmptyListFromRepository` | Repository bo'sh ro'yxat qaytarsa |

### `GeminiAiProviderImplIntegrationTest`

| O'zgarish | Sabab |
|-----------|-------|
| `@MockBean LegalSearchService` | Integration testda haqiqiy embedding/qidiruv chaqirilmasin |
| `when(...).thenReturn(emptyList())` | Chat API ni mustaqil tekshirish |

---

# Entity diagrammasi

```
legal_documents (1) ──────< (N) law_chunks
        │                         │
        │ status                  ├─ text_hash   → diff kaliti
        │ (ACTIVE,                └─ embedding   → vector(768)
        │  PARTIALLY_AMENDED,              │
        │  SUPERSEDED, REVOKED)           └─ HNSW indeks (cosine)
        │
        └─ RAG qidiruvda faqat ACTIVE va PARTIALLY_AMENDED ishtirok etadi
```

---

# Fayl tuzilmasi (to'liq)

```
api.ailawyer.uz/
├── pom.xml
└── src/
    ├── main/java/api/ailawyer/uz/
    │   ├── ai/
    │   │   └── GeminiAiProviderImpl.java             [5-bosqich — Hybrid RAG]
    │   ├── config/
    │   │   ├── AsyncConfig.java                      [4-bosqich — embeddingExecutor]
    │   │   └── RagDatabaseConfig.java                [5-bosqich — HNSW indeks]
    │   ├── controller/
    │   │   └── LegalDocumentAdminController.java     [2-bosqich]
    │   ├── dto/legal/
    │   │   ├── LegalDocumentUploadDTO.java           [2-bosqich]
    │   │   └── LegalDocumentUploadResponseDTO.java   [2-bosqich]
    │   ├── enums/
    │   │   ├── DocumentStatus.java                   [1-bosqich]
    │   │   └── DocumentType.java                     [1-bosqich]
    │   ├── entity/
    │   │   ├── LegalDocumentEntity.java              [1-bosqich]
    │   │   └── LawChunkEntity.java                   [1-bosqich]
    │   ├── exps/
    │   │   └── GeminiApiException.java               [4-bosqich]
    │   ├── repository/
    │   │   ├── LegalDocumentRepository.java          [1-bosqich]
    │   │   ├── LawChunkRepository.java               [1, 4, 5-bosqich]
    │   │   ├── LawChunkRepositoryCustom.java         [5-bosqich]
    │   │   └── LawChunkRepositoryImpl.java           [5-bosqich]
    │   ├── service/
    │   │   ├── LegalDocumentParsingService.java      [2-bosqich]
    │   │   ├── LegalDocumentDiffService.java         [3-bosqich]
    │   │   ├── LegalDocumentService.java             [2–4-bosqich]
    │   │   ├── GeminiEmbeddingService.java           [4-bosqich]
    │   │   ├── DocumentEmbeddingProcessor.java       [4-bosqich]
    │   │   └── LegalSearchService.java               [5-bosqich]
    │   └── util/
    │       └── PgVectorUtils.java                    [5-bosqich]
    └── test/java/api/ailawyer/uz/
        ├── ai/
        │   └── GeminiAiProviderImplIntegrationTest.java  [5-bosqichda yangilandi]
        └── service/
            ├── LegalDocumentParsingServiceTest.java    [2-bosqich]
            ├── LegalDocumentDiffServiceTest.java       [3-bosqich]
            ├── LegalDocumentServiceTest.java           [3–4-bosqich]
            ├── GeminiEmbeddingServiceTest.java         [4-bosqich]
            ├── DocumentEmbeddingProcessorTest.java     [4-bosqich]
            └── LegalSearchServiceTest.java             [5-bosqich]
```

---

# Tekshirish

## Kompilyatsiya va testlar

```bash
cd api.ailawyer.uz
./mvnw compile
./mvnw test
```

Kutilgan natija: **0 xato** (1–5-bosqichlar tasdiqlangan).

## PostgreSQL — embedding holati

```sql
-- Embedding kutilayotgan chunklar
SELECT d.doc_number, c.article_ref, c.text_hash
FROM law_chunks c
JOIN legal_documents d ON d.id = c.document_id
WHERE c.embedding IS NULL;

-- Embedding muvaffaqiyatli yozilgan chunklar
SELECT d.doc_number, COUNT(*) AS embedded_count
FROM law_chunks c
JOIN legal_documents d ON d.id = c.document_id
WHERE c.embedding IS NOT NULL
GROUP BY d.doc_number;

-- Vektor o'lchamini tekshirish
SELECT article_ref, vector_dims(embedding) AS dims
FROM law_chunks
WHERE embedding IS NOT NULL
LIMIT 5;
-- Kutilgan: dims = 768
```

## PostgreSQL — HNSW indeks (5-bosqich)

```sql
-- Indeks mavjudligini tekshirish
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'law_chunks'
  AND indexname = 'law_chunks_embedding_hnsw_idx';

-- Indeks ishlatilayotganini EXPLAIN bilan ko'rish
EXPLAIN ANALYZE
SELECT c.id
FROM law_chunks c
JOIN legal_documents d ON c.document_id = d.id
WHERE d.status IN ('ACTIVE', 'PARTIALLY_AMENDED')
  AND c.embedding IS NOT NULL
ORDER BY c.embedding <=> (SELECT embedding FROM law_chunks WHERE embedding IS NOT NULL LIMIT 1)
LIMIT 5;
-- Kutilgan: "Index Scan using law_chunks_embedding_hnsw_idx"
```

## Upload + embedding testi

1. `.docx` yuklang → HTTP 200 darhol qaytadi
2. Console da `Embedding boshlandi...` va `Embedding yakunlandi...` loglarini kuting
3. SQL da `embedding IS NOT NULL` chunklar soni oshganini tekshiring
4. Qayta yuklang (faqat 1 modda o'zgartirilgan) → faqat o'sha modda uchun yangi embedding chaqiriladi

## Hybrid RAG testi (5-bosqich)

1. Kamida bitta hujjat yuklangan va embedding tugagan bo'lsin
2. AI chat orqali huquqiy savol bering (masalan: modda mazmuni haqida)
3. Javob bazadagi matnga mos kelishini tekshiring
4. `AiResponse.ragUsed = true` bo'lsa — qidiruv ishlagan
5. Bazada mos matn bo'lmagan savolda — fallback javob + oxirida ogohlantirish bo'lishi kerak

---

# Hali amalga oshirilmagan funksiyalar

| Funksiya | Reja |
|----------|------|
| Hujjat holatini `REVOKED` ga o'zgartirish API | Keyinroq |
| Hujjatlar ro'yxati / admin qidiruv API | Keyinroq |
| RAG natijalarini audit logga yozish | Keyinroq |
| Embedding/qidiruv monitoring dashboard | Keyinroq |

---

# Xulosa

| Nima qilindi | Bosqich | Tafsilot |
|--------------|---------|----------|
| ✅ pgvector integratsiyasi | 1 | Maven + Hibernate vector mapping |
| ✅ Entity / enum / repository | 1 | `legal_documents`, `law_chunks` |
| ✅ Apache POI parsing | 2 | Ierarxik bob/modda/band |
| ✅ Admin upload API | 2 | `POST /upload` |
| ✅ Hash-based diffing | 3 | Faqat o'zgargan chunklar |
| ✅ `PARTIALLY_AMENDED` | 3 | O'zgarishda avtomatik |
| ✅ Gemini embedding | 4 | `text-embedding-004`, 768 o'lcham |
| ✅ Async batch processor | 4 | `@Async embeddingExecutor` |
| ✅ Upload trigger | 4 | HTTP bloklanmaydi |
| ✅ Rate limit himoyasi | 4 | 500ms delay |
| ✅ HNSW indeks | 5 | `law_chunks_embedding_hnsw_idx`, cosine ops |
| ✅ Cosine qidiruv | 5 | Native SQL, `<=>` operator |
| ✅ Status filtri | 5 | `ACTIVE`, `PARTIALLY_AMENDED` |
| ✅ `LegalSearchService` | 5 | Savol embedding + top-K retrieval |
| ✅ Hybrid RAG prompt | 5 | `GeminiAiProviderImpl`, o'zbekcha |
| ✅ `ragUsed` flag | 5 | `AiResponse` metadata |

**RAG tizimining barcha 5 bosqichi** — ma'lumotlar qatlami, parsing, diff, embedding, qidiruv va AI kontekst — **yakunlandi va ishga tayyor**.
