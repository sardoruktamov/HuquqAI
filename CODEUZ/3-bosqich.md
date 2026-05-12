# 3‑bosqich — Case system (Case ichida chat) yakuniy hujjat

Ushbu hujjat **BOSQICH 3** doirasida qilingan o‘zgarishlarni jamlaydi: `Case` ichida **2 ta alohida chat** (AI va Advokat) hamda **text + attach** qo‘llab‑quvvatlashi.

## 1) Mazmun (Asosiy g‘oya)

- `Case` — ilovaning **root** obyektidir.
- Har bir `Case` ichida **2 ta kanal** bor:
  - **AI** kanali: mijoz ↔ AI
  - **LAWYER** kanali: mijoz ↔ advokat
- Chat bo‘lmasa, AI ham, advokat ham ishlamaydi. Shuning uchun **chat hamisha `caseId`ga bog‘lanadi**.

## 2) Yangi entity’lar (DB modeli)

### 2.1 `cases` (mavjud)
- `CaseEntity` kengaydi:
  - `closedDate` qo‘shildi (case yopilganda vaqt yoziladi).

### 2.2 `case_channels` (yangi)
Har bir `caseId` uchun 2 ta row:
- `(caseId, AI)`
- `(caseId, LAWYER)`

Maydonlar:
- `caseId`
- `type`: `AI | LAWYER`
- `status`: `ACTIVE | LOCKED`
- `createdDate`

Qoidalar:
- `case_id + type` **unique**.
- Case yopilganda kanallar `LOCKED` bo‘ladi.

### 2.3 `case_messages` (yangi)
Chat message’lar jadvali.

Maydonlar:
- `caseId`
- `channelType`: `AI | LAWYER`
- `senderType`: `CLIENT | AI | LAWYER | SYSTEM`
- `senderId` (AI/SYSTEM uchun null)
- `content`
- `status`: `SENT | DELETED` (soft delete uchun)
- `replyToMessageId` (ixtiyoriy)
- `createdDate`

### 2.4 `case_message_attach` (yangi)
Message ↔ Attach bog‘lovchi jadval.
- `messageId`
- `attachId`
- `createdDate`

## 3) REST API endpointlar

Base: `/api/v1/cases`

### 3.1 Case
- `POST /api/v1/cases` — Case yaratish (case yaratilganda 2 ta kanal ham avtomatik yaratiladi)
- `GET /api/v1/cases?page&size` — Caselar ro‘yxati (rolga qarab scope)
- `GET /api/v1/cases/{id}` — Case detail (rol/ownership tekshiriladi)
- `PUT /api/v1/cases/{id}/close` — Case yopish (case yopilganda kanallar lock bo‘ladi)

### 3.2 Case Channels
- `GET /api/v1/cases/{caseId}/channels`
  - Natija: `AI` va `LAWYER` kanallari (statuslari bilan).

### 3.3 AI chat
- `GET /api/v1/cases/{caseId}/ai/messages?page&size`
- `POST /api/v1/cases/{caseId}/ai/messages`
  - Body: `{ "content": "...", "attachIds": ["..."], "replyToMessageId": "..." }`

### 3.4 Lawyer chat
- `GET /api/v1/cases/{caseId}/lawyer/messages?page&size`
- `POST /api/v1/cases/{caseId}/lawyer/messages`
  - Agar user `ROLE_LAWYER` bo‘lsa — message advokat nomidan yoziladi
  - Aks holda — mijoz nomidan yoziladi

## 4) Permission (xavfsizlik) qoidalari

Rollar: `ROLE_USER`, `ROLE_LAWYER`, `ROLE_ADMIN`, `ROLE_SUPERADMIN`.

### 4.1 Case ko‘rish (read)
- **Client**: faqat o‘z case’i (`case.clientId == me`)
- **Lawyer**: faqat o‘ziga biriktirilgan case (`case.lawyerId == me`)
- **Admin/Superadmin**: hammasi

### 4.2 Chat yozish (write)
- Case `CLOSED` bo‘lsa — **hech kim yozolmaydi**.
- **AI kanal**: faqat case owner client (yoki admin) yozadi.
- **LAWYER kanal**:
  - mijoz yozishi uchun `case.lawyerId` biriktirilgan bo‘lishi shart (aks holda “advokat biriktirilmagan” xatosi)
  - advokat yozishi uchun `case.lawyerId == me` bo‘lishi shart (adminlar mustasno)

## 5) Attach oqimi (chatda fayl yuborish)

1) Client file yuboradi:
- `POST /api/v1/attach/upload` → `AttachDTO { id, url, ... }`

2) Chat message yuboradi:
- `POST /ai/messages` yoki `/lawyer/messages` body’da `attachIds: ["<attachId>"]`

3) Server:
- attach mavjudligini tekshiradi (`AttachService.getEntity`)
- `case_message_attach` ga link yozadi
- response’da `attachments: [{id,url}]` qaytaradi

## 6) BOSQICH 4 (Gemini) uchun tayyorgarlik (hook)

Hozircha AI provider implement qilinmagan, lekin keyingi bosqichga tayyor “kontrakt” qo‘yildi:
- `AiProvider.generate(AiRequest) -> AiResponse`
- `AiResponse` ichida: `model`, `tokensIn/tokensOut`, `latencyMs`, `ragUsed`
- `AiPromptVersion.V1` — system prompt versiyalash uchun

## 7) Real user-flow (hayotiy misollar)

### Misol A: Oddiy foydalanuvchi AI’dan maslahat oladi
1) Login qiladi (JWT oladi).
2) `POST /api/v1/cases` → “Meros masalasi bo‘yicha savolim bor”
   - Natija: `caseId` qaytadi, `AI` va `LAWYER` kanal avtomatik yaratiladi.
3) `POST /api/v1/cases/{caseId}/ai/messages`
   - content: “Otam vafot etdi, meros qanday taqsimlanadi?”
4) Hozirgi BOSQICH 3 da: message DBga yoziladi va tarixda ko‘rinadi.
5) BOSQICH 4 da: shu endpoint avtomatik Gemini’ga yuborib, AI javobini ham `case_messages` ga yozadi.

### Misol B: Foydalanuvchi hujjat rasmini ilova qilib AI’ga yuboradi
1) `POST /api/v1/attach/upload` → `attachId`
2) `POST /api/v1/cases/{caseId}/ai/messages`:
   - content: “Shu shartnomani ko‘rib bering”
   - attachIds: `[attachId]`
3) Server: link’ni `case_message_attach` ga yozadi.

### Misol C: Advokat biriktirilgan case’da advokat chat
1) (Keyingi bosqichlarda) admin `lawyerId` biriktiradi.
2) Mijoz `POST /lawyer/messages` yuboradi: “Assalomu alaykum, vaziyat shunaqa…”
3) Advokat `ROLE_LAWYER` bilan kiradi, `POST /lawyer/messages` yuboradi:
   - message `senderType=LAWYER`, `senderId=advokatId` bo‘ladi.

### Misol D: Case yopiladi
1) Client `PUT /api/v1/cases/{id}/close`
2) Server:
   - `Case.status=CLOSED`, `closedDate=now`
   - `case_channels.status=LOCKED`
3) Endi chat endpointlariga `POST` qilinsa “Case yopilgan!” xatosi qaytadi.

