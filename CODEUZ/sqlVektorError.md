# PostgreSQL pgvector va `law_chunks` Initialization Xatosi — Tuzatish Hujjati

**Loyiha:** QalqonAI (`api.ailawyer.uz`)  
**Sana:** 2026-06-19  
**Muammo kodi:** `PSQLException: ERROR: relation "law_chunks" does not exist`  
**Bog'liq funksiya:** RAG hujjat yuklash, vektor embedding, AI chat qidiruv

---

## Qisqacha xulosa

Ilova ishga tushganda yoki hujjat yuklanganda PostgreSQL **`law_chunks` jadvali mavjud emas** deb xato berardi. Asosiy sabab — **pgvector extension** Hibernate schema yaratishidan **oldin** yoqilmaganligi tufayli `embedding vector(768)` ustuni yaratilmay, jadval shakllanmas edi. Qo'shimcha muammo — HNSW indeks `@PostConstruct` vaqtida jadval hali mavjud bo'lmagan paytda yaratilishga urinardi.

Quyidagi o'zgarishlar bilan initialization tartibi to'g'rilandi:

| # | O'zgarish | Fayl |
|---|-----------|------|
| 1 | pgvector extension SQL skripti | `schema.sql` *(yangi)* |
| 2 | Spring Boot SQL init sozlamalari | `application.properties` |
| 3 | Extension Hibernate'dan oldin yaratish | `PgVectorExtensionConfig.java` *(yangi)* |
| 4 | HNSW indeks vaqtini kechiktirish | `RagDatabaseConfig.java` |
| 5 | Entity mapping tekshiruvi | `LawChunkEntity.java` *(o'zgartirilmadi)* |

---

## 1. Xato belgilari

### 1.1. Asosiy xato (hujjat yuklash / chat paytida)

```
org.postgresql.util.PSQLException: ERROR: relation "law_chunks" does not exist
```

Bu xato quyidagi holatlarda paydo bo'lishi mumkin:

- Admin hujjat yuklaganda (`POST /api/v1/admin/legal-documents/upload`)
- Embedding processor chunk saqlashga urinayotganda
- RAG qidiruv (`LegalSearchService`) native SQL bajarayotganda

### 1.2. Ilova ishga tushish paytidagi xato (pgvector o'rnatilmagan bo'lsa)

```
org.postgresql.util.PSQLException: ERROR: could not open extension control file
"C:/Program Files/PostgreSQL/12/share/extension/vector.control": No such file or directory
```

Bu xabar PostgreSQL serverda **pgvector extension o'rnatilmagan**ligini bildiradi.

### 1.3. Flyway + JPA circular dependency (o'rtasida uchragan)

```
Circular depends-on relationship between 'flyway' and 'entityManagerFactory'
```

`spring.jpa.defer-datasource-initialization=true` qo'shilganda Flyway va Hibernate o'zaro kutib qolardi.

---

## 2. Root Cause Analysis (ildiz sabab)

### 2.1. Initialization tartibi noto'g'ri edi

RAG tizimida `LawChunkEntity` quyidagi ustunga ega:

```java
@Column(name = "embedding")
@JdbcTypeCode(SqlTypes.VECTOR)
@Array(length = 768)
private float[] embedding;
```

Bu ustun PostgreSQL da **`vector(768)`** tipida yaratilishi kerak. Bu tip faqat **`CREATE EXTENSION vector`** bajarilgandan keyin mavjud bo'ladi.

**Muammo zanjir:**

```
1. Ilova ishga tushadi
2. Hibernate ddl-auto=update → law_chunks jadvalini yaratishga urinadi
3. PostgreSQL "vector" tipini tanimaydi
4. embedding ustuni yaratilmaydi / jadval shakllanmaydi
5. Keyingi operatsiyalar "law_chunks does not exist" deb yiqiladi
```

### 2.2. HNSW indeks ertadan yaratilardi

`RagDatabaseConfig` dastlab `@PostConstruct` ishlatgan:

```java
@PostConstruct
public void ensureHnswIndex() {
    jdbcTemplate.execute("CREATE INDEX ... ON law_chunks ...");
}
```

`@PostConstruct` bean yaratilishi bilan darhol ishga tushadi — bu **Hibernate jadvalni yaratishidan oldin** bo'lishi mumkin edi.

### 2.3. Spring Boot `defer-datasource-initialization` tushunchasi

| Property qiymati | `schema.sql` qachon ishlaydi |
|------------------|------------------------------|
| `defer=false` (default) | Hibernate **dan oldin** |
| `defer=true` | Hibernate **dan keyin** |

`defer=true` aslida Hibernate yaratgan schema ustiga qo'shimcha SQL yozish uchun mo'ljallangan. pgvector extension esa **Hibernate dan oldin** kerak. Shuning uchun faqat `schema.sql` + `defer=true` yetarli emas — **`PgVectorExtensionConfig`** qo'shildi.

---

## 3. Qilingan o'zgarishlar (batafsil)

### 3.1. `LawChunkEntity` — tekshiruv (o'zgartirilmadi)

Fayl: `src/main/java/api/ailawyer/uz/entity/LawChunkEntity.java`

Entity allaqachon to'g'ri sozlangan:

```java
@Entity
@Table(name = "law_chunks", indexes = { ... })
public class LawChunkEntity {
    // ...
    @Column(name = "embedding")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    private float[] embedding;
}
```

`application.properties` da `spring.jpa.hibernate.ddl-auto=update` mavjud — Hibernate jadvallarni avtomatik yangilaydi.

---

### 3.2. `schema.sql` — YANGI fayl

Fayl: `src/main/resources/schema.sql`

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

| Parametr | Qiymat |
|----------|--------|
| Maqsad | PostgreSQL da pgvector extension yoqish |
| Xavfsizlik | `IF NOT EXISTS` — qayta ishga tushirishda xato bermaydi |
| Ishga tushish | Spring Boot `spring.sql.init.mode=always` bilan avtomatik |

---

### 3.3. `application.properties` — YANGI sozlamalar

Fayl: `src/main/resources/application.properties`

**Qo'shilgan qatorlar:**

```properties
# JPA
spring.jpa.defer-datasource-initialization=true

# pgvector — schema.sql + PgVectorExtensionConfig (Hibernate dan oldin extension yaratiladi)
spring.sql.init.mode=always
spring.sql.init.continue-on-error=true

# Flyway migratsiyasi yo'q; JPA defer bilan circular dependency oldini olish
spring.flyway.enabled=false
```

| Property | Qiymat | Vazifasi |
|----------|--------|----------|
| `spring.jpa.hibernate.ddl-auto` | `update` | *(mavjud)* Hibernate schema avtomatik yangilash |
| `spring.jpa.defer-datasource-initialization` | `true` | SQL init skriptlarini Hibernate keyinroq bosqichda ham ishlatish imkoniyati |
| `spring.sql.init.mode` | `always` | `schema.sql` har doim bajarilsin |
| `spring.sql.init.continue-on-error` | `true` | pgvector o'rnatilmagan dev/test muhitda ilova to'xtamasin |
| `spring.flyway.enabled` | `false` | Flyway migratsiyasi yo'q; circular dependency oldini olish |

**Nima uchun `spring.flyway.enabled=false`?**

Loyihada `db/migration/` papkasida Flyway migratsiya fayllari yo'q. `defer-datasource-initialization=true` bilan Flyway va `entityManagerFactory` o'rtasida circular dependency yuzaga keldi. Flyway o'chirilganda muammo bartaraf etildi.

---

### 3.4. `PgVectorExtensionConfig.java` — YANGI fayl (asosiy yechim)

Fayl: `src/main/java/api/ailawyer/uz/config/PgVectorExtensionConfig.java`

Bu klass **Hibernate schema generation dan oldin** pgvector extension yaratishni kafolatlaydi.

#### Annotatsiyalar

```java
@Configuration
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
@Slf4j
public class PgVectorExtensionConfig { ... }
```

`@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)` — Hibernate JPA auto-config dan **oldin** yuklanadi.

#### Bean 1: `pgVectorExtensionInitializer`

```java
@Bean(name = "pgVectorExtensionInitializer")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public Object pgVectorExtensionInitializer(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement()) {
        statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
        log.info("PostgreSQL pgvector extension tayyor");
    } catch (Exception e) {
        log.warn("pgvector extension yaratib bo'lmadi: {}", e.getMessage());
    }
    return new Object();
}
```

| Xususiyati | Tavsif |
|------------|--------|
| `@Role(INFRASTRUCTURE)` | Infrastruktura bean sifatida erta yuklanadi |
| try-catch | pgvector yo'q bo'lsa ilova ishga tushishda yiqilmaydi |
| SQL | `schema.sql` bilan bir xil — ikki marta xavfsiz |

#### Bean 2: `entityManagerFactoryDependsOnPgVector`

```java
@Bean
public static BeanFactoryPostProcessor entityManagerFactoryDependsOnPgVector() {
    return beanFactory -> {
        BeanDefinition emf = beanFactory.getBeanDefinition("entityManagerFactory");
        // entityManagerFactory → depends-on → pgVectorExtensionInitializer
        emf.setDependsOn(...);
    };
}
```

Bu **initialization tartibini majburiy qiladi:**

```
DataSource
    ↓
pgVectorExtensionInitializer  →  CREATE EXTENSION vector
    ↓
entityManagerFactory (Hibernate)  →  ddl-auto=update  →  law_chunks yaratiladi
    ↓
schema.sql (defer=true bo'yicha keyinroq, qo'shimcha)
    ↓
ApplicationReadyEvent
    ↓
RagDatabaseConfig  →  HNSW indeks
```

---

### 3.5. `RagDatabaseConfig.java` — O'ZGARTIRILDI

Fayl: `src/main/java/api/ailawyer/uz/config/RagDatabaseConfig.java`

#### Oldin (muammoli)

```java
@PostConstruct
public void ensureHnswIndex() {
    jdbcTemplate.execute(HNSW_INDEX_SQL);
}
```

`@PostConstruct` bean yaratilishi bilan ishga tushardi — `law_chunks` jadvali hali bo'lmasligi mumkin edi.

#### Keyin (to'g'ri)

```java
@EventListener(ApplicationReadyEvent.class)
public void ensureHnswIndex() {
    try {
        jdbcTemplate.execute(HNSW_INDEX_SQL);
        log.info("HNSW indeks tayyor: law_chunks_embedding_hnsw_idx");
    } catch (Exception e) {
        log.warn("HNSW indeks yaratib bo'lmadi: {}", e.getMessage());
    }
}
```

| O'zgarish | Sabab |
|-----------|-------|
| `@PostConstruct` → `@EventListener(ApplicationReadyEvent.class)` | Ilova **to'liq tayyor** bo'lgach ishga tushadi |
| Hibernate allaqachon `law_chunks` yaratgan | HNSW indeks muvaffaqiyatli qo'shiladi |

**HNSW indeks SQL:**

```sql
CREATE INDEX IF NOT EXISTS law_chunks_embedding_hnsw_idx
ON law_chunks USING hnsw (embedding vector_cosine_ops);
```

---

## 4. Yangi initialization oqimi (to'liq diagramma)

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot ishga tushadi                │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  PgVectorExtensionConfig (@AutoConfigureBefore Hibernate)   │
│  pgVectorExtensionInitializer bean yaratiladi                 │
│  → CREATE EXTENSION IF NOT EXISTS vector                      │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  entityManagerFactory (depends-on: pgVectorExtensionInitializer)│
│  Hibernate ddl-auto=update                                  │
│  → legal_documents jadvali                                  │
│  → law_chunks jadvali (embedding vector(768) ustuni bilan)  │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  schema.sql (spring.sql.init.mode=always, defer=true)       │
│  → CREATE EXTENSION IF NOT EXISTS vector (qo'shimcha)       │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  ApplicationReadyEvent — ilova to'liq tayyor                │
│  RagDatabaseConfig.ensureHnswIndex()                        │
│  → CREATE INDEX law_chunks_embedding_hnsw_idx ...           │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. O'zgartirilgan / qo'shilgan fayllar ro'yxati

| Fayl | Holat | Tavsif |
|------|-------|--------|
| `src/main/resources/schema.sql` | **YANGI** | pgvector extension SQL |
| `src/main/resources/application.properties` | **O'ZGARTIRILDI** | SQL init, defer, flyway sozlamalari |
| `src/main/java/.../config/PgVectorExtensionConfig.java` | **YANGI** | Extension Hibernate dan oldin |
| `src/main/java/.../config/RagDatabaseConfig.java` | **O'ZGARTIRILDI** | `@PostConstruct` → `ApplicationReadyEvent` |
| `src/main/java/.../entity/LawChunkEntity.java` | Tekshirildi | O'zgartirish talab qilinmadi |

---

## 6. Tekshirish (verification)

### 6.1. Kompilyatsiya va testlar

```bash
cd api.ailawyer.uz
./mvnw compile
./mvnw test
```

Kutilgan natija: **0 xato**.

### 6.2. Ilova loglarida qidirish

Muvaffaqiyatli ishga tushganda:

```
INFO  ... PgVectorExtensionConfig - PostgreSQL pgvector extension tayyor
INFO  ... RagDatabaseConfig - HNSW indeks tayyor: law_chunks_embedding_hnsw_idx
```

pgvector o'rnatilmagan bo'lsa (dev/test):

```
WARN  ... PgVectorExtensionConfig - pgvector extension yaratib bo'lmadi: ...
```

### 6.3. PostgreSQL SQL tekshiruvlari

```sql
-- 1. pgvector extension mavjudligi
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
-- Kutilgan: vector | 0.x.x

-- 2. law_chunks jadvali mavjudligi
SELECT table_name FROM information_schema.tables
WHERE table_name = 'law_chunks';
-- Kutilgan: law_chunks

-- 3. embedding ustuni tipi
SELECT column_name, udt_name
FROM information_schema.columns
WHERE table_name = 'law_chunks' AND column_name = 'embedding';
-- Kutilgan: embedding | vector

-- 4. HNSW indeks mavjudligi
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'law_chunks'
  AND indexname = 'law_chunks_embedding_hnsw_idx';
-- Kutilgan: law_chunks_embedding_hnsw_idx | ... USING hnsw ...
```

### 6.4. Funksional test

1. Ilovani ishga tushiring
2. Admin sifatida `.docx` hujjat yuklang: `POST /api/v1/admin/legal-documents/upload`
3. `law_chunks does not exist` xatosi bo'lmasligi kerak
4. Embedding loglari: `Embedding boshlandi...` / `Embedding yakunlandi...`
5. AI chat orqali huquqiy savol bering — RAG ishlashi kerak

---

## 7. Production talablari

### 7.1. PostgreSQL da pgvector o'rnatish

Extension server darajasida o'rnatilgan bo'lishi **shart**. Aks holda `vector(768)` tipi yaratilmaydi.

**Docker (tavsiya etiladi):**

```bash
docker run -d \
  --name qalqon-postgres \
  -e POSTGRES_DB=legaltech_db \
  -e POSTGRES_USER=giybat_user \
  -e POSTGRES_PASSWORD=1234 \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

**Mavjud PostgreSQL ga o'rnatish (Linux):**

```bash
git clone https://github.com/pgvector/pgvector.git
cd pgvector
make
sudo make install
```

Keyin PostgreSQL da:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 7.2. Database foydalanuvchi huquqlari

`CREATE EXTENSION` odatda **superuser** yoki extension huquqiga ega foydalanuvchi talab qiladi. Agar `giybat_user` superuser bo'lmasa:

```sql
-- superuser sifatida bajarish
CREATE EXTENSION IF NOT EXISTS vector;

-- yoki foydalanuvchiga ruxsat berish
GRANT CREATE ON DATABASE legaltech_db TO giybat_user;
```

### 7.3. Mavjud buzilgan schema ni tiklash

Agar oldin noto'g'ri yaratilgan jadvallar bo'lsa:

```sql
-- Ehtiyotkorlik bilan! Ma'lumotlar o'chadi
DROP TABLE IF EXISTS law_chunks CASCADE;
DROP TABLE IF EXISTS legal_documents CASCADE;
```

Keyin ilovani qayta ishga tushiring — Hibernate jadvallarni to'g'ri yaratadi.

---

## 8. Muammolarni bartaraf etish (troubleshooting)

| Belgilar | Ehtimoliy sabab | Yechim |
|----------|-----------------|--------|
| `relation "law_chunks" does not exist` | pgvector yo'q, jadval yaratilmagan | pgvector o'rnatish, ilovani qayta ishga tushirish |
| `vector.control: No such file` | pgvector PostgreSQL ga o'rnatilmagan | pgvector build/install yoki Docker image |
| `Circular depends-on flyway/entityManagerFactory` | Flyway + defer=true ziddiyati | `spring.flyway.enabled=false` (allaqachon qo'shilgan) |
| HNSW indeks yaratilmadi (WARN log) | `law_chunks` hali bo'sh yoki pgvector yo'q | Extension va jadvalni tekshiring |
| Extension yaratildi, lekin jadval yo'q | Hibernate ddl-auto o'chirilgan | `spring.jpa.hibernate.ddl-auto=update` ni tekshiring |
| Embedding NULL qolmoqda | Gemini API kalit/noto'g'ri | `gemini.api.key` va embedding loglarini tekshiring |

---

## 9. Xulosa

| Muammo | Yechim |
|--------|--------|
| pgvector extension Hibernate dan oldin yo'q | `PgVectorExtensionConfig` + `schema.sql` |
| `law_chunks` jadvali yaratilmaydi | Extension → Hibernate tartibi `depends-on` bilan kafolatlandi |
| HNSW indeks erta yaratiladi | `@PostConstruct` → `ApplicationReadyEvent` |
| Flyway circular dependency | `spring.flyway.enabled=false` |
| Dev muhitda pgvector yo'q | `spring.sql.init.continue-on-error=true` + try-catch |

Ushbu o'zgarishlardan keyin RAG tizimi to'g'ri initialization tartibida ishga tushadi: **extension → jadval → indeks → upload/embedding/qidiruv**.

---

## 10. Bog'liq hujjatlar

- [`RAG.md`](./RAG.md) — RAG tizimining to'liq arxitekturasi (1–5 bosqich)
- [pgvector GitHub](https://github.com/pgvector/pgvector) — PostgreSQL extension o'rnatish
- [Spring Boot Database Initialization](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization) — `schema.sql` va defer sozlamalari
