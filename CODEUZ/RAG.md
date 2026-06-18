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
| **3 — Aqlli yangilash (hash diff)** | ⏳ Keyingi |
| **4 — Gemini embedding** | ⏳ Rejada |
| **5 — Vektor qidiruv + AI kontekst** | ⏳ Rejada |

Hozirgacha admin `.docx` hujjat yuklay oladi, tizim uni ierarxik tarzda modda/band bo'laklariga ajratadi, SHA-256 hash hisoblaydi va `law_chunks` jadvaliga saqlaydi. **Embedding hali yozilmaydi** (`embedding = null`).

---

## Umumiy oqim

```
Admin .docx yuklaydi (POST /upload)
        │
        ▼
LegalDocumentEntity saqlanadi (legal_documents)
        │
        ▼
LegalDocumentParsingService — Apache POI + ierarxik parsing
        │
        ▼
LawChunkEntity[] yaratiladi (content + text_hash, embedding=null)
        │
        ▼
law_chunks jadvaliga saqlash
        │
        ▼  [KEYINGI BOSQICHLAR]
Hash diff → Gemini embedding → pgvector qidiruv → AI kontekst
```

---

## Umumiy arxitektura rejasi (5 bosqich)

| Bosqich | Nomi | Holat | Tavsif |
|---------|------|-------|--------|
| **1** | Ma'lumotlar qatlami | ✅ **Bajarildi** | `legal_documents`, `law_chunks`, enumlar, repositorylar, pgvector dependency |
| **2** | Hujjat yuklash qatlami | ✅ **Bajarildi** | Apache POI, ierarxik parsing, SHA-256 hash, admin upload API |
| **3** | Aqlli yangilash | ⏳ Keyingi | Hash-based diffing — faqat o'zgargan moddalarni qayta embedding qilish |
| **4** | AI integratsiyasi | ⏳ Rejada | Gemini `text-embedding-004` orqali vektorlash |
| **5** | Qidiruv qatlami | ⏳ Rejada | Cosine similarity qidiruv, faqat `ACTIVE` hujjatlar, AI kontekstiga ulash |

---

## Dizayn qarorlari

### 1. Fayl formati — `.docx` (PDF emas)

PDF matn emas, dizayn saqlaydi — moddalarni ajratish qiyin. Shuning uchun qonunlar **asosan `.docx` formatida** yuklanadi (Lex.uz dagi `.doc` yuklab olish odatiga mos).

- **Kutubxona:** Apache POI (`poi-ooxml`)
- **Parsing yondashuvi:** Butun matnni regex bilan kesish **emas**, balki `XWPFDocument` + `IBodyElement` bo'yicha **ketma-ket, holatli (stateful) ierarxik parsing**

### 2. Ierarxik parsing mantiq

O'zbekiston huquqiy hujjatlari turli tuzilmaga ega:

| Hujjat turi | Bo'linish qoidasi | `articleRef` misoli |
|-------------|-------------------|---------------------|
| `CODE`, `LAW` | `-modda` bo'yicha | `15-modda` |
| `CABINET_RESOLUTION`, `PRESIDENTIAL_DECREE`, va boshqalar | `-BOB` (bob) + `-band` / raqamlangan band | `1-bob, 3-band` |

**Kontekst saqlash:** Har bir chunk yaratilganda `currentChapter` (masalan, `1-BOB. Umumiy qoidalar`) matn boshiga qo'shiladi — LLM moddani qaysi bob ichida ekanini tushunadi.

**Jadvallar:** `XWPFTable` elementlari Markdown (`|`) formatiga o'tkazilib joriy chunk ga qo'shiladi.

### 3. Aqlli qisman yangilash — Hash-based diffing (3-bosqich)

Admin butun kodeksni qayta yuklaganda tizim faqat **o'zgargan moddalarni** yangilaydi:

1. Har bir modda uchun `text_hash` (SHA-256) hisoblanadi — **2-bosqichda allaqachon yoziladi**
2. Eski va yangi hash solishtiriladi
3. Hash bir xil → o'tkazib yuboriladi
4. Hash farqli → eski chunk o'chiriladi, yangi matn embedding qilinadi

### 4. Mustaqil bekor qilish (Independent Revocation)

VMQ-370 kabi hujjatni alohida **REVOKED** holatiga o'tkazish mumkin. `DocumentStatus.REVOKED` bo'lgan hujjatlar RAG qidiruvidan chiqariladi (5-bosqich + status API).

### 5. Embedding va vektor baza

| Komponent | Tanlov |
|-----------|--------|
| Embedding model | Gemini `text-embedding-004` |
| Vektor o'lchami | **768** |
| Vektor bazasi | PostgreSQL **pgvector** kengaytmasi |
| O'xshashlik | Cosine distance |

---

# 1-BOSQICH: Ma'lumotlar qatlami ✅

## Maven dependencylar

```xml
<!-- PostgreSQL pgvector (RAG vektor saqlash) -->
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

| Dependency | Vazifa |
|------------|--------|
| `com.pgvector:pgvector` | PostgreSQL pgvector JDBC qo'llab-quvvatlash |
| `org.hibernate.orm:hibernate-vector` | Hibernate 6 da `@JdbcTypeCode(SqlTypes.VECTOR)` mapping |

## Enumlar

### `DocumentStatus` — hujjat holati (RAG filtri)

| Qiymat | Tavsif |
|--------|--------|
| `ACTIVE` | Hujjat amalda, AI maslahatlarida ishlatiladi |
| `PARTIALLY_AMENDED` | Ba'zi moddalar yangilangan |
| `SUPERSEDED` | Yangi hujjat bilan almashtirilgan |
| `REVOKED` | Bekor qilingan — AI maslahat bermaydi |

**Fayl:** `enums/DocumentStatus.java`

### `DocumentType` — hujjat turi

| Qiymat | Tavsif |
|--------|--------|
| `CODE` | Kodeks (masalan, Mehnat kodeksi) |
| `LAW` | Qonun |
| `PRESIDENTIAL_DECREE` | Prezident farmoni |
| `PRESIDENTIAL_RESOLUTION` | Prezident qarori |
| `CABINET_RESOLUTION` | Vazirlar Mahkamasi qarori |
| `MINISTRY_ORDER` | Vazirlik buyrug'i |
| `OTHER` | Boshqa |

**Fayl:** `enums/DocumentType.java`

## Ma'lumotlar bazasi

Jadvallar Hibernate `ddl-auto=update` orqali avtomatik yaratiladi.

### PostgreSQL: pgvector ni faollashtirish

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Jadval: `legal_documents`

| Ustun | Tip | Majburiy | Tavsif |
|-------|-----|----------|--------|
| `id` | UUID | Ha | Primary key |
| `type` | VARCHAR (enum) | Ha | `DocumentType` |
| `doc_number` | VARCHAR(64) | Ha | Masalan: `VMQ-370` |
| `doc_date` | DATE | Yo'q | Hujjat sanasi |
| `title` | TEXT | Ha | Hujjat nomi |
| `status` | VARCHAR (enum) | Ha | Default: `ACTIVE` |
| `superseded_by_id` | UUID | Yo'q | Almashtirgan hujjat id si |
| `created_at` | TIMESTAMP | Ha | Yaratilgan vaqt |
| `updated_at` | TIMESTAMP | Yo'q | Yangilangan vaqt |

**Entity:** `entity/LegalDocumentEntity.java`

### Jadval: `law_chunks`

| Ustun | Tip | Majburiy | Tavsif |
|-------|-----|----------|--------|
| `id` | UUID | Ha | Primary key |
| `document_id` | UUID | Ha | FK → `legal_documents.id` |
| `article_ref` | VARCHAR(64) | Ha | Masalan: `12-modda` yoki `1-bob, 3-band` |
| `content` | TEXT | Ha | Modda/band matni (+ bob konteksti, markdown jadvallar) |
| `text_hash` | VARCHAR(64) | Ha | `content` ning SHA-256 hash |
| `embedding` | **vector(768)** | Yo'q | Gemini embedding (hozircha `null`) |

**Entity:** `entity/LawChunkEntity.java`

**Vektor mapping:**

```java
@Column(name = "embedding")
@JdbcTypeCode(SqlTypes.VECTOR)
@Array(length = 768)
private float[] embedding;
```

## Repositorylar

### `LegalDocumentRepository`

| Metod | Vazifa |
|-------|--------|
| `findByDocNumber(String)` | Raqam bo'yicha hujjat topish |
| `findAllByStatus(DocumentStatus)` | Holat bo'yicha ro'yxat |

### `LawChunkRepository`

| Metod | Vazifa |
|-------|--------|
| `findAllByDocumentIdOrderByArticleRefAsc(UUID)` | Hujjatning barcha chunklari |
| `findByDocumentIdAndArticleRef(UUID, String)` | Bitta chunk (hash diff uchun) |
| `deleteAllByDocumentId(UUID)` | Hujjat chunklarini tozalash |

---

# 2-BOSQICH: Hujjat yuklash va parsing ✅

## Maven dependency (qo'shimcha)

```xml
<!-- Word (.docx) hujjat parsing (RAG) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

## Yangi fayllar

| Fayl | Vazifa |
|------|--------|
| `service/LegalDocumentParsingService.java` | `.docx` ierarxik parsing, chunk yaratish, SHA-256 |
| `service/LegalDocumentService.java` | Upload orchestration (`@Transactional`) |
| `controller/LegalDocumentAdminController.java` | Admin upload REST API |
| `dto/legal/LegalDocumentUploadDTO.java` | Upload form maydonlari |
| `dto/legal/LegalDocumentUploadResponseDTO.java` | Upload javobi |
| `test/.../LegalDocumentParsingServiceTest.java` | Parsing unit testlari |

## Upload oqimi

```
POST /api/v1/admin/legal-documents/upload
        │
        ▼
LegalDocumentAdminController
        │
        ▼
LegalDocumentService.upload()  [@Transactional]
        │
        ├─► docNumber takrorlanishini tekshirish
        ├─► supersededById mavjudligini tekshirish
        ├─► LegalDocumentEntity saqlash (status=ACTIVE)
        ├─► LegalDocumentParsingService.parseAndChunkDocx()
        └─► LawChunkRepository.saveAll(chunks)
```

## REST API

### `POST /api/v1/admin/legal-documents/upload`

| Parametr | Turi | Majburiy | Tavsif |
|----------|------|----------|--------|
| `file` | MultipartFile | Ha | Faqat `.docx` |
| `type` | DocumentType | Ha | Hujjat turi |
| `docNumber` | String | Ha | Masalan: `VMQ-370` |
| `docDate` | LocalDate | Yo'q | `YYYY-MM-DD` |
| `title` | String | Ha | Hujjat nomi |
| `supersededById` | UUID | Yo'q | Almashtirilgan hujjat id si |

**Ruxsat:** `ROLE_ADMIN`, `ROLE_SUPERADMIN`  
**Content-Type:** `multipart/form-data`

### Javob misoli

```json
{
  "success": true,
  "message": "Hujjat muvaffaqiyatli yuklandi, chunkCount=142",
  "data": {
    "documentId": "a1b2c3d4-...",
    "type": "CODE",
    "docNumber": "MK-001",
    "docDate": "2024-01-15",
    "title": "O'zbekiston Respublikasining Mehnat kodeksi",
    "status": "ACTIVE",
    "chunkCount": 142
  }
}
```

### cURL misoli

```bash
curl -X POST "http://localhost:8080/api/v1/admin/legal-documents/upload" \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -F "file=@mehnat-kodeksi.docx" \
  -F "type=CODE" \
  -F "docNumber=MK-001" \
  -F "docDate=2024-01-15" \
  -F "title=Mehnat kodeksi"
```

## `LegalDocumentParsingService` — parsing mantiq

### Asosiy metod

```java
public List<LawChunkEntity> parseAndChunkDocx(
    MultipartFile file,
    LegalDocumentEntity document
)
```

### Iteratsiya usuli

`XWPFDocument.getBodyElements()` bo'yicha ketma-ket o'qiladi:

| Element turi | Harakat |
|--------------|---------|
| `XWPFParagraph` | Hujjat turiga qarab modda/band/bob aniqlash |
| `XWPFTable` | Markdown jadvalga aylantirish, joriy chunk ga qo'shish |

### Holat o'zgaruvchilari (ParsingContext)

| O'zgaruvchi | Vazifa |
|-------------|--------|
| `currentChapter` | Joriy bob konteksti, masalan: `1-BOB. Umumiy qoidalar` |
| `currentArticleRef` | Chunk identifikatori |
| `currentContent` | Joriy chunk matni (StringBuilder) |

### Regex patternlar

| Pattern | Maqsad | Misol |
|---------|--------|-------|
| `BOB_PATTERN` | Bob sarlavhasi | `1-BOB. Umumiy qoidalar` |
| `MODDA_PATTERN` | Modda (kodeks/qonun) | `15-modda. Matn...` |
| `BAND_PATTERN` | Band (qaror/farmon) | `3-band. Matn...` |
| `NUMBERED_POINT_PATTERN` | Raqamlangan band | `1. Matn...` yoki `1) Matn...` |

### Kodeks / Qonun (`CODE`, `LAW`)

1. `-BOB` topilsa → `currentChapter` yangilanadi (chunk yaratilmaydi)
2. `-modda` topilsa → oldingi chunk yakunlanadi, yangi chunk boshlanadi
3. Keyingi paragraflar joriy modda chunkiga qo'shiladi
4. Chunk yakunlanganda `currentChapter` matn boshiga qo'shiladi

### Qaror / Farmon (qolgan turlar)

1. `-BOB` topilsa → `currentChapter` yangilanadi
2. `-band` yoki raqamlangan band topilsa → yangi chunk, `articleRef = "1-bob, 3-band"`
3. Jadval → Markdown formatida joriy chunk ga qo'shiladi

### Chunk yaratish

Har bir chunk uchun:

```java
chunk.setDocumentId(document.getId());
chunk.setArticleRef("15-modda");          // yoki "1-bob, 3-band"
chunk.setContent(chapterContext + body);  // bob konteksti + matn
chunk.setTextHash(sha256(content));       // SHA-256 (64 belgi hex)
chunk.setEmbedding(null);                 // 4-bosqichda to'ldiriladi
```

### Xato holatlari

| Xato | Sabab |
|------|-------|
| `Yuklanadigan fayl bo'sh!` | Fayl yo'q yoki 0 bayt |
| `Faqat .docx formatidagi Word hujjat qabul qilinadi!` | Noto'g'ri kengaytma |
| `Word hujjatini o'qishda xatolik: ...` | IOException (buzilgan fayl) |
| `Hujjatdan modda yoki band ajratib bo'lmadi!` | Hech qanday chunk topilmadi |
| `Bu hujjat raqami allaqachon mavjud: ...` | Takroriy `docNumber` |

## Unit testlar

**Fayl:** `LegalDocumentParsingServiceTest.java`

| Test | Tekshiruv |
|------|-----------|
| `sha256_generatesConsistentHash` | Hash barqaror va 64 belayli |
| `parseAndChunkDocx_splitsCodeByModdaAndPrependsChapter` | Kodeks: 2 modda, bob konteksti qo'shilgan |
| `parseAndChunkDocx_splitsResolutionByBobAndBand` | Qaror: `1-bob, 1-band` va `1-bob, 2-band` |

---

## Entity munosabatlari diagrammasi

```
┌─────────────────────────┐
│   legal_documents       │
├─────────────────────────┤
│ id (UUID) PK            │
│ type, doc_number        │
│ doc_date, title         │
│ status                  │
│ superseded_by_id        │
│ created_at / updated_at │
└───────────┬─────────────┘
            │ 1
            │ N
┌───────────▼─────────────┐
│   law_chunks            │
├─────────────────────────┤
│ id (UUID) PK            │
│ document_id FK          │
│ article_ref             │
│ content (+ bob context) │
│ text_hash (SHA-256)     │
│ embedding vector(768)   │  ← hozircha null
└─────────────────────────┘
```

---

## Fayl tuzilmasi (hozirgi holat)

```
api.ailawyer.uz/
├── pom.xml                                          [pgvector, hibernate-vector, poi-ooxml]
└── src/
    ├── main/java/api/ailawyer/uz/
    │   ├── controller/
    │   │   └── LegalDocumentAdminController.java    [YANGI — 2-bosqich]
    │   ├── dto/legal/
    │   │   ├── LegalDocumentUploadDTO.java          [YANGI — 2-bosqich]
    │   │   └── LegalDocumentUploadResponseDTO.java  [YANGI — 2-bosqich]
    │   ├── enums/
    │   │   ├── DocumentStatus.java                  [1-bosqich]
    │   │   └── DocumentType.java                    [1-bosqich]
    │   ├── entity/
    │   │   ├── LegalDocumentEntity.java             [1-bosqich]
    │   │   └── LawChunkEntity.java                  [1-bosqich]
    │   ├── repository/
    │   │   ├── LegalDocumentRepository.java         [1-bosqich]
    │   │   └── LawChunkRepository.java              [1-bosqich]
    │   └── service/
    │       ├── LegalDocumentParsingService.java     [YANGI — 2-bosqich]
    │       └── LegalDocumentService.java            [YANGI — 2-bosqich]
    └── test/java/api/ailawyer/uz/service/
        └── LegalDocumentParsingServiceTest.java     [YANGI — 2-bosqich]
```

**Keyingi bosqichlarda yoziladi:**

```
service/
├── LegalDocumentDiffService.java       [3-bosqich — hash diffing]
├── EmbeddingService.java               [4-bosqich — Gemini embedding]
└── LegalSearchService.java             [5-bosqich — vektor qidiruv]

controller/
└── LegalDocumentAdminController.java   [status REVOKED API — keyinroq]
```

---

## Hali amalga oshirilmagan funksiyalar

| Funksiya | Bosqich |
|----------|---------|
| Hash-based diffing (qayta yuklashda faqat o'zgargan moddalar) | 3 |
| Gemini Embedding API (`text-embedding-004`) | 4 |
| pgvector cosine qidiruv | 5 |
| AI suhbatiga kontekst qo'shish | 5 |
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

Natija: **0 xato** (1 va 2-bosqichlar yakunlanganda tasdiqlangan).

### Bazada ma'lumotlarni ko'rish

```sql
-- Yuklangan hujjatlar
SELECT id, doc_number, type, status, title
FROM legal_documents
ORDER BY created_at DESC;

-- Chunklar (embedding hozircha null)
SELECT document_id, article_ref, LEFT(content, 80) AS preview, text_hash,
       embedding IS NULL AS embedding_empty
FROM law_chunks
ORDER BY article_ref
LIMIT 20;

-- Muayyan hujjat bo'yicha chunklar soni
SELECT d.doc_number, COUNT(c.id) AS chunk_count
FROM legal_documents d
JOIN law_chunks c ON c.document_id = d.id
GROUP BY d.doc_number;
```

---

## Keyingi bosqich (3-qadam) — reja

1. Admin hujjatni **qayta yuklash** endpointi (yoki mavjud upload ni yangilash rejimi)
2. Yangi `.docx` parse qilinadi, har bir chunk uchun yangi `text_hash` hisoblanadi
3. Eski chunk hash bilan solishtiriladi:
   - Bir xil → o'tkazib yuboriladi
   - Farqli → eski chunk o'chiriladi, yangisi embedding uchun navbatga qo'yiladi
4. `DocumentStatus.PARTIALLY_AMENDED` holati yangilanadi

---

## Xulosa

| Nima qilindi | Bosqich | Tafsilot |
|--------------|---------|----------|
| ✅ pgvector integratsiyasi | 1 | Maven + Hibernate vector mapping |
| ✅ `legal_documents` / `law_chunks` | 1 | Entity, enum, repository |
| ✅ Apache POI parsing | 2 | Ierarxik bob/modda/band ajratish |
| ✅ Markdown jadvallar | 2 | `XWPFTable` → `\|` format |
| ✅ SHA-256 hash | 2 | Har chunk uchun `text_hash` |
| ✅ Admin upload API | 2 | `POST /api/v1/admin/legal-documents/upload` |
| ✅ Unit testlar | 2 | Kodeks va qaror parsing testlari |
| ⏳ Hash diff | 3 | Keyingi qadam |
| ⏳ Gemini embedding | 4 | Rejada |
| ⏳ Vektor qidiruv | 5 | Rejada |

RAG tizimining **ma'lumotlar qatlami va hujjat yuklash qatlami** tayyor. Keyingi qadam — hash-based aqlli yangilash algoritmi.
