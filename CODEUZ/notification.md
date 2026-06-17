# BOSQICH 7 — FCM Push Notification

**Loyiha:** QalqonAI (`api.ailawyer.uz`)  
**Texnologiya:** Spring Boot 3, Java 17, Firebase Admin SDK  
**Maqsad:** Advokat chat va onboarding jarayonlari uchun asinxron FCM push-bildirishnomalar tizimi.

---

## Qisqacha xulosa

Tizimga Firebase Cloud Messaging (FCM) orqali push-bildirishnomalar yuborish mantiqi qo'shildi. Asosiy xabar saqlash va onboarding operatsiyalari **bloklanmaydi** — FCM so'rovlari Spring `ApplicationEvent` + `@Async` orqali fonda bajariladi.

**Qat'iy cheklovlar (bajarilmagan / qo'shilmagan):**
- Push xabarlarda ko'p tillilik (I18n) yo'q — matnlar hardcoded
- `ProfileEntity` ga `isPushEnabled` maydoni qo'shilmagan — barcha aktiv tokenlarga yuboriladi

---

## 1. Maven va konfiguratsiya

### `pom.xml`
Yangi qaramlik:
```xml
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.4.1</version>
</dependency>
```

### `application.properties`
```properties
firebase.enabled=false
firebase.credentials.path=
```

| Parametr | Tavsif |
|----------|--------|
| `firebase.enabled` | `true` bo'lsa FCM haqiqiy yuboriladi; `false` da eventlar ishlaydi, lekin Firebase ga so'rov ketmaydi |
| `firebase.credentials.path` | Firebase service account JSON fayl yo'li |

### Yangi konfiguratsiya klasslari

| Fayl | Vazifa |
|------|--------|
| `config/AsyncConfig.java` | `@EnableAsync`, `notificationExecutor` thread pool (core=2, max=8, queue=100) |
| `config/FirebaseProperties.java` | `firebase.*` property binding |
| `config/FirebaseConfig.java` | `@PostConstruct` da Firebase Admin SDK ishga tushirish (faqat `enabled=true` bo'lsa) |

---

## 2. Yangi fayllar (to'liq ro'yxat)

### Entity
| Fayl | Jadval | Maydonlar |
|------|--------|-----------|
| `entity/DeviceTokenEntity.java` | `device_token` | `id` (UUID), `profileId`, `token` (unique, 512), `platform`, `active`, `createdDate`, `lastUsedDate` |

### Repository
| Fayl | Metodlar |
|------|----------|
| `repository/DeviceTokenRepository.java` | `findAllByProfileIdAndActiveTrue`, `findByProfileIdAndToken`, `findByToken` |

### Enum
| Fayl | Qiymatlar |
|------|-----------|
| `enums/NotificationType.java` | `LAWYER_NEW_MESSAGE`, `CLIENT_NEW_MESSAGE`, `LAWYER_ONBOARDING_PENDING`, `LAWYER_ONBOARDING_APPROVED`, `LAWYER_ONBOARDING_REJECTED` |
| `enums/DevicePlatform.java` | `ANDROID`, `IOS` |

### DTO
| Fayl | Maydonlar |
|------|-----------|
| `dto/notification/DeviceTokenRegisterDTO.java` | `token` (NotBlank), `platform` (NotNull) |

### Event
| Fayl | Tarkib |
|------|--------|
| `event/PushNotificationEvent.java` | `record`: `type`, `title`, `body`, `targetProfileIds`, `data` |

### Service
| Fayl | Vazifa |
|------|--------|
| `service/DeviceTokenService.java` | Token ro'yxatdan o'tkazish va deaktivatsiya |
| `service/NotificationService.java` | Event publisher — push trigger metodlari |
| `service/FcmNotificationSender.java` | Firebase ga haqiqiy yuborish, o'lik token tozalash |
| `service/PushNotificationEventListener.java` | `@EventListener` + `@Async` — fonda FCM yuborish |

### Controller
| Fayl | Endpointlar |
|------|-------------|
| `controller/NotificationController.java` | `POST/DELETE /api/v1/notifications/device-token` |

### HTTP test
| Fayl | Maqsad |
|------|--------|
| `src/main/resources/http/notifications.http` | FCM token register/delete va chat push trigger testlari |

### Unit test
| Fayl | Testlar |
|------|---------|
| `test/.../NotificationServiceTest.java` | Event publish tekshiruvi (`LAWYER_NEW_MESSAGE`, `LAWYER_ONBOARDING_PENDING`) |

---

## 3. O'zgartirilgan fayllar

### `entity/LawyerChatEntity.java`
Yangi maydonlar (unread count uchun):
- `lastReadMessageIdByClient` — mijoz oxirgi o'qigan xabar id si
- `lastReadMessageIdByLawyer` — advokat oxirgi o'qigan xabar id si

### `repository/LawyerMessageRepository.java`
Yangi query:
- `countUnreadMessages(chatId, senderType, afterCreatedDate)` — qarama-qarshi tomondan kelgan o'qilmagan xabarlar soni

### `repository/ProfileRoleRepository.java`
Yangi metod:
- `findDistinctProfileIdsByRolesIn(List<ProfileRole>)` — admin push uchun admin/superadmin profil id lari

### `service/LawyerChatService.java`
- `markAsReadForSender()` — xabar yuborilganda yuboruvchi uchun o'qilgan deb belgilash
- `markAsReadToLatest()` — chat xabarlar ro'yxati ochilganda eng oxirgi xabargacha o'qilgan deb belgilash
- `resolveUnreadCount()` — chat DTO da `unreadCount` hisoblash

### `service/LawyerMessageService.java`
- `startChat()` — chat boshlanganda `notifyLawyerNewMessage()`
- `sendMessage()` — mijoz yozsa `notifyLawyerNewMessage()`, advokat yozsa `notifyClientNewMessage()`
- `list()` — xabarlar ochilganda `markAsReadToLatest()`
- `saveMessage()` — saqlangandan keyin `markAsReadForSender()`

### `service/LawyerProfileService.java`
- `submit()` — `notifyLawyerOnboardingPending()` (adminlarga)
- `approve()` — `notifyLawyerOnboardingApproved()` (advokatga)
- `reject()` — `notifyLawyerOnboardingRejected()` (advokatga)

### `dto/lawyerchat/LawyerChatDTO.java`
- `unreadCount` maydoni endi `LawyerChatService` orqali to'g'ri hisoblanadi

### Test fayllar
| Fayl | O'zgarish |
|------|-----------|
| `LawyerMessageServiceTest.java` | `NotificationService` mock, `notifyLawyerNewMessage` verify |
| `LawyerProfileServiceTest.java` | `NotificationService` mock qo'shildi |

---

## 4. REST API

### Device Token

**Ro'yxatdan o'tkazish (login dan keyin)**
```
POST /api/v1/notifications/device-token
Authorization: Bearer {token}
Content-Type: application/json

{
  "token": "FCM_DEVICE_TOKEN",
  "platform": "ANDROID"
}
```

**O'chirish / deaktivatsiya (logout da)**
```
DELETE /api/v1/notifications/device-token?token=FCM_DEVICE_TOKEN
Authorization: Bearer {token}
```

**Ruxsat:** `ROLE_USER`, `ROLE_LAWYER`, `ROLE_ADMIN`, `ROLE_SUPERADMIN`

### DeviceTokenService mantiq
- Token boshqa profilga tegishli bo'lsa — eski yozuv `active=false` qilinadi
- Bir xil token qayta yuborilsa — yangilanadi (`active=true`, `lastUsedDate` yangilanadi)
- Logout da faqat joriy foydalanuvchining tokeni deaktivatsiya qilinadi

---

## 5. Push bildirishnoma turlari

| `NotificationType` | Kimga | Trigger | Title | Body |
|--------------------|-------|---------|-------|------|
| `LAWYER_NEW_MESSAGE` | Advokat | Mijoz xabar yozganda | Sizda yangi xabar | Mijozdan yangi xabar keldi |
| `CLIENT_NEW_MESSAGE` | Mijoz | Advokat javob berganda | Sizda yangi xabar | Advokatdan javob keldi |
| `LAWYER_ONBOARDING_PENDING` | Adminlar | Onboarding submit | Yangi advokat arizasi | Tasdiqlash uchun yangi advokat profili yuborildi |
| `LAWYER_ONBOARDING_APPROVED` | Advokat | Admin approve | Profil tasdiqlandi | Advokat profilingiz admin tomonidan tasdiqlandi |
| `LAWYER_ONBOARDING_REJECTED` | Advokat | Admin reject | Profil rad etildi | Advokat profilingiz rad etildi. Sababni ko'rib chiqing |

### Push `data` payload (qo'shimcha)
Har bir eventda `type` kaliti bor. Qo'shimcha kalitlar:
- Chat: `chatId`, `lawyerId` yoki `clientId`
- Onboarding: `profileId`, reject da `reason`

---

## 6. Asinxron arxitektura

```
[LawyerMessageService / LawyerProfileService]
        │
        ▼
[NotificationService.publishEvent()]
        │
        ▼
[ApplicationEventPublisher → PushNotificationEvent]
        │
        ▼ (tranzaksiya tugagach, alohida thread)
[PushNotificationEventListener @Async("notificationExecutor")]
        │
        ▼
[FcmNotificationSender.sendToProfile()]
        │
        ▼
[Firebase Cloud Messaging]
```

**Muhim:** FCM so'rovi asosiy HTTP request thread da **ishlamaydi**. Xabar DB ga saqlanadi, tranzaksiya commit bo'ladi, keyin event fonda qayta ishlanadi.

---

## 7. O'lik token tozalash (Dead Token Cleanup)

`FcmNotificationSender` da `FirebaseMessagingException` ushlanadi. Quyidagi holatlarda token `active = false` qilinadi:

| Xatolik | Izoh |
|---------|------|
| `MessagingErrorCode.UNREGISTERED` | Ilova o'chirilgan yoki token bekor qilingan |
| `MessagingErrorCode.INVALID_ARGUMENT` | Token noto'g'ri yoki eskirgan |
| Xabar matnida `NOT_FOUND` | Firebase token topilmadi |

Muvaffaqiyatli yuborishda `lastUsedDate` yangilanadi.

---

## 8. Unread Count (o'qilmagan xabarlar)

### Hisoblash mantiq
1. Har chatda ikkita "oxirgi o'qilgan xabar" pointer bor:
   - `lastReadMessageIdByClient`
   - `lastReadMessageIdByLawyer`
2. Ko'ruvchi kim bo'lsa, qarama-qarshi tomondan kelgan va shu pointer dan keyingi xabarlar sanaladi.
3. `GET /api/v1/lawyer-chats` javobida har bir chat uchun `unreadCount` qaytariladi.

### O'qilgan deb belgilash
| Hodisa | Metod |
|--------|-------|
| Xabar yuborish | `markAsReadForSender()` — yuboruvchi o'z xabarini o'qigan deb hisoblanadi |
| Chat xabarlarini ochish | `markAsReadToLatest()` — eng oxirgi xabargacha o'qilgan |

---

## 9. Ma'lumotlar bazasi (yangi jadval)

### `device_token`
```sql
-- JPA orqali avtomatik yaratiladi (ddl-auto bo'yicha)
CREATE TABLE device_token (
    id UUID PRIMARY KEY,
    profile_id INTEGER NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    platform VARCHAR(255) NOT NULL,  -- ANDROID | IOS
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP NOT NULL,
    last_used_date TIMESTAMP
);
-- Index: profile_id, token
```

### `lawyer_chat` (yangi ustunlar)
```sql
ALTER TABLE lawyer_chat ADD COLUMN last_read_message_id_by_client UUID;
ALTER TABLE lawyer_chat ADD COLUMN last_read_message_id_by_lawyer UUID;
```

---

## 10. Ishga tushirish (Production)

1. [Firebase Console](https://console.firebase.google.com/) da loyiha yarating
2. Service Account JSON yuklab oling
3. `application.properties` yoki env orqali sozlang:
   ```properties
   firebase.enabled=true
   firebase.credentials.path=/path/to/service-account.json
   ```
4. Mobil ilova login dan keyin `POST /api/v1/notifications/device-token` chaqirsin
5. Logout da `DELETE /api/v1/notifications/device-token` chaqirsin

**Dev rejim:** `firebase.enabled=false` — kod va eventlar ishlaydi, lekin Firebase ga so'rov ketmaydi (logda `FCM disabled — skip push`).

---

## 11. Test qilish

### Kompilyatsiya
```bash
cd api.ailawyer.uz
.\mvnw.cmd compile
```

### Unit testlar
```bash
.\mvnw.cmd test "-Dtest=NotificationServiceTest,LawyerMessageServiceTest,LawyerProfileServiceTest,LawyerChatServiceTest"
```

### HTTP test
`api.ailawyer.uz/src/main/resources/http/notifications.http` — IntelliJ/VS Code REST Client bilan.

---

## 12. Fayl xaritasi (BOSQICH 7)

```
api.ailawyer.uz/
├── pom.xml                                          [O'ZGARTIRILDI]
├── src/main/
│   ├── java/api/ailawyer/uz/
│   │   ├── config/
│   │   │   ├── AsyncConfig.java                     [YANGI]
│   │   │   ├── FirebaseConfig.java                  [YANGI]
│   │   │   └── FirebaseProperties.java              [YANGI]
│   │   ├── controller/
│   │   │   └── NotificationController.java          [YANGI]
│   │   ├── dto/
│   │   │   ├── lawyerchat/LawyerChatDTO.java        [O'ZGARTIRILDI — unreadCount]
│   │   │   └── notification/DeviceTokenRegisterDTO.java [YANGI]
│   │   ├── entity/
│   │   │   ├── DeviceTokenEntity.java               [YANGI]
│   │   │   └── LawyerChatEntity.java                [O'ZGARTIRILDI]
│   │   ├── enums/
│   │   │   ├── DevicePlatform.java                  [YANGI]
│   │   │   └── NotificationType.java                [YANGI]
│   │   ├── event/
│   │   │   └── PushNotificationEvent.java           [YANGI]
│   │   ├── repository/
│   │   │   ├── DeviceTokenRepository.java           [YANGI]
│   │   │   ├── LawyerMessageRepository.java         [O'ZGARTIRILDI]
│   │   │   └── ProfileRoleRepository.java           [O'ZGARTIRILDI]
│   │   └── service/
│   │       ├── DeviceTokenService.java              [YANGI]
│   │       ├── FcmNotificationSender.java           [YANGI]
│   │       ├── NotificationService.java             [YANGI]
│   │       ├── PushNotificationEventListener.java   [YANGI]
│   │       ├── LawyerChatService.java               [O'ZGARTIRILDI]
│   │       ├── LawyerMessageService.java            [O'ZGARTIRILDI]
│   │       └── LawyerProfileService.java            [O'ZGARTIRILDI]
│   ├── resources/
│   │   ├── application.properties                   [O'ZGARTIRILDI]
│   │   └── http/notifications.http                  [YANGI]
│   └── test/java/api/ailawyer/uz/service/
│       ├── NotificationServiceTest.java               [YANGI]
│       ├── LawyerMessageServiceTest.java            [O'ZGARTIRILDI]
│       └── LawyerProfileServiceTest.java            [O'ZGARTIRILDI]
```

---

## 13. Keyingi bosqichlar (tavsiya)

- Mobil ilovada FCM integratsiyasi (token register/delete lifecycle)
- Push tarix (in-app notification inbox) — hozircha yo'q
- `ProfileEntity.isPushEnabled` — foydalanuvchi sozlamalari
- Ko'p tillik push matnlari (I18n)
- `FcmNotificationSender` uchun alohida unit test (mock Firebase)

---

*BOSQICH 7 yakunlandi. Kompilyatsiya va tegishli unit testlar muvaffaqiyatli o'tgan.*
