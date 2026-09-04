# FBR NTN Verification

A native Android verification flow built with Kotlin, Jetpack Compose, Material 3, Haze backdrop blur, Retrofit, and encrypted session storage.

## Included v1 flow

1. Animated splash and secure session check
2. NTN lookup
3. Registered mobile confirmation and OTP request
4. Six-cell OTP verification with auto-submit, shake feedback, and resend timer
5. In-app secure WebView login with callback interception and retry handling
6. Pull-to-refresh pending list with loading, empty, and error states

## Run

Open the project in Android Studio (JDK 17), sync Gradle, and run on Android 7.0 (API 24) or newer.

The project starts with `DEMO_MODE=true`, because the API contracts are placeholders. In demo mode:

- Enter any 7–13 digit NTN.
- Use OTP `123456`.
- Press **Continue securely** on the hosted login simulation.

## Connect the real API

1. Change `API_BASE_URL` and `DEMO_MODE` in `app/build.gradle.kts`.
2. Update endpoint paths and DTOs in `data/FbrApi.kt`—all API placeholders live in that file.
3. Update the callback scheme/host build fields and manifest intent filter to match the backend contract.
4. If the login page does not need JavaScript, disable it in `WebLoginScreen.kt`.

Tokens are stored with AndroidX Security `EncryptedSharedPreferences`; WebView cookies are enabled for cookie-based authentication.
