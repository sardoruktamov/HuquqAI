# BOSQICH 8 — RAG (Retrieval-Augmented Generation) Tizimi

**Loyiha:** QalqonAI (`api.ailawyer.uz`)  
**Texnologiya:** Spring Boot 3, Java 17, Hibernate 6, PostgreSQL + pgvector, Apache POI, Gemini Embedding API  
**Maqsad:** Huquqiy hujjatlarni (kodeks, qonun, VMQ va hokazo) vektor bazaga saqlab, foydalanuvchi savoliga mos moddalarni topish va AI javobiga kontekst sifatida berish.

---

## Qisqacha xulosa

QalqonAI uchun **RAG arxitekturasi** bosqichma-bosqich qurilmoqda.

| Bosqich | Holat |
|---------|-------|
| **1 — Ma'lumotlar qatlami** | ✅ Bajarildi |
| **2 — Hujjat yuklash va parsing** | ✅ Bajarildi |
| **3 — Aqlli yangilash (hash diff)** | ✅ Bajarildi |
| **4 — Gemini embedding** | ✅ Bajarildi |
| **5 — Vektor qidiruv + AI kontekst** | ⏳ Keyingi |

Hozirgacha admin `.docx` hujjat yuklay oladi. Tizim uni modda/band bo'laklariga ajratadi, hash diff qiladi va **fonda Gemini `text-embedding-004` orqali 768 o'lchamli vektorlarga aylantiradi**. Upload HTTP javobi embedding tugashini kutmaydi — jarayon `@Async` da ishlaydi.

---

## Umumiy oqim

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
        ▼  @Async — embeddingExecutor thread
DocumentEmbeddingProcessor.processEmbeddingsForDocument()
        │
        ├─► embedding IS NULL chunklarni topish
        ├─► har biri uchun GeminiEmbeddingService.getEmbedding()
        ├─► law_chunks.embedding ga yozish (vector 768)
        └─► 500ms kechikish (rate limit himoyasi)
        │
        ▼  [KEYINGI BOSQICH — 5]
pgvector cosine qidiruv → topilgan moddalar AI kontekstiga qo'shiladi
```

---

## Umumiy arxitektura rejasi (5 bosqich)

| Bosqich | Nomi | Holat | Tavsif |
|---------|------|-------|--------|
| **1** | Ma'lumotlar qatlami | ✅ **Bajarildi** | `legal_documents`, `law_chunks`, enumlar, repositorylar, pgvector |
| **2** | Hujjat yuklash qatlami | ✅ **Bajarildi** | Apache POI, ierarxik parsing, SHA-256 hash, admin upload API |
| **3** | Aqlli yangilash | ✅ **Bajarildi** | Hash-based diffing — faqat o'zgargan chunklar yangilanadi |
| **4** | AI integratsiyasi | ✅ **Bajarildi** | Gemini `text-embedding-004`, async batch embedding |
| **5** | Qidiruv qatlami | ⏳ Keyingi | Cosine similarity qidiruv, faqat `ACTIVE` hujjatlar, AI kontekst |

---

## Dizayn qarorlari

### 1. Fayl formati — `.docx` (PDF emas)

PDF matn emas, dizayn saqlaydi. Qonunlar **asosan `.docx` formatida** yuklanadi.

- **Kutubxona:** Apache POI (`poi-ooxml`)
- **Parsing:** `XWPFDocument` + `IBodyElement` bo'yicha stateful ierarxik parsing

### 2. Ierarxik parsing

| Hujjat turi | Bo'linish | `articleRef` misoli |
|-------------|-----------|---------------------|
| `CODE`, `LAW` | `-modda` | `15-modda` |
| Qaror/farmon | `-BOB` + `-band` | `1-bob, 3-band` |

Bob konteksti chunk boshiga qo'shiladi. Jadvallar Markdown formatida saqlanadi.

### 3. Hash-based diffing ✅

Qayta yuklashda faqat o'zgargan/yangi/o'chirilgan chunklar yangilanadi. Hash bir xil bo'lsa embedding saqlanadi.

### 4. Async embedding ✅

Embedding uzoq vaqt olishi va Gemini rate limit tufayli **HTTP javobdan keyin fonda** bajariladi. Faqat `embedding IS NULL` chunklar vektorlanadi — hash diff bilan birgalikda API xarajati minimal.

### 5. Embedding model

| Parametr | Qiymat |
|----------|--------|
| Model | Gemini `text-embedding-004` |
| O'lcham | **768** |
| Saqlash | PostgreSQL `pgvector` (`float[]` + `@JdbcTypeCode(SqlTypes.VECTOR)`) |
| HTTP client | `RestTemplate` (mavjud `AppConfig` bean) |

### 6. Mustaqil bekor qilish (keyinroq)

`DocumentStatus.REVOKED` bo'lgan hujjatlar RAG qidiruvidan chiqariladi (5-bosqich + status API).

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
| `LawChunkRepository` | `findAllByDocumentIdOrderByArticleRefAsc`, `findByDocumentIdAndArticleRef`, `findAllByDocumentIdAndEmbeddingIsNull`, `deleteAllByDocumentId` |

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

## Upload — mavjud hujjat

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

### Metod

```java
public float[] getEmbedding(String text)
```

### API chaqiruvi

```
POST https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key={API_KEY}
```

### Request body

```json
{
  "model": "models/text-embedding-004",
  "content": {
    "parts": [{ "text": "Modda matni..." }]
  }
}
```

### Response (parse qilinadigan qism)

```json
{
  "embedding": {
    "values": [0.012, -0.034, ...]
  }
}
```

### Xato holatlari

| Holat | Harakat |
|-------|---------|
| Matn bo'sh | `GeminiApiException` |
| API kalit yo'q | `GeminiApiException` |
| HTTP xato / rate limit | `GeminiApiException` + log |
| `embedding.values` yo'q | `GeminiApiException` |
| O'lcham ≠ 768 | Warning log, lekin davom etadi |

## `DocumentEmbeddingProcessor`

### Metod

```java
@Async("embeddingExecutor")
public void processEmbeddingsForDocument(UUID documentId)
```

### Algoritm

```
1. findAllByDocumentIdAndEmbeddingIsNull(documentId)
2. Har bir chunk uchun:
   a. geminiEmbeddingService.getEmbedding(content)
   b. chunk.setEmbedding(vector)
   c. lawChunkRepository.save(chunk)
   d. Thread.sleep(500ms)  — rate limit himoyasi
3. Xato bo'lsa — log, keyingi chunk ga o'tish
```

### Console log misollari

```
Embedding boshlandi documentId=a1b2..., pendingCount=142
Embedding yakunlandi documentId=a1b2..., success=140, failed=2
Embedding kerak emas documentId=a1b2...
```

## Async konfiguratsiya

`AsyncConfig` da `@EnableAsync` mavjud (notification va audit bilan birga).

| Bean | Thread prefix | Vazifa |
|------|---------------|--------|
| `notificationExecutor` | `notification-` | FCM push |
| `auditExecutor` | `audit-` | Audit log |
| `embeddingExecutor` | `embedding-` | Gemini embedding |

## Upload + embedding to'liq oqimi

```
LegalDocumentService.upload()  [@Transactional]
        │
        ├─► Yangi hujjat YOKI hash diff
        │
        ├─► HTTP response tayyorlanadi
        │
        └─► documentEmbeddingProcessor.processEmbeddingsForDocument(id)
                ↓ (async, HTTP allaqachon qaytgan)
            faqat embedding=null chunklar → Gemini → pgvector save
```

> **Muhim:** Hash diff tufayli o'zgarmagan moddalarda embedding saqlanadi — faqat yangi/o'zgargan moddalar uchun Gemini chaqiriladi.

## Unit testlar

### `GeminiEmbeddingServiceTest`

| Test | Tekshiruv |
|------|-----------|
| `getEmbedding_parses768DimensionalVector` | JSON javobdan `float[]` parse |
| `getEmbedding_throwsWhenTextBlank` | Bo'sh matn → exception |

### `DocumentEmbeddingProcessorTest`

| Test | Tekshiruv |
|------|-----------|
| `processEmbeddingsForDocument_embedsPendingChunks` | Muvaffaqiyatli embedding + save |
| `processEmbeddingsForDocument_skipsSaveWhenEmbeddingFails` | Xato bo'lsa save qilinmaydi |

### `LegalDocumentServiceTest`

| Test | Tekshiruv |
|------|-----------|
| `upload_existingDocumentUsesDiffServiceInsteadOfCreatingNew` | Upload dan keyin async trigger chaqiriladi |

---

## Entity diagrammasi

```
legal_documents (1) ──────< (N) law_chunks
                              │
                              ├─ text_hash   → diff kaliti
                              └─ embedding   → vector(768), Gemini orqali to'ldiriladi
```

---

## Fayl tuzilmasi (hozirgi holat)

```
api.ailawyer.uz/
├── pom.xml
└── src/
    ├── main/java/api/ailawyer/uz/
    │   ├── config/
    │   │   └── AsyncConfig.java                        [embeddingExecutor — 4-bosqich]
    │   ├── controller/
    │   │   └── LegalDocumentAdminController.java       [2-bosqich]
    │   ├── dto/legal/
    │   │   ├── LegalDocumentUploadDTO.java             [2-bosqich]
    │   │   └── LegalDocumentUploadResponseDTO.java       [2-bosqich]
    │   ├── enums/
    │   │   ├── DocumentStatus.java                     [1-bosqich]
    │   │   └── DocumentType.java                       [1-bosqich]
    │   ├── entity/
    │   │   ├── LegalDocumentEntity.java                [1-bosqich]
    │   │   └── LawChunkEntity.java                     [1-bosqich]
    │   ├── exps/
    │   │   └── GeminiApiException.java                 [YANGI — 4-bosqich]
    │   ├── repository/
    │   │   ├── LegalDocumentRepository.java            [1-bosqich]
    │   │   └── LawChunkRepository.java                 [1-bosqich, 4-bosqichda yangilandi]
    │   └── service/
    │       ├── LegalDocumentParsingService.java        [2-bosqich]
    │       ├── LegalDocumentDiffService.java           [3-bosqich]
    │       ├── LegalDocumentService.java               [2–4-bosqich]
    │       ├── GeminiEmbeddingService.java             [YANGI — 4-bosqich]
    │       └── DocumentEmbeddingProcessor.java         [YANGI — 4-bosqich]
    └── test/java/api/ailawyer/uz/service/
        ├── LegalDocumentParsingServiceTest.java        [2-bosqich]
        ├── LegalDocumentDiffServiceTest.java           [3-bosqich]
        ├── LegalDocumentServiceTest.java               [3–4-bosqich]
        ├── GeminiEmbeddingServiceTest.java             [YANGI — 4-bosqich]
        └── DocumentEmbeddingProcessorTest.java         [YANGI — 4-bosqich]
```

**Keyingi bosqichda yoziladi:**

```
service/
└── LegalSearchService.java             [5-bosqich — vektor qidiruv]
```

---

## Hali amalga oshirilmagan funksiyalar

| Funksiya | Bosqich |
|----------|---------|
| pgvector cosine qidiruv | 5 |
| AI suhbatiga kontekst qo'shish | 5 |
| Faqat `ACTIVE` hujjatlardan qidiruv | 5 |
| Hujjat holatini `REVOKED` ga o'zgartirish API | Keyinroq |
| Hujjatlar ro'yxati / qidiruv API | Keyinroq |

---

## Tekshirish

### Kompilyatsiya va testlar

```bash
cd api.ailawyer.uz
./mvnw compile
./mvnw test
```

Natija: **0 xato** (1–4-bosqichlar yakunlanganda tasdiqlangan).

### Embedding holatini tekshirish

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

-- Vektor o'lchamini tekshirish (pgvector)
SELECT article_ref, vector_dims(embedding) AS dims
FROM law_chunks
WHERE embedding IS NOT NULL
LIMIT 5;
-- Kutilgan: dims = 768
```

### Upload + embedding testi

1. `.docx` yuklang → HTTP 200 darhol qaytadi
2. Console da `Embedding boshlandi...` va `Embedding yakunlandi...` loglarini kuting
3. SQL da `embedding IS NOT NULL` chunklar soni oshganini tekshiring
4. Qayta yuklang (faqat 1 modda o'zgartirilgan) → faqat o'sha modda uchun yangi embedding chaqiriladi

---

## Keyingi bosqich (5-qadam) — reja

1. `LegalSearchService` — foydalanuvchi savolini vektorlaydi
2. pgvector cosine similarity (`<=>` yoki `cosine_distance`) bilan eng yaqin chunklarni topadi
3. Faqat `DocumentStatus.ACTIVE` (va `PARTIALLY_AMENDED`) hujjatlardan qidiradi
4. Topilgan modda matnlarini AI chat kontekstiga qo'shadi

---

## Xulosa

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
| ⏳ Vektor qidiruv | 5 | Keyingi qadam |
| ⏳ AI kontekst | 5 | Rejada |

RAG tizimining **ma'lumotlar, parsing, diff va embedding qatlamlari** tayyor. Keyingi qadam — pgvector orqali semantic qidiruv va AI ga kontekst berish.
