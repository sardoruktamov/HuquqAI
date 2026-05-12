# AI/Lawyer Chat Isolation Refactor (Qalqon AI) — Summary

Sana: 2026-05-11  
Stack: Spring Boot 3.3.x, Java 17  
Maqsad: **AI chat** va **Advokat (Lawyer) chat** tarixlarini 100% ajratish (privacy/isolation).  
Natija: AI chat tarixi advokatga **hech qachon** o‘tmaydi; advokat bilan chat **noldan** boshlanadi.

---

## 1) Cleanup (eskilarini tozalash)

Quyidagi eski `Case` arxitekturasi va unga bog‘liq barcha chat fayllari **to‘liq o‘chirildi**:

- `api.giybat.uz/src/main/java/api/ailawyer/uz/entity/CaseEntity.java`
- `api.giybat.uz/src/main/java/api/ailawyer/uz/controller/CaseController.java`
- `api.giybat.uz/src/main/java/api/ailawyer/uz/service/CaseService.java`
- `api.giybat.uz/src/main/java/api/ailawyer/uz/repository/CaseRepository.java`
- `api.giybat.uz/src/main/java/api/ailawyer/uz/enums/CaseStatus.java`
- Case ichidagi chat’ga oid eski fayllar: `Case*Message*`, `Case*Channel*`, `CaseAccessService`, `CaseChannelService`, va ularning DTO/Repository/Controller’lari.

Repo bo‘yicha `Case*` ga referenslar tekshirildi va qolmagan.

---

## 2) Yangi DB schema (Entity’lar)

### 2.1 AI Chat (izolyatsiya)

- **AI chat**: `AiChatEntity`
  - `id: UUID`
  - `clientId: Integer` → `ProfileEntity`
  - `title: String`
  - `status: AiChatStatus (ACTIVE, CLOSED)`
  - `createdDate: LocalDateTime`

- **AI message**: `AiMessageEntity`
  - `id: UUID`
  - `aiChatId: UUID` → `AiChatEntity`
  - `senderType: AiMessageSenderType (USER, AI)`
  - `content: text`
  - `isEscalation: boolean` (default `false`)
  - `createdDate: LocalDateTime`

- **AI message attach link**: `AiMessageAttachEntity`
  - `messageId: UUID`
  - `attachId: String` → `AttachEntity`

### 2.2 Lawyer Chat (izolyatsiya)

- **Lawyer chat**: `LawyerChatEntity`
  - `id: UUID`
  - `clientId: Integer` → `ProfileEntity`
  - `lawyerId: Integer` → `ProfileEntity`
  - `status: LawyerChatStatus (ACTIVE, CLOSED)` (yaratilganda darhol `ACTIVE`)
  - `createdDate: LocalDateTime`

- **Lawyer message**: `LawyerMessageEntity`
  - `id: UUID`
  - `lawyerChatId: UUID` → `LawyerChatEntity`
  - `senderType: LawyerMessageSenderType (USER, LAWYER)`
  - `content: text`
  - `createdDate: LocalDateTime`

- **Lawyer message attach link**: `LawyerMessageAttachEntity`
  - `messageId: UUID`
  - `attachId: String` → `AttachEntity`

Yangi enum’lar:
- `AiChatStatus`, `AiMessageSenderType`
- `LawyerChatStatus`, `LawyerMessageSenderType`

---

## 3) Escalation (AI chat uchun)

`AiMessageService` ichida:
- User message saqlanadi
- AI provider’ga yuboriladi (hozircha stub)
- AI javobi saqlanadi
- AI javob matnida trigger so‘zlar bo‘lsa, `isEscalation=true`

Trigger so‘zlar ro‘yxati:
`["advokat", "sud", "jinoiy", "huquqshunos", "xavf", "sudya"]`

Frontend uchun:
- AI javob `AiMessageDTO.isEscalation=true` bo‘lsa UI “Advokatga bog‘lanish” tugmasini chiqarishi mumkin.

---

## 4) Lawyer chat start + Notification hook

Client advokat profiliga kirib birinchi xabarni yuborganda:
1) `LawyerChatEntity` **ACTIVE** holatda “ensure” qilinadi (mavjud bo‘lsa qayta ishlatiladi)
2) `LawyerMessageEntity` saqlanadi
3) `NotificationService.notifyNewMessage(...)` chaqiriladi (hozircha `log.info`)

Notification hozircha quyidagilarni qabul qiladi:
- `title`
- `body`
- `payload` (masalan `{ chatId: "..." }`)

---

## 5) Repository/Service/Controller’lar

### Repository’lar
- `AiChatRepository`, `AiMessageRepository`, `AiMessageAttachRepository`
- `LawyerChatRepository`, `LawyerMessageRepository`, `LawyerMessageAttachRepository`

### Service’lar
- `AiChatService` (create/list/get/close + access)
- `AiMessageService` (list + send user message → AI javob + escalation)
  - AI provider: `AiProvider` (hozircha `StubAiProvider` qo‘yilgan)
- `LawyerChatService` (list/get + ensureActiveChat + access)
- `LawyerMessageService` (list + startChatAndSend + sendMessage)
- `NotificationService` (hozircha log)

### Controller’lar (Swagger annotationlari bilan)
AI:
- `POST   /api/v1/ai-chats`
- `GET    /api/v1/ai-chats?page&size`
- `GET    /api/v1/ai-chats/{id}`
- `PUT    /api/v1/ai-chats/{id}/close`
- `GET    /api/v1/ai-chats/{aiChatId}/messages?page&size`
- `POST   /api/v1/ai-chats/{aiChatId}/messages`

Lawyer:
- `GET    /api/v1/lawyer-chats?page&size`
- `GET    /api/v1/lawyer-chats/{id}`
- `POST   /api/v1/lawyer-chats/start` (client birinchi xabar)
- `GET    /api/v1/lawyer-chats/{lawyerChatId}/messages?page&size`
- `POST   /api/v1/lawyer-chats/{lawyerChatId}/messages`

---

## 6) Security (Authentication’dan user olish)

Hamma servislar user identifikatsiyasi uchun mavjud util’dan foydalandi:
- `SpringSecurityUtil.getCurrentUserId()`
- `SpringSecurityUtil.hazRole(ProfileRole ...)`

Access qoidalari qisqacha:
- AI chat: odatda faqat owner client (admin/superadmin mustasno)
- Lawyer chat: client yoki o‘sha lawyer (admin/superadmin mustasno)

---

## 7) Build tekshiruvi

`api.giybat.uz` modulida:
- `.\\mvnw.cmd compile` → ✅ 0 ta error

