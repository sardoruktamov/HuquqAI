# BOSQICH 8 — RAG (Retrieval-Augmented Generation) Tizimi

**Loyiha:** QalqonAI (`api.ailawyer.uz`)  
**Texnologiya:** Spring Boot 3, Java 17, Hibernate 6, PostgreSQL + pgvector, Gemini Embedding API  
**Maqsad:** Huquqiy hujjatlarni (kodeks, qonun, VMQ va hokazo) vektor bazaga saqlab, foydalanuvchi savoliga mos moddalarni topish va AI javobiga kontekst sifatida berish.

---

## Qisqacha xulosa

QalqonAI uchun **RAG arxitekturasi** bosqichma-bosqich qurilmoqda. Hozircha **1-bosqich (Ma'lumotlar qatlami)** yakunlandi — entity, enum va repositorylar tayyor. Parsing, hash-diff, embedding va qidiruv servislari **hali yozilmagan**.

RAG tizimi quyidagi mantiqda ishlaydi:

```
.docx hujjat yuklash → moddalarga bo'lish (chunking)
        → hash solishtirish (faqat o'zgargan moddalar)
        → Gemini embedding (768 o'lcham)
        → pgvector bazasiga saqlash
        → foydalanuvchi savolida cosine qidiruv (faqat ACTIVE hujjatlar)
        → topilgan moddalar Gemini suhbatiga kontekst sifatida qo'shiladi
```

---

## Umumiy arxitektura rejasi (5 bosqich)

| Bosqich | Nomi | Holat | Tavsif |
|---------|------|-------|--------|
| **1** | Ma'lumotlar qatlami | ✅ **Bajarildi** | `legal_documents`, `law_chunks` jadvallari, enumlar, repositorylar, pgvector dependency |
| **2** | Hujjat yuklash qatlami | ⏳ Keyingi | Apache POI + `.docx` o'qish, regex bilan moddalarga bo'lish |
| **3** | Aqlli yangilash | ⏳ Rejada | Hash-based diffing — faqat o'zgargan moddalarni qayta embedding qilish |
| **4** | AI integratsiyasi | ⏳ Rejada | Gemini `text-embedding-004` orqali vektorlash |
| **5** | Qidiruv qatlami | ⏳ Rejada | Cosine similarity qidiruv, faqat `ACTIVE` hujjatlar, AI kontekstiga ulash |

---

## Dizayn qarorlari (kelajak bosqichlar uchun)

Quyidagi yechimlar arxitektura hujjatida qabul qilingan va keyingi bosqichlarda amalga oshiriladi:

### 1. Fayl formati — `.docx` (PDF emas)

PDF matn emas, dizayn saqlaydi — moddalarni (`1-modda`, `2-modda`) ajratish qiyin. Shuning uchun qonunlar **asosan `.docx` formatida** yuklanadi (Lex.uz dagi `.doc` yuklab olish odatiga mos).

- **Kutubxona:** Apache POI
- **Bo'lish (chunking):** Regex — `(?=\d+-modda\.)` (raqam + `-modda` oldidan kesish)

### 2. Aqlli qisman yangilash — Hash-based diffing

Admin butun kodeksni qayta yuklaganda tizim faqat **o'zgargan moddalarni** yangilaydi:

1. Har bir modda uchun `text_hash` (SHA-256) hisoblanadi
2. Eski va yangi hash solishtiriladi
3. Hash bir xil → o'tkazib yuboriladi (Gemini API chaqiruvi yo'q)
4. Hash farqli → eski chunk o'chiriladi, yangi matn embedding qilinadi

Bu Gemini API xarajatini va admin vaqtini tejaydi.

### 3. Mustaqil bekor qilish (Independent Revocation)

VMQ-370 kabi hujjatni alohida **REVOKED** holatiga o'tkazish mumkin — boshqa hujjatni kutmasdan. `DocumentStatus.REVOKED` bo'lgan hujjatlar RAG qidiruvidan chiqariladi, AI maslahat bermaydi.

### 4. Embedding va vektor baza

| Komponent | Tanlov |
|-----------|--------|
| Embedding model | Gemini `text-embedding-004` |
| Vektor o'lchami | **768** |
| Vektor bazasi | PostgreSQL **pgvector** kengaytmasi |
| O'xshashlik | Cosine distance (millisoniyalarda qidiruv) |

---

## 1-bosqich: Bajarilgan ishlar

### Maven dependencylar (`pom.xml`)

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
| `org.hibernate.orm:hibernate-vector` | Hibernate 6 da `@JdbcTypeCode(SqlTypes.VECTOR)` mapping (Spring Boot 3.3.5 Hibernate 6.5.3 ga mos) |

> `hibernate-vector` modulisiz `float[] embedding` maydoni runtime da xato beradi — pgvector-java hujjatlari ham shuni tavsiya qiladi.

---

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

---

## Ma'lumotlar bazasi

Jadvallar Hibernate `ddl-auto=update` orqali avtomatik yaratiladi. Flyway migratsiyasi hozircha yo'q.

### PostgreSQL: pgvector ni faollashtirish

Kod ichida avtomatik yoqilmaydi — DB da **bir marta qo'lda** bajarish kerak:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Jadval: `legal_documents`

Huquqiy hujjat metadata (manba hujjat).

| Ustun | Tip | Majburiy | Tavsif |
|-------|-----|----------|--------|
| `id` | UUID | Ha | Primary key |
| `type` | VARCHAR (enum) | Ha | `DocumentType` |
| `doc_number` | VARCHAR(64) | Ha | Hujjat raqami, masalan: `VMQ-370` |
| `doc_date` | DATE | Yo'q | Hujjat sanasi |
| `title` | TEXT | Ha | Hujjat nomi |
| `status` | VARCHAR (enum) | Ha | Default: `ACTIVE` |
| `superseded_by_id` | UUID | Yo'q | Almashtirgan yangi hujjat id si |
| `created_at` | TIMESTAMP | Ha | Yaratilgan vaqt |
| `updated_at` | TIMESTAMP | Yo'q | Yangilangan vaqt |

**Indekslar:** `doc_number`, `status`, `type`

**Entity:** `entity/LegalDocumentEntity.java`

### Jadval: `law_chunks`

Hujjat modda bo'laklari va vektor embeddinglari.

| Ustun | Tip | Majburiy | Tavsif |
|-------|-----|----------|--------|
| `id` | UUID | Ha | Primary key |
| `document_id` | UUID | Ha | FK → `legal_documents.id` |
| `article_ref` | VARCHAR(64) | Ha | Modda havolasi, masalan: `12-modda` |
| `content` | TEXT | Ha | Modda matni |
| `text_hash` | VARCHAR(64) | Ha | `content` ning SHA-256 hash (aqlli yangilash uchun) |
| `embedding` | **vector(768)** | Yo'q | Gemini embedding vektori |

**Indekslar:** `document_id`, `article_ref`, `text_hash`

**Entity:** `entity/LawChunkEntity.java`

**Vektor mapping (Hibernate 6):**

```java
@Column(name = "embedding")
@JdbcTypeCode(SqlTypes.VECTOR)
@Array(length = 768)
private float[] embedding;
```

**Bog'lanish:**

```
legal_documents (1) ──────< (N) law_chunks
```

---

## Repositorylar

### `LegalDocumentRepository`

**Fayl:** `repository/LegalDocumentRepository.java`

| Metod | Vazifa |
|-------|--------|
| `findByDocNumber(String)` | Raqam bo'yicha hujjat topish (`VMQ-370`) |
| `findAllByStatus(DocumentStatus)` | Holat bo'yicha ro'yxat (masalan, faqat `ACTIVE`) |

### `LawChunkRepository`

**Fayl:** `repository/LawChunkRepository.java`

| Metod | Vazifa |
|-------|--------|
| `findAllByDocumentIdOrderByArticleRefAsc(UUID)` | Hujjatning barcha moddalari |
| `findByDocumentIdAndArticleRef(UUID, String)` | Bitta modda (hash diff uchun) |
| `deleteAllByDocumentId(UUID)` | Hujjat moddalarini tozalash |

---

## Entity munosabatlari diagrammasi

```
┌─────────────────────────┐
│   legal_documents       │
├─────────────────────────┤
│ id (UUID) PK            │
│ type                    │
│ doc_number              │
│ doc_date                │
│ title                   │
│ status                  │
│ superseded_by_id        │
│ created_at / updated_at │
└───────────┬─────────────┘
            │ 1
            │
            │ N
┌───────────▼─────────────┐
│   law_chunks            │
├─────────────────────────┤
│ id (UUID) PK            │
│ document_id FK          │
│ article_ref             │
│ content                 │
│ text_hash               │
│ embedding vector(768)   │
└─────────────────────────┘
```

---

## Fayl tuzilmasi (hozirgi holat)

```
api.ailawyer.uz/
├── pom.xml                                 [O'ZGARTIRILDI — pgvector, hibernate-vector]
└── src/main/java/api/ailawyer/uz/
    ├── enums/
    │   ├── DocumentStatus.java             [YANGI]
    │   └── DocumentType.java               [YANGI]
    ├── entity/
    │   ├── LegalDocumentEntity.java        [YANGI]
    │   └── LawChunkEntity.java             [YANGI]
    └── repository/
        ├── LegalDocumentRepository.java    [YANGI]
        └── LawChunkRepository.java         [YANGI]
```

**Hali yozilmagan (keyingi bosqichlar):**

```
service/
├── LegalDocumentParsingService.java        [2-bosqich — .docx + regex chunking]
├── LegalDocumentDiffService.java           [3-bosqich — hash diffing]
├── EmbeddingService.java                   [4-bosqich — Gemini embedding]
└── LegalSearchService.java                 [5-bosqich — vektor qidiruv]

controller/
└── LegalDocumentAdminController.java       [Admin upload / status API]
```

---

## Hali amalga oshirilmagan funksiyalar

Quyidagilar **1-bosqichda ataylab qoldirilgan**:

- `.docx` fayl yuklash va parsing (Apache POI)
- Regex bilan moddalarga bo'lish
- SHA-256 hash hisoblash servisi
- Hash-based diffing algoritmi
- Gemini Embedding API integratsiyasi
- Cosine similarity qidiruv
- Admin panel REST API (upload, status o'zgartirish, qidiruv)
- `REVOKED` holatini boshqarish API si

---

## Tekshirish

### Kompilyatsiya va testlar

```bash
cd api.ailawyer.uz
./mvnw compile
./mvnw test
```

Natija: **0 xato** (1-bosqich yakunlanganda tasdiqlangan).

### Bazada jadvallar paydo bo'lishi

Ilovani ishga tushirgandan keyin (pgvector extension yoqilgan bo'lsa):

```sql
-- Jadvallar mavjudligini tekshirish
SELECT table_name FROM information_schema.tables
WHERE table_name IN ('legal_documents', 'law_chunks');

-- law_chunks embedding ustuni tipi
SELECT column_name, udt_name
FROM information_schema.columns
WHERE table_name = 'law_chunks' AND column_name = 'embedding';
-- Kutilgan: udt_name = 'vector'
```

---

## Keyingi bosqich (2-qadam) — reja

1. `pom.xml` ga **Apache POI** dependency qo'shish
2. `.docx` o'qish servisi yozish
3. Regex `(?=\d+-modda\.)` bilan matnni moddalarga kesish
4. `LegalDocumentEntity` + `LawChunkEntity` ga saqlash (embedding hali bo'sh)
5. Admin upload endpoint (controller)

---

## Xulosa

| Nima qilindi | Tafsilot |
|--------------|----------|
| ✅ pgvector integratsiyasi | Maven dependency + Hibernate vector mapping |
| ✅ `legal_documents` jadvali | Hujjat metadata, status, turi |
| ✅ `law_chunks` jadvali | Modda matni, hash, 768-o'lchamli vektor |
| ✅ Enumlar | `DocumentStatus`, `DocumentType` |
| ✅ Repositorylar | CRUD + qidiruv metodlari |
| ⏳ Parsing, diff, embedding, qidiruv | Keyingi 2–5 bosqichlar |

RAG tizimining **poydevori** tayyor — keyingi qadam hujjat yuklash va moddalarga bo'lish servisidir.
