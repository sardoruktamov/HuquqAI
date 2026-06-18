# Level 2 Audit Log — Harakatlar tarixi

**Loyiha:** QalqonAI (`api.ailawyer.uz`)  
**Texnologiya:** Spring Boot 3, Java 17, Spring Data JPA, `@Async`  
**Maqsad:** Tizimdagi muhim harakatlarni (hozircha advokat onboarding tasdiqlash/rad etish) alohida `audit_logs` jadvalida saqlab qolish va kim, qachon, nima qilganini keyinchalik tekshirish imkonini berish.

---

## Qisqacha xulosa

QalqonAI da **Level 2 Audit Log** tizimi joriy qilindi. Bu yondashuvda audit ma'lumotlari biznes jadvaliga (`lawyer_profile`) aralashmaydi, balki universal `audit_logs` jadvaliga yoziladi.

Hozirgi integratsiya doirasi:
- Admin advokat arizasini **tasdiqlaganda** (`APPROVE`)
- Admin advokat arizasini **rad etganda** (`REJECT`)

Audit yozuvi **asinxron** (`@Async`) bajariladi — asosiy biznes operatsiyasi (profil holatini yangilash) audit yozuvini kutmaydi.

---

## Level 1 vs Level 2

| Yondashuv | Tavsif | Holat |
|-----------|--------|-------|
| **Level 1** | `lawyer_profile` jadvaliga `reviewerProfileId`, `reviewedAt` kabi maydonlar qo'shish | **Ishlatilmaydi** — loyihada qo'llanilmagan |
| **Level 2** | Alohida `audit_logs` jadvali + universal `AuditLogService` | **Joriy yechim** |

Level 1 yondashuvi rad etildi, chunki:
- Har bir jadvalga alohida audit maydonlari qo'shish aralash va kengayishga qiyin model beradi
- Bir nechta turdagi harakatlarni bitta joyda qidirish qiyinlashadi
- Universal audit servisi kelajakda boshqa modullarga ham oson ulanadi

---

## Ma'lumotlar bazasi: `audit_logs`

Jadval Hibernate `ddl-auto=update` orqali avtomatik yaratiladi (Flyway migratsiyasi yo'q).

### Jadval strukturasi

| Ustun | Tip | Majburiy | Tavsif |
|-------|-----|----------|--------|
| `id` | UUID | Ha | Primary key |
| `entity_name` | VARCHAR(120) | Ha | Qaysi jadval/entity da o'zgarish bo'lgani (masalan: `lawyer_profile`) |
| `entity_id` | VARCHAR(64) | Ha | O'zgartirilgan obyektning ID si (string ko'rinishda) |
| `action` | VARCHAR(64) | Ha | Bajarilgan harakat (masalan: `APPROVE`, `REJECT`) |
| `performed_by` | INTEGER | Ha | Harakatni bajargan foydalanuvchining `profile_id` si |
| `performed_at` | TIMESTAMP | Ha | Harakat bajarilgan vaqt |
| `details` | TEXT | Yo'q | Qo'shimcha ma'lumot (masalan, rad etish sababi) |

### Indekslar

| Indeks nomi | Ustunlar | Maqsad |
|-------------|----------|--------|
| `idx_audit_logs_entity` | `entity_name`, `entity_id` | Muayyan obyekt bo'yicha tarixni qidirish |
| `idx_audit_logs_performed_by` | `performed_by` | Qaysi admin nima qilganini qidirish |
| `idx_audit_logs_performed_at` | `performed_at` | Vaqt bo'yicha filtrlash |

### Namuna yozuvlar

**Tasdiqlash (APPROVE):**

| id | entity_name | entity_id | action | performed_by | performed_at | details |
|----|-------------|-----------|--------|--------------|--------------|---------|
| `a1b2c3...` | `lawyer_profile` | `7` | `APPROVE` | `1` | `2026-06-18 18:15:00` | `NULL` |

**Rad etish (REJECT):**

| id | entity_name | entity_id | action | performed_by | performed_at | details |
|----|-------------|-----------|--------|--------------|--------------|---------|
| `d4e5f6...` | `lawyer_profile` | `7` | `REJECT` | `2` | `2026-06-18 18:20:00` | `Hujjat xato` |

---

## Yangi fayllar

### Entity

| Fayl | Vazifa |
|------|--------|
| `entity/AuditLogEntity.java` | `audit_logs` jadvali entity klassi |

### Repository

| Fayl | Vazifa |
|------|--------|
| `repository/AuditLogRepository.java` | `JpaRepository<AuditLogEntity, UUID>` |

### Service

| Fayl | Vazifa |
|------|--------|
| `service/AuditLogService.java` | Universal `logAction(...)` metodi — audit yozuvini saqlaydi |

### O'zgartirilgan fayllar

| Fayl | O'zgarish |
|------|-----------|
| `config/AsyncConfig.java` | `auditExecutor` thread pool qo'shildi |
| `service/LawyerProfileService.java` | `approve()` va `reject()` metodlariga audit chaqiruvi ulandi |

### Test fayllar

| Fayl | Vazifa |
|------|--------|
| `test/.../AuditLogServiceTest.java` | Audit yozuvi to'g'ri yaratilishini tekshiradi |
| `test/.../LawyerProfileServiceTest.java` | Approve/reject da `auditLogService.logAction(...)` chaqirilishini verify qiladi |

---

## `AuditLogService` — universal API

```java
@Async("auditExecutor")
@Transactional
public void logAction(
    String entityName,
    String entityId,
    String action,
    Integer performedBy,
    String details
)
```

| Parametr | Tavsif | Misol |
|----------|--------|-------|
| `entityName` | Entity/jadval nomi | `"lawyer_profile"` |
| `entityId` | O'zgartirilgan obyekt ID si (string) | `"7"` |
| `action` | Harakat turi | `"APPROVE"`, `"REJECT"` |
| `performedBy` | Amalni bajargan foydalanuvchi `profile_id` | `1` |
| `details` | Qo'shimcha matn (ixtiyoriy) | `"Hujjat xato"` yoki `null` |

**Xususiyatlar:**
- `@Async("auditExecutor")` — HTTP so'rov threadini bloklamaydi
- `@Transactional` — alohida async threadda DB transaksiyasi ochiladi
- Muvaffaqiyatli saqlangandan keyin DEBUG log chiqadi:
  ```
  Audit log yozildi entity=lawyer_profile, entityId=7, action=APPROVE, performedBy=1
  ```

---

## Asinxron konfiguratsiya

`AsyncConfig` da audit uchun alohida thread pool:

| Parametr | Qiymat |
|----------|--------|
| Bean nomi | `auditExecutor` |
| Core pool size | 1 |
| Max pool size | 4 |
| Queue capacity | 200 |
| Thread prefix | `audit-` |

Console da audit threadlari `audit-1`, `audit-2` ko'rinishida paydo bo'ladi.

> **Eslatma:** Push bildirishnomalar uchun alohida `notificationExecutor` ishlatiladi. Audit va notification bir-biriga aralashmaydi.

---

## Biznes mantiq integratsiyasi

### Oqim diagrammasi

```
Admin HTTP so'rovi
    │
    ▼
LawyerAdminController
    │  PUT /api/v1/admin/lawyers/{id}/approve
    │  PUT /api/v1/admin/lawyers/{id}/reject
    ▼
LawyerProfileService.approve() / reject()
    │
    ├─► lawyer_profile holati yangilanadi (sinxron, @Transactional)
    │
    ├─► AuditLogService.logAction(...)  ──► @Async audit thread
    │       └─► audit_logs jadvaliga INSERT
    │
    └─► NotificationService (push event publish)
```

### `LawyerProfileService` konstantalari

```java
private static final String AUDIT_ENTITY_LAWYER_PROFILE = "lawyer_profile";
private static final String AUDIT_ACTION_APPROVE = "APPROVE";
private static final String AUDIT_ACTION_REJECT = "REJECT";
```

### `approve(profileId)` — tasdiqlash

1. Profil `PENDING` holatda ekanligi tekshiriladi
2. `onboarding_status = APPROVED`, `verified_at = now()` saqlanadi
3. Audit yoziladi:
   ```java
   auditLogService.logAction(
       "lawyer_profile",
       String.valueOf(profileId),
       "APPROVE",
       SpringSecurityUtil.getCurrentUserId(),
       null
   );
   ```
4. Advokatga push bildirishnoma yuboriladi

### `reject(profileId, dto)` — rad etish

1. Profil `PENDING` holatda ekanligi tekshiriladi
2. `onboarding_status = REJECTED`, `rejection_reason` saqlanadi
3. Audit yoziladi:
   ```java
   auditLogService.logAction(
       "lawyer_profile",
       String.valueOf(profileId),
       "REJECT",
       SpringSecurityUtil.getCurrentUserId(),
       rejectionReason
   );
   ```
4. Advokatga push bildirishnoma yuboriladi

### Kimning ID si yoziladi?

`performed_by` maydoniga JWT orqali autentifikatsiya qilingan **joriy adminning** `profile_id` si yoziladi:

```java
SpringSecurityUtil.getCurrentUserId()
```

Bu qiymat `SecurityContextHolder` dagi `CustomUserDetails` dan olinadi. Admin endpointlari `@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')")` bilan himoyalangan.

---

## API endpointlar (o'zgarishsiz)

Audit tizimi **yangi REST endpoint qo'shmaydi**. Mavjud admin endpointlari orqali ishlaydi:

| Metod | URL | Rol | Audit action |
|-------|-----|-----|--------------|
| PUT | `/api/v1/admin/lawyers/{id}/approve` | ADMIN, SUPERADMIN | `APPROVE` |
| PUT | `/api/v1/admin/lawyers/{id}/reject` | ADMIN, SUPERADMIN | `REJECT` |

Response DTO (`LawyerProfileDTO`) o'zgarmagan — audit ma'lumotlari alohida jadvalda saqlanadi, API javobida qaytmaydi.

---

## Ma'lumotlar bazasini tekshirish

DBeaver, pgAdmin yoki IntelliJ Database plugin orqali:

```sql
-- Oxirgi audit yozuvlari
SELECT *
FROM audit_logs
ORDER BY performed_at DESC
LIMIT 20;

-- Muayyan advokat bo'yicha tarix
SELECT *
FROM audit_logs
WHERE entity_name = 'lawyer_profile'
  AND entity_id = '7'
ORDER BY performed_at DESC;

-- Muayyan admin bajargan harakatlar
SELECT *
FROM audit_logs
WHERE performed_by = 1
ORDER BY performed_at DESC;
```

---

## Testlar

### `AuditLogServiceTest`

- `logAction(...)` chaqirilganda `AuditLogRepository.save()` to'g'ri entity bilan chaqirilishini tekshiradi

### `LawyerProfileServiceTest`

| Test | Tekshiruv |
|------|-----------|
| `approve_pending_logsAuditAction` | PENDING profil tasdiqlanganda `logAction("lawyer_profile", "7", "APPROVE", 1, null)` chaqiriladi |
| `reject_pending_logsAuditActionWithReason` | PENDING profil rad etilganda `logAction(..., "REJECT", 2, "Hujjat xato")` chaqiriladi |

Barcha testlar o'tkazish:
```bash
cd api.ailawyer.uz
./mvnw test
```

---

## Kelajakda kengaytirish

`AuditLogService.logAction(...)` universal dizaynga ega. Yangi modul qo'shish uchun:

1. Biznes servisda muhim harakat bajarilgandan **keyin** `auditLogService.logAction(...)` chaqiring
2. `entityName` — jadval/entity nomi (masalan: `"lawyer_chat"`, `"post"`)
3. `action` — aniq harakat (masalan: `"DELETE"`, `"BAN"`, `"UPDATE"`)
4. `performedBy` — `SpringSecurityUtil.getCurrentUserId()`
5. `details` — kerak bo'lsa JSON yoki matn

**Hozircha qo'shilmagan (keyingi bosqichlar uchun):**
- Audit loglarni o'qish uchun Admin REST API
- Audit action uchun enum (`AuditAction`)
- `@CreatedBy` / JPA Auditing global tizimi
- Audit log export (CSV/PDF)

---

## Fayl tuzilmasi (xulosa)

```
api.ailawyer.uz/src/main/java/api/ailawyer/uz/
├── config/
│   └── AsyncConfig.java                    [O'ZGARTIRILDI — auditExecutor]
├── entity/
│   └── AuditLogEntity.java                 [YANGI]
├── repository/
│   └── AuditLogRepository.java             [YANGI]
└── service/
    ├── AuditLogService.java                [YANGI]
    └── LawyerProfileService.java           [O'ZGARTIRILDI — approve/reject audit]

api.ailawyer.uz/src/test/java/api/ailawyer/uz/service/
├── AuditLogServiceTest.java                [YANGI]
└── LawyerProfileServiceTest.java           [O'ZGARTIRILDI]
```

---

## Xulosa

Level 2 Audit Log tizimi QalqonAI ga muvaffaqiyatli ulandi:

- Audit ma'lumotlari **alohida** `audit_logs` jadvalida saqlanadi
- Advokat onboarding **APPROVE** va **REJECT** harakatlari avtomatik yoziladi
- Yozuv **asinxron** bajariladi — API tezligi ta'sirlanmaydi
- Kim bajargani (`performed_by`) JWT kontekstidan olinadi
- Rad etish sababi `details` ustunida saqlanadi
- Barcha unit testlar muvaffaqiyatli o'tadi
