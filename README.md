# Zeeno SmartDi — FBR Digital Invoicing

A native Android verification + invoicing app built with Kotlin, Jetpack Compose, Material 3, Retrofit, and encrypted session storage. UI follows an iOS-soft invoice style: warm porcelain paper, white 28dp cards with soft shadows, near-black type, vivid `#0700FF` accent — plus UI sounds and springy staggered animations.

## Included flow

1. Animated splash with official FBR logo
2. **NTN lookup** (any 7–13 digit NTN in demo mode)
3. **PIN verification** (4+ digit PIN — no OTP, no email)
4. Invoice home: debounced live search, full month calendar date-range filter, filter tabs (All / Validate / Post), per-invoice Preview and Share-as-PDF — smooth for 100k+ rows (keyed lazy list, off-keystroke filtering, static background)
5. Invoice detail: bill-to card, HS-code line items, subtotal / sales tax / further tax / grand total, Validate (confirmation popup → moves to Post), Post (confirmation popup → removed from list), Share as PDF, Preview PDF in viewer
6. **Settings**: System/Light/Dark appearance, interface sounds toggle (OFF by default), switch account
6. **Post safety**: confirmation alert before every Validate/Post action — prevents accidental clicks
7. Super-secure lock: every app reopen lands on a lock screen (NTN + PIN); lock anytime from the home top bar
8. PDF generation: `Sales Tax Invoice` title, aspect-ratio-preserved FBR logo, HS-code table, Pakistani amount-in-words, FBR verification block with scannable QR

## Run

Open the project in Android Studio (JDK 17), sync Gradle, and run on Android 7.0 (API 24) or newer.

The project starts with `DEMO_MODE=true`. In demo mode:

- Enter any 7–13 digit NTN → proceed to PIN screen
- Enter any 4+ digit PIN (e.g. `1234`) → logged in
- Explore 8 demo invoices (4 to validate, 4 posted, spread across months so the date filter shows its effect), validate, post, filter by date, share as PDF

## Connect the real API

1. Change `API_BASE_URL` and `DEMO_MODE` in `app/build.gradle.kts`.
2. Update endpoint paths and DTOs in `data/FbrApi.kt` — `POST v1/ntn/check`, `POST v1/auth/pin/verify`, `GET v1/pending`, `POST v1/invoices/validate`, `POST v1/invoices/post`. `PendingItemDto` already carries every field the PDF prints (seller/buyer, HS-code lines, dates, FBR verification), so once the backend returns them the PDF is generated 100% from live API data.
3. Update the callback scheme/host build fields and manifest intent filter to match the backend contract.

Tokens and workspace credentials are stored with AndroidX Security `EncryptedSharedPreferences` (passwords are never stored). On devices where the Keystore/encrypted prefs fail, `SessionStore` automatically falls back to private plain prefs so login state still saves.

Invoices can be shared as generated PDF files (`data/PdfInvoice.kt`, Android `PdfDocument` — no extra dependency). Logo aspect ratio is preserved (`object-fit: contain` equivalent). The seller logo prints from the live `sellerLogoUrl` API field (cached on device) and falls back to the bundled asset only when the backend provides no URL.

## Date filtering

Home screen has Start Date and End Date pickers. Filtering applies to the Post tab — only invoices within the selected date range appear.
