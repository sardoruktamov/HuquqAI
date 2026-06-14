<div align="center">
  <h1>🛡️ QalqonAI</h1>
  <p><b>O'zbekiston Respublikasi qonunlari asosida huquqiy maslahat beruvchi ilova</b></p>
</div>

---

**QalqonAI** — insonlarga huquqiy masalalarda oson, qulay va tushunarli tilda yordam berishga qaratilgan innovatsion huquqiy yordamchi platformadir. Loyihaning bosh maqsadi — xalqning huquqiy savodxonligini oshirish va malakali yuristlar (advokatlar) xizmatlarini raqamli makonda hamma uchun ochiq qilishdir. 

## 🎯 Loyiha Maqsadi va Mazmuni

Platforma foydalanuvchilari o'zlarining huquqiy savollariga qonunlarga tayangan yechimlarni tezkor yo'l bilan olishi, yuridik ishlarini tartibga solishi va tajribali huquqshunoslar tomonidan tayyorlangan tahliliy materiallar (postlar) bilan muntazam tanishib borish imkoniyatiga ega bo'ladilar. Keyingi bosqichlarda loyihaga maxsus Sun'iy Intellekt (AI) integratsiya qilinib, huquqiy savollarga avtomatlashtirilgan tarzda maslahat beruvchi aqlli yordamchi funksionalini ishga tushirish maqsad qilingan.

## 🚀 Asosiy Imkoniyatlar (Joriy Holatida)

Loyiha hozirda faol ishlab chiqish (development) bosqichida bo'lib, quyidagi fundamental qismlar muvaffaqiyatli amalga oshirilgan:

- **🔐 Xavfsizlik va Avtorizatsiya (JWT):** Foydalanuvchilarni xavfsiz ro'yxatdan o'tkazish, autentifikatsiya va tizim imkoniyatlaridan foydalanishi uchun JSON Web Token (JWT) asosidagi qat'iy xavfsizlik arxitekturasi.
- **👥 Kengaytirilgan rollar tizimi (RBAC):** Tizimda qat'iy rollar taqsimoti amalga oshirilgan: `ROLE_USER` (oddiy foydalanuvchilar), `ROLE_LAWYER` (malakali huquqshunoslar), va tizim boshqaruvchilari (`ROLE_ADMIN`, `ROLE_SUPERADMIN`).
- **👨‍⚖️ Advokatlar uchun Post Tizimi:** Platformada faqat tasdiqlangan advokatlar (`ROLE_LAWYER`) mutaxassis sifatidagi tasdiqlangan huquqiy tahlillar va maqolalarni ommaga e'lon qilishi uchun mustahkamlangan CRUD tizimi.
- **📊 Profil Statistikasi:** Advokatlarning reytingi va ishonchliligini ifodalovchi nashr qilingan maqolalari soni (`postCount`), shu bilan birga foydalanuvchining amaldagi yuridik ishlari sonini (`caseCount`) hisob-kitob qilish imkoni.
- **📖 API Hujjatlashtirish:** Frontend ilovalar va boshqa tizimlar erkin ulanishi uchun `Swagger` (OpenAPI v3) orqali barcha mavjud API endpointlar sifatli tarzda hujjatlashtirilgan. Xavfsiz `Bearer Auth` standarti orqali himoyalangan.

## 🛠 Texnologiyalar Steki (Tech Stack)

Backend qismi so'ngi texnologiyalar va amaliyotlar asosida yozilmoqda:
- **Asos:** `Java 17+`, `Spring Boot 3.x`
- **Xavfsizlik qatlami:** `Spring Security`, `JWT`
- **Ma'lumotlar Bazasi:** `PostgreSQL`, `Spring Data JPA` (Native va JPQL query)
- **Hujjatlashtirish:** `Swagger` (OpenAPI)
- **Build arxitekturasi:** `Maven` (`./mvnw`)

## So'nggi rivojlantirishlar (kod bazasi va konfiguratsiya)

Quyidagi qisqa xulosalar oxirgi commitlar va `api.ailawyer.uz` moduli tahlili asosida README dagi umumiy tavsifga **qo'shimcha** ravishda kiritilgan: ular joriy texnik holatni aniqroq aks ettiradi.

### Brend va loyiha yo'nalishi

- Loyiha avvalgi nom va kontsepsiyadan (`soccialnetwork` / `LegalTech` kabi) **QalqonAI** brendiga yo'naltirilgan; API va foydalanuvchi yoqidagi matnlar shu nom bilan uyg'unlashtirilmoqda.

### Suhbat va AI infratuzilmasi

- **AI chat (izolyatsiya qilingan):** foydalanuvchi uchun `AiChat` / `AiMessage` modellari, holatlar (`ACTIVE`, `CLOSED`) va REST yo'llari (`/api/v1/ai-chats`, xabarlar uchun `/api/v1/ai-chats/{id}/messages`) joriy etilgan.
- **Tashqi LLM ulanishi:** `AiProvider` interfeysi orqali javoblar shakllantiriladi; hozirda **`GeminiAiProviderImpl`** (Google Gemini 2.5 Flash) ishlaydi. Konfiguratsiya: `gemini.api.key`, `gemini.api.model` (`application.properties`).
- **AI eskalatsiya:** AI javobida xavfli/trigger so'zlar bo'lsa `isEscalation=true` qaytariladi — mobil UI "Advokatga bog'lanish" tugmasini ko'rsatadi.
- **Advokat tizimi (BOSQICH 5):** onboarding (`DRAFT → PENDING → APPROVED`), admin tasdiqlash, public katalog (`/api/v1/lawyers/public`), litsenziya detail. Test: `src/main/resources/http/lawyer.http`.
- **Advokat bilan chat (BOSQICH 6):** mijoz va advokat o'rtasidagi aloqa uchun `LawyerChat` / `LawyerMessage` REST API to'liq ishlaydi. Chat ro'yxati ism, rasm va oxirgi xabar preview qaytaradi. Yopilgan chatga yozish bloklangan. Test: `src/main/resources/http/lawyer-chat.http`.

### Xavfsizlik va hujjatlashtirish (yangilangan sozlamalar)

- **Spring Security:** CORS sozlamalari kengaytirilgan; Swagger/OpenAPI uchun **`/v3/api-docs/**`** bilan bir qatorda **`springdoc.api-docs.path=/api-docs`** bo'yicha **`/api-docs/**`** yo'llari ham ochiq ro'yxatga (`AUTH_WHITELIST`) kiritilgan — UI va JSON hujjatlar token talab qilinmasdan yuklanishi uchun.
- **OpenAPI:** `springdoc-openapi-starter-webmvc-ui` **2.6.0** versiyasi ishlatiladi (`Spring Boot 3.3.5` bilan mos).

### Ma'lumotlar bazasi va xabarlar

- **Flyway:** `flyway-core` va PostgreSQL uchun `flyway-database-postgresql` qo'shimchalari mavjud — sxema o'zgarishlari migratsiya orqali boshqariladi.
- **Email (`spring-boot-starter-mail`):** ro'yxatdan o'tishni email orqali tasdiqlash uchun HTML-shablonli xatlar, parolni tiklash oqimi, yuborishlar tarixini hisobga olish va cheklov (masalan, bir xil manzilga takroriy xat) kabi xususiyatlar `EmailSendingService` da jamlangan.
- **Bildirishnomalar:** `NotificationService` kelajakdagi push (masalan, FCM) ulanishi uchun hook sifatida qoldirilgan; hozircha hodisalar log orqali kuzatiladi.

### Fayllar va postlar

- **Fayl biriktirish:** `AttachController` orqali yuklash va ochiq URL orqali fayl ochish (`/api/v1/attach/...`) qo'llab-quvvatlanadi; ayrim yo'llar ochiq ro'yxatda.
- **Postlar:** jamoat va admin filtrlash, o'xshash postlar qidiruvi kabi endpointlar `PostController` da rivojlantirilgan (Swagger orqali batafsil ko'rish mumkin).

### Bosqichlar holati (backend)

| Bosqich | Mavzu | Holat |
|---------|-------|-------|
| 1–2 | Auth, JWT, RBAC | Tugallangan |
| 3 | AI/Lawyer chat izolyatsiya | Tugallangan |
| 4 | Gemini AI integratsiya | Tugallangan |
| 5 | Advokat onboarding + katalog | Tugallangan |
| 6 | Lawyer chat yakunlash | Tugallangan |
| 7 | Push notification (FCM) | Reja |
| 8 | Mobil ilova | Reja |
| 9 | Hujjatlar moduli | Reja |
| 10 | RAG (O'zbekiston qonunlari) | Reja |

---

*Ushbu hujjat "QalqonAI" ilovasi rivojlanishi va yangi AI modullarining ulanishiga qarab doimiy ravishda kengaytirilib boriladi.*
