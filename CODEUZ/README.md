<div align="center">
  <h1>🛡️ QalqonAI</h1>
  <p><b>O'zbekiston Respublikasi qonunlari asosida huquqiy maslahat beruvchi ilova</b></p>
</div>

---

**QalqonAI** — insonlarga huquqiy masalalarda oson, qulay va tushunarli tilda yordam berishga qaratilgan innovatsion huquqiy yordamchi platformadir. Loyihaning bosh maqsadi — xalqning huquqiy savodxonligini oshirish va malakali yuristlar (advokatlar) xizmatlarini raqamli makonda hamma uchun ochiq qilishdir. 

## 🎯 Loyiha Maqsadi va Mazmuni

Platforma foydalanuvchilari o'zlarining huquqiy savollariga qonunlarga tayangan yechimlarni tezkor yul bilan olishi, yuridik ishlarini tartibga solishi va tajribali huquqshunoslar tomonidan tayyorlangan tahliliy materiallar (postlar) bilan muntazam tanishib borish imkoniyatiga ega bo'ladilar. Keyingi bosqichlarda loyihaga maxsus Sun'iy Intellekt (AI) integratsiya qilinib, huquqiy savollarga avtomatlashtirilgan tarzda maslahat beruvchi aqlli yordamchi funksionalini ishga tushirish maqsad qilingan.

## 🚀 Asosiy Imkoniyatlar (Joriy Holatida)

Loyiha hozirda faol ishlab chiqish (development) bosqichida bo'lib, quyidagi fundamental qismlar muvaffaqiyatli amalga oshirilgan:

- **🔐 Xavfsizlik va Avtorizatsiya (JWT):** Foydalanuvchilarni xavfsiz ro'yxatdan o'tkazish, autentifikatsiya va tizim imkoniyatlaridan foydalanishi uchun JSON Web Token (JWT) asosidagi qat'iy xavfsizlik arxitekturasi.
- **👥 Kusaytirilgan Rollar Tizimi (RBAC):** Tizimda qat'iy rollar taqsimoti amalga oshirilgan: `ROLE_USER` (oddiy foydalanuvchilar), `ROLE_LAWYER` (malakali huquqshunoslar), va tizim boshqaruvchilari (`ROLE_ADMIN`, `ROLE_SUPERADMIN`).
- **👨‍⚖️ Advokatlar uchun Post Tizimi:** Platformada faqatгина tasdiqlangan advokatlar (ROLE_LAWYER) mutaxassis sifatidagi tasdiqlangan huquqiy tahlillar va maqolalarni ommaga e'lon qilishi uchun mustahkamlangan CRUD tizimi.
- **📊 Profil Statistikasi:** Advokatlarning reytingi va ishonchliligini ifodalovchi nashr qilingan maqolalari soni (`postCount`), shu bilan birga foydalanuvchining amaldagi yuridik ishlari sonini (`caseCount`) hisob-kitob qilish imkoni.
- **📖 API Hujjatlashtirish:** Frontend ilovalar va boshqa tizimlar erkin ulanishi uchun `Swagger` (OpenAPI v3) orqali barcha mavjud API endpointlar sifatli tarzda hujjatlashtirilgan. Xavfsiz `Bearer Auth` standarti orqali himoyalangan.

## 🛠 Texnologiyalar Steki (Tech Stack)

Backend qismi so'ngi texnologiyalar va amaliyotlar asociada yozilmoqda:
- **Asos:** `Java 17+`, `Spring Boot 3.x`
- **Xavfsizlik qatlami:** `Spring Security`, `JWT`
- **Ma'lumotlar Bazasi:** `PostgreSQL`, `Spring Data JPA` (Native va JPQL query)
- **Hujjatlashtirish:** `Swagger` (OpenAPI)
- **Bulid arxitekturasi:** `Maven` (`./mvnw`)

---
*Ushbu hujjat "QalqonAI" ilovasi rivojlanishi va yangi AI modullarining ulanishiga qarab doimiy ravishda kengaytirilib boriladi.*
