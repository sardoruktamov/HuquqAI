# ChatGPT uchun UI dizayn prompti — QalqonAI "AI Suhbatlar" bo'limi

> **Qanday ishlatish:** Ushbu faylning **"ASOSIY PROMPT"** bo'limidan boshlab oxirigacha nusxa oling va ChatGPT ga yuboring. ChatGPT dan **Android va iOS uchun alohida-alohida** yuqori sifatli mobil UI mockup chizishni so'rang (Figma-style yoki realistik ekran rasmlari).

---

## ASOSIY PROMPT (ChatGPT ga nusxa qiling)

Sen professional **mobil UI/UX dizayner** va **product designer**san. Mening loyiham **QalqonAI** 🛡️ — O'zbekiston qonunlari asosida huquqiy maslahat beruvchi mobil ilova (Android + iOS).

Mening ilovamda **pastki navigatsiya (Bottom Tab Bar)** da 5 ta tugma bor:
1. **Bosh sahifa**
2. **AI Suhbatlar** ← *shu bo'limni chizishing kerak*
3. **Advokatlar**
4. **Hujjatlar**
5. **Profil**

Foydalanuvchi **"AI Suhbatlar"** tugmasiga bosganda ochiladigan **barcha ekranlar va holatlarni** chiz. Har bir element, matn, ikonka, rang, o'lcham va joylashuvni **aniq va batafsil** ko'rsat. Umumiy, noaniq dizayn qilma.

---

### 1. LOYIHA KONTEKSTI (Product Context)

**Ilova nomi:** QalqonAI  
**Tagline:** *"O'zbekiston qonunlari asosida huquqiy maslahat"*  
**Maqsad:** Oddiy fuqarolar huquqiy savollariga AI yordamida tez va tushunarli javob olishadi. Murakkab yoki xavfli holatlarda **haqiqiy advokatga yo'naltiriladi**.

**AI xulq-atvori (backend qoidalari — UI matnlarida aks ettirilsin):**
- AI faqat **huquqiy masalalar** bo'yicha javob beradi
- Noaniq savollar bo'lsa: *"Muammoingizni batafsilroq va aniq savol shaklida yozing"* deb so'raydi
- Haqorat/so'kinish bo'lsa: *"Iltimos, savolingizni adabiy tilda va hurmat bilan qayta bering"* deb rad etadi
- AI javoblari **o'zbek tilida**, strukturali (sarlavhalar, ro'yxatlar, **qalin matn** — markdown ko'rinishida)
- AI javob berish **3–10 soniya** davom etishi mumkin (streaming yo'q — foydalanuvchi kutadi)

**Muhim maxfiylik qoidasi (UI da ko'rsatilishi SHART):**
- AI suhbat tarixi advokatga **HECH QACHON uzatilmaydi**
- Advokat bilan suhbat **alohida, noldan** boshlanadi
- Eskalatsiya bannerida bu haqida qisqa ogohlantirish bo'lsin

---

### 2. DIZAYN TIZIMI (Design System)

Mening bosh sahifa dizaynim allaqachon mavjud (tashqarida). **AI Suhbatlar** bo'limi unga vizual jihatdan mos kelishi kerak.

Quyidagi dizayn tizimini taklif qil va barcha ekranlarda qo'lla:

| Element | Qiymat |
|---------|--------|
| **Asosiy rang (Primary)** | `#1B3A5C` — to'q ko'k (ishonch, huquq) |
| **Ikkinchi rang (Secondary)** | `#2E7D6F` — yashil-ko'k (xavfsizlik, qalqon) |
| **Accent / CTA** | `#C9A227` — oltin (premium, diqqat) |
| **Fon (Light)** | `#F5F7FA` |
| **Karta fon** | `#FFFFFF` |
| **Matn asosiy** | `#1A1A2E` |
| **Matn ikkilamchi** | `#6B7280` |
| **Xato** | `#DC2626` |
| **Muvaffaqiyat / ACTIVE** | `#16A34A` |
| **Yopilgan / CLOSED** | `#9CA3AF` |
| **Eskalatsiya banner fon** | `#FEF3C7` (och sariq ogohlantirish) |
| **Eskalatsiya chegara** | `#F59E0B` |
| **User bubble fon** | `#1B3A5C` |
| **User bubble matn** | `#FFFFFF` |
| **AI bubble fon** | `#FFFFFF` + `#E5E7EB` chegara |
| **AI bubble matn** | `#1A1A2E` |
| **Border radius (karta)** | 12px |
| **Border radius (bubble)** | 16px (pastki burchak 4px — chat uslubi) |
| **Font** | Inter yoki SF Pro (platformaga mos) |
| **Sarlavha (Screen title)** | 20px, SemiBold |
| **Ro'yxat sarlavhasi** | 16px, Medium |
| **Asosiy matn** | 15px, Regular |
| **Kichik matn (vaqt, badge)** | 12px, Regular |
| **Bottom Tab Bar balandligi** | 56px + safe area |
| **FAB o'lchami** | 56×56px |

**Ikonkalar:** Outlined style, 24px. AI uchun: robot/shield/scale of justice aralash uslub. Advokatga bog'lanish uchun: advokat portreti + chat ikonkasi.

**Til:** Asosiy interfeys **O'zbek (Lotin)**. Keyingi versiyada RU/EN qo'llab-quvvatlanadi — matnlar qisqa va tarjima qilinadigan bo'lsin.

---

### 3. PASTKI NAVIGATSIYA (Bottom Tab Bar — doim ko'rinadi)

Har bir ekranda (suhbat ichidan tashqari) pastki 5 ta tugma ko'rsatilsin:

| # | Label | Ikonka | Holat |
|---|-------|--------|-------|
| 1 | Bosh sahifa | Uy | Inactive |
| 2 | **AI Suhbatlar** | AI/Robot chat | **ACTIVE (highlighted)** |
| 3 | Advokatlar | Advokat/portfel | Inactive |
| 4 | Hujjatlar | Hujjat/papka | Inactive |
| 5 | Profil | User avatar | Inactive |

**Active tab:** Primary rang `#1B3A5C`, label qalin.  
**Inactive tab:** `#6B7280`.  
**Badge (ixtiyoriy):** AI Suhbatlar tabida yangi xabar bo'lsa qizil nuqta.

---

### 4. CHIZILADIGAN EKRANLAR (Screen Inventory)

Quyidagi **7 ta ekran/holat** ni alohida-alohida, yuqori detallarda chiz:

---

#### EKRAN 1: AI Suhbatlar — Ro'yxat (Asosiy, tab ochilganda)

**Navigatsiya:** Foydalanuvchi pastki tabdan "AI Suhbatlar" ni bosadi → shu ekran ochiladi.

**Layout (yuqoridan pastga):**

1. **Status Bar** (iOS/Android standart)

2. **App Bar / Header**
   - Chap: QalqonAI logotipi (🛡️ + "QalqonAI" matni, 18px)
   - O'ng: Qidiruv ikonkasi (🔍) — kelajak funksiya, hozir disabled ko'rinishda
   - Fon: oq, pastki soya (elevation 2)

3. **Sarlavha bloki**
   - "AI Suhbatlar" — 24px, Bold
   - Subtitle: "Huquqiy savollaringizga AI yordamchi javob beradi" — 14px, kulrang

4. **Ogohlantirish strip (disclaimer) — birinchi marta yoki doimiy kichik banner**
   - Fon: `#EFF6FF`, chapda ℹ️ ikonka
   - Matn: *"AI maslahati umumiy xarakterda. Murakkab ishlar uchun advokat bilan maslahatlashing."*
   - O'ngda ✕ yopish tugmasi (kichik)

5. **Suhbatlar ro'yxati (ScrollView / FlatList)**
   
   Har bir **suhbat kartochkasi** (Swipe qilinmaydi, oddiy tap):
   ```
   ┌─────────────────────────────────────────────┐
   │ [🤖]  Meros va yer masalasi qanday?         │
   │       12 iyun, 2026 · 07:43                 │
   │                              [Faol] badge   │
   └─────────────────────────────────────────────┘
   ```
   
   **Karta elementlari:**
   - Chap: AI avatar (doira, 44px, robot/shield ikonka, `#2E7D6F` fon)
   - Markaz ustun:
     - **title** (suhbat nomi) — 16px Medium, 1 qator, ortiqcha matn `...`
     - **createdDate** — 13px, `#6B7280`, format: "12 iyun, 2026 · 07:43"
   - O'ng:
     - **Status badge:**
       - `ACTIVE` → yashil nuqta + "Faol" (12px)
       - `CLOSED` → kulrang + "Yopilgan" (12px)
   - Karta: oq fon, 12px radius, 16px padding, 8px vertikal margin
   - Kartalar orasida Divider yo'q, faqat margin

   **Eslatma (API cheklovi):** Backend ro'yxatda faqat `title`, `status`, `createdDate` qaytaradi — **oxirgi xabar matni yo'q**. Shuning uchun preview sifatida faqat title ishlatilsin (oxirgi xabar ko'rsatilmasin).

6. **Bo'sh holat (Empty State)** — alohida variant sifatida ham chiz:
   - Markazda: katta AI ikonka (120px, och kulrang)
   - "Hali AI suhbatlaringiz yo'q"
   - "Huquqiy savolingiz bormi? Yangi suhbat boshlang."
   - CTA tugma: **"+ Yangi suhbat"** (Primary rang, to'liq kenglik emas, markazda)

7. **FAB (Floating Action Button)**
   - O'ng past burchak (Bottom Tab Bar ustida, 16px margin)
   - 56×56px, dumaloq, `#1B3A5C` fon
   - Oq "+" ikonka
   - Soya: elevation 6
   - Vazifasi: Yangi suhbat yaratish

8. **Pastki Tab Bar** (5 tugma, AI Suhbatlar active)

9. **Pull-to-refresh** indikatorini dizaynda eslat (yuqorida tortganda)

10. **Pagination:** Ro'yxat oxirida "Yana yuklash..." loader (backend page=1, size=10)

---

#### EKRAN 2: Yangi suhbat yaratish (Modal / Bottom Sheet)

**Trigger:** FAB "+" bosilganda yoki Empty State dagi CTA.

**iOS:** Bottom Sheet (pastdan chiqadi, 60% ekran balandligi)  
**Android:** Material Bottom Sheet yoki to'liq ekran modal

**Elementlar:**

1. **Drag handle** (yuqorida kichik chiziq, 40×4px)

2. **Sarlavha:** "Yangi AI suhbat" — 20px Bold

3. **Input maydoni:**
   - Label: "Suhbat mavzusi *"
   - Placeholder: "Masalan: Meros va yer masalasi"
   - Validation: bo'sh bo'lsa qizil chegara + "Mavzu kiritish majburiy"
   - Maksimum: 100 belgi, belgi hisoblagich o'ng pastda

4. **Maslahat matni (hint):**
   - 💡 "Mavzuni aniq yozing — keyinroq suhbatlaringizni topish oson bo'ladi"

5. **Tugmalar (pastda):**
   - **"Boshlash"** — Primary, to'liq kenglik, 48px balandlik
   - **"Bekor qilish"** — Text button, kulrang

6. Klaviatura ochiq holatini ham chiz (input focus)

**Flow:** "Boshlash" → suhbat yaratiladi → EKRAN 3 ga o'tiladi

---

#### EKRAN 3: AI Suhbat — Suhbat oynasi (Conversation — Normal holat)

**Eng muhim ekran.** WhatsApp/Telegram uslubida, lekin huquqiy ilovaga mos professional ko'rinish.

**Layout:**

1. **Top App Bar**
   - Chap: ← Orqaga tugmasi
   - Markaz:
     - Suhbat **title** (16px Bold, 1 qator)
     - Subtitle: "AI Huquqiy Yordamchi" + yashil nuqta (online)
   - O'ng: ⋮ (3 nuqta menu):
     - "Suhbatni yopish"
     - "Suhbatni o'chirish" (ixtiyoriy, kelajak)

2. **Disclaimer banner (suhbat boshida, bir marta)**
   - Kichik karta, `#F0FDF4` fon
   - 🤖 "Men QalqonAI yordamchisiman. O'zbekiston qonunlari asosida umumiy maslahat beraman. Aniq huquqiy xulosa uchun advokatga murojaat qiling."

3. **Xabarlar maydoni (ScrollView, pastdan yuqoriga)**

   **Foydalanuvchi xabari (USER) — o'ng tomonda:**
   ```
                    ┌──────────────────────────┐
                    │ Assalomu alaykum. Otamdan│
                    │ 16 sotix yer qolgan...   │
                    └──────────────────────────┘
                                         07:40 ✓✓
   ```
   - Bubble fon: `#1B3A5C`, matn oq
   - Vaqt: 11px, `#6B7280`, bubble ostida o'ngda
   - Maksimum kenglik: ekranning 75%

   **AI javobi (AI) — chap tomonda:**
   ```
   [🤖]  ┌────────────────────────────────┐
         │ Va alaykum assalom. Otangizdan  │
         │ qolgan meros mulkini...        │
         │                                │
         │ ### 1. Meros huquqi            │
         │ **Birinchi qadam:** Notariusga │
         │ murojaat qiling...             │
         └────────────────────────────────┘
         07:43
   ```
   - AI avatar: 32px doira, chapda
   - Bubble: oq fon, `#E5E7EB` border
   - **Markdown qo'llab-quvvatlash:** `**qalin**`, `### sarlavha`, `- ro'yxat` — UI da formatlangan ko'rinsin
   - Uzun javoblarda "Davomini o'qish" yoki to'liq scroll

4. **Fayl biriktirish (attachments) — xabar ichida**
   - Agar fayl biriktirilgan bo'lsa: kichik karta (rasm/pdf ikonka + fayl nomi + hajmi)
   - API: `attachments[]` — `originName`, `extension`, `url`

5. **Xabar yuborish paneli (Composer) — pastda, Tab Bar YO'Q**
   ```
   ┌─────────────────────────────────────────────┐
   │ [📎]  [  Xabar yozing...              ] [➤] │
   └─────────────────────────────────────────────┘
   ```
   - 📎 — fayl biriktirish (rasm, PDF)
   - Text input: ko'p qatorli, max 2000 belgi
   - ➤ Send tugmasi: Primary rang, input bo'sh bo'lsa disabled (kulrang)
   - iOS: klaviatura ustida safe area
   - Android: navigation bar ustida

6. **Yuborish jarayoni (Loading holati) — alohida variant:**
   - User xabari optimistik ko'rsatiladi (o'ngda)
   - AI tomonda "typing" indikator:
     ```
     [🤖]  ● ● ●  AI javob tayyorlayapti...
     ```
   - Send tugmasi → spinner
   - **3–10 soniya** davom etishi mumkin — foydalanuvchi kutayotganini aniq ko'rsat

7. **Xato holati (AI javob bermadi):**
   - Qizil snackbar/toast: "AI javob olishda xatolik yuz berdi!"
   - "Qayta urinish" tugmasi

---

#### EKRAN 4: AI Suhbat — Eskalatsiya holati (Advokatga bog'lanish)

**Trigger:** AI javobida backend `isEscalation: true` qaytarsa.

**Eskalatsiya trigger so'zlar (backend):** advokat, sud, qurol, qotillik, narkotik, huquqshunos, xavf va hokazo — foydalanuvchi sud/qurol/xavf haqida yozganda.

**Ekran 3 ga QO'SHIMCHA quyidagi bloklar paydo bo'ladi:**

1. **Eskalatsiya Banner (AI javobidan keyin, composer ustida — STICKY)**
   ```
   ┌─────────────────────────────────────────────┐
   │ ⚠️  Bu masala murakkab yoki xavfli bo'lishi │
   │     mumkin. Malakali advokat yordami        │
   │     tavsiya etiladi.                        │
   │                                             │
   │  [👨‍⚖️ Advokatga bog'lanish]  [Davom etish] │
   │                                             │
   │  ℹ️ AI suhbat tarixi advokatga uzatilmaydi  │
   └─────────────────────────────────────────────┘
   ```
   - Fon: `#FEF3C7`, chegara: `#F59E0B` 1px
   - Padding: 16px
   - **"Advokatga bog'lanish"** — Primary CTA, `#C9A227` fon, oq matn, 44px balandlik
   - **"Davom etish"** — Text/Outline button, kulrang
   - **Maxfiylik eslatmasi:** 11px, `#92400E`

2. **AI xabarida kichik badge:**
   - AI bubble yonida: "⚠️ Muhim" yoki "Advokat tavsiya etildi" chip

3. **"Advokatga bog'lanish" bosilganda → EKRAN 5 ga o'tiladi**

---

#### EKRAN 5: Advokatga bog'lanish (Eskalatsiya flow)

**Trigger:** Eskalatsiya banneridagi CTA.

**Variant A — Advokat tanlash (asosiy flow):**

1. **Header:** "Advokat tanlang" + ← orqaga

2. **Ogohlantirish karta (yuqorida, qizil/sariq):**
   - 🔒 "AI suhbat tarixi advokatga yuborilmaydi. Yangi suhbat boshlanadi."

3. **Birinchi xabar input (majburiy):**
   - Label: "Advokatga birinchi xabaringiz"
   - Placeholder: "Vaziyatingizni qisqacha tushuntiring..."
   - Ko'p qatorli textarea, min 20 belgi

4. **Advokatlar ro'yxati (scroll):**
   Har bir advokat kartochkasi:
   ```
   ┌─────────────────────────────────────────────┐
   │ [Foto]  Sardor Uktamov                       │
   │         Fuqarolik huquqi · 12 yil tajriba    │
   │         ⭐ 4.8 · 24 maqola                   │
   │                          [Tanlash →]        │
   └─────────────────────────────────────────────┘
   ```
   - Avatar: 48px
   - Ism, ixtisoslik, tajriba, reyting, postCount
   - "Tanlash" tugmasi

5. **"Yuborish" tugmasi** (advokat tanlangan + xabar yozilgan bo'lsa active)

**Variant B — Advokatlar tabiga yo'naltirish:**
   - Modal: "Advokatlar bo'limiga o'tasizmi?" → Advokatlar tab ochiladi

**Muvaffaqiyat holati:**
   - ✅ "Advokatga xabaringiz yuborildi!"
   - "Suhbatni ko'rish" → Lawyer chat ekraniga (kelajak)

---

#### EKRAN 6: AI Suhbat — Yopilgan holat (CLOSED)

**Trigger:** Suhbat `status: CLOSED` bo'lsa (foydalanuvchi yoki tizim yopgan).

**Ekran 3 ga o'xshash, LEKIN:**

1. **Top bar subtitle:** "Yopilgan" — kulrang badge

2. **Composer o'rniga — kulrang strip:**
   ```
   ┌─────────────────────────────────────────────┐
   │  🔒 Bu suhbat yopilgan. Yangi savol uchun   │
   │     yangi suhbat boshlang.                  │
   └─────────────────────────────────────────────┘
   ```
   - Fon: `#F3F4F6`, matn `#6B7280`
   - Input va Send **disabled/hidden**

3. **Xabarlar faqat o'qish rejimida** (read-only)

4. **Menu da:** "Suhbatni yopish" o'rniga "Suhbat yopilgan ✓"

---

#### EKRAN 7: Xatolik va yuklanish holatlari (Overlay/States)

Alohida kichik ekranlar yoki EKRAN 1/3 ustiga overlay:

| Holat | Ko'rinish |
|-------|-----------|
| **Ro'yxat yuklanmoqda** | Markazda spinner + "Suhbatlar yuklanmoqda..." |
| **Internet yo'q** | Wi-Fi xato ikonka + "Internet aloqasi yo'q" + "Qayta urinish" |
| **Server xatosi** | "Xatolik yuz berdi. Keyinroq urinib ko'ring." |
| **Auth talab** | "Kirish kerak" + Login tugmasi (AI chat faqat tizimga kirgan user uchun) |
| **AI kutish** | Typing indicator (EKRAN 3 da) |
| **Fayl yuklanmoqda** | Progress bar attachment ustida |

---

### 5. FOYDALANUVCHI OQIMI (User Flow Diagram)

Quyidagi oqimni dizaynda aks ettir:

```
Bottom Tab "AI Suhbatlar"
        │
        ▼
  [EKRAN 1: Ro'yxat]
        │
        ├── FAB "+" ──► [EKRAN 2: Yangi suhbat] ──► [EKRAN 3: Suhbat]
        │
        └── Karta tap ──► [EKRAN 3: Suhbat]
                              │
                              ├── AI javob (normal) ──► davom etadi
                              │
                              ├── isEscalation=true ──► [EKRAN 4: Banner]
                              │                              │
                              │                              └── "Advokatga bog'lanish"
                              │                                      │
                              │                                      ▼
                              │                              [EKRAN 5: Advokat tanlash]
                              │
                              └── Suhbat yopildi ──► [EKRAN 6: CLOSED]
```

---

### 6. BACKEND API (Dizayn uchun ma'lumot manbalari)

Dizayn **real API** ga mos bo'lsin:

**Suhbat ro'yxati:** `GET /api/v1/ai-chats?page=1&size=10`
```json
{
  "content": [{
    "id": "uuid",
    "title": "Meros va yer masalasi qanday?",
    "status": "ACTIVE",
    "createdDate": "2026-06-12T11:06:10"
  }]
}
```

**Yangi suhbat:** `POST /api/v1/ai-chats` — body: `{ "title": "..." }`

**Xabar yuborish:** `POST /api/v1/ai-chats/{id}/messages` — body: `{ "content": "...", "attachIds": [] }`
- Javob: faqat **AI xabari** qaytadi (`senderType: "AI"`, `isEscalation: true/false`)
- User xabarini client optimistik qo'shadi

**Xabarlar tarixi:** `GET /api/v1/ai-chats/{id}/messages?page=1&size=20`
- Xabarlar **eng yangisi birinchi** keladi — UI da **teskari tartibda** ko'rsat (pastda eng yangi)

**Suhbat yopish:** `PUT /api/v1/ai-chats/{id}/close`

---

### 7. REAL KONTENT NAMUNALARI (Mockup matnlari)

Dizaynda haqiqiy o'zbek matnlardan foydalan:

**Suhbat 1 title:** "Meros va yer masalasi qanday?"  
**User xabar:** "Assalomu alaykum. Otamdan 16 sotix yer qolgan, uni ukam bilan qanday teng bo'lishimiz mumkin?"  
**AI javob (qisqartirilgan):** "Va alaykum assalom. O'zbekiston Fuqarolik kodeksining 1135-moddasiga binoan, bolalar teng ulushda merosxo'r hisoblanadi. **Birinchi qadam:** notarial idoraga murojaat qiling..."

**Suhbat 2 (eskalatsiya):**  
**User xabar:** "Qo'shnim qo'lida qurol bilan tahdid qildi. Sudga bermoqchiman."  
**AI javob + isEscalation: true** → Banner chiqadi

**Suhbat 3 (yopilgan):** status CLOSED, title "Ish beruvchi oylik to'lamayapti"

---

### 8. PLATFORM SPECIFIK TALABLAR

**iOS:**
- Safe Area (notch, home indicator) hurmat qilinsin
- Swipe back gesture (chap chetdan)
- Bottom Sheet for yangi suhbat
- SF Pro font
- 44px minimum touch target

**Android:**
- Material Design 3 uslubi
- Navigation bar (gesture/buttons) hisobga olinsin
- Ripple effect tugmalarda
- Roboto font
- 48dp minimum touch target

**Ikkala platforma uchun:**
- Dark mode versiyasini ham chiz (ixtiyoriy, lekin tavsiya etiladi)
- Accessibility: kontrast nisbati WCAG AA
- Ekran o'lchamlari: iPhone 14 (390×844) va Samsung Galaxy S23 (360×780)

---

### 9. CHIQISH FORMATI (Output Format)

Mening so'rovimga javoban quyidagilarni ber:

1. **7 ta ekran/holat** uchun alohida yuqori sifatli mockup rasmlari (yoki bitta katta artboard da barchasi)
2. Har bir ekran uchun **elementlar ro'yxati** (annotatsiya bilan)
3. **Rang palitrasi** va **font o'lchamlari** jadvali
4. **Foydalanuvchi oqimi** diagrammasi
5. **Android va iOS** farqlari (agar bor bo'lsa)
6. Dizaynni amalga oshiruvchi dasturchi uchun **qisqa UI spec** (padding, margin, hex ranglar)

**Usul:** Realistik mobil UI mockup (Figma-style). Flat wireframe emas — rangli, professional, ishlab chiqishga tayyor ko'rinish.

**Muhim:** Har bir detalni aniq chiz — placeholder "Lorem ipsum" ishlatma, yuqoridagi haqiqiy o'zbek matnlardan foydalan.

---

## PROMPT OXIRI

---

## Qo'shimcha eslatma (dasturchi / product owner uchun)

> Ushbu bo'lim ChatGPT ga yuborilmaydi — bu men (AI architect) loyiha tahlili asosida qo'shgan professional eslatmalar.

### Sen aytgan narsalar hisobga olindi:
- ✅ Pastki 5 ta tugma (AI Suhbatlar fokusda)
- ✅ AI chatlar ro'yxati
- ✅ AI bilan suhbat oynasi
- ✅ Advokatga bog'lanish tugmasi (eskalatsiya)

### Men qo'shib qo'ygan narsalar (hisobga olinmagan bo'lishi mumkin):

1. **Yangi suhbat yaratish ekrani** — API da avval `title` bilan chat yaratish majburiy, keyin xabar yuboriladi
2. **AI javob kutish holati (3–10 son)** — backend streaming qo'llamaydi, dizaynda loading shart
3. **Maxfiylik disclaimer** — AI tarixi advokatga uzatilmaydi (backend arxitekturasi talabi)
4. **Yopilgan suhbat holati** — `CLOSED` status uchun read-only UI
5. **Bo'sh ro'yxat (empty state)** — birinchi foydalanuvchi uchun
6. **Xato holatlari** — internet, server, AI xato
7. **Fayl biriktirish UI** — API `attachIds` qo'llab-quvvatlaydi
8. **Markdown formatlangan AI javoblar** — AI `**qalin**`, `### sarlavha` ishlatadi
9. **Auth gate** — AI chat faqat login qilgan user uchun
10. **Ro'yxatda oxirgi xabar preview yo'q** — API faqat `title` qaytaradi, preview dizayn qilinmagan
11. **Advokat tanlash ekrani** — backend da public lawyer list API yo'q; dizayn Advokatlar tab/postlar bilan integratsiya qilinadi
12. **Pull-to-refresh va pagination** — ro'yxat va xabarlar uchun
13. **Platform specifika** — iOS safe area, Android Material 3
14. **Rang palitrasi** — repoda rasmiy brand book yo'q, huquqiy ilovaga mos palitra taklif qilindi (bosh sahifa dizayningizga moslashtiring)

### Loyihada mobil kod yo'q
Repoda faqat **Spring Boot backend** mavjud. Bosh sahifa dizayningiz tashqarida — ChatGPT ga bosh sahifa ranglarini moslashtirish uchun o'sha dizaynni ham **reference image** sifatida yuborishingiz tavsiya etiladi.
