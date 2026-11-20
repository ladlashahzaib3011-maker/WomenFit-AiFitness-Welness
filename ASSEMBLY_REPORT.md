# 🌸 WomanFit – Mandatory Authentication System Assembly Report

**Application:** WomanFit – AI Fitness & Wellness (`com.aistudio.womanfit.kqvxzl`)  
**Architecture:** Kotlin, Jetpack Compose (Material 3), MVVM, Coroutines & StateFlow, Jetpack DataStore, Navigation Compose, Retrofit + Moshi, Room Cache  
**Backend Integration:** Supabase Authentication (`/auth/v1`) & Supabase PostgreSQL (`/rest/v1/profiles`)

---

## 1. Overview & Security Guarantees

The mandatory Sign In / Sign Up authentication gate has been implemented across the existing **WomanFit** Android application without altering or damaging any existing workout, hydration, menstrual cycle tracking, nutrition, progress, or Coach Maya AI features.

### Enforced Security Rules
1. **Strict Entry Gate:** On every launch, the app evaluates the saved session on a branded `SplashScreen` before rendering any navigation destination. Unauthenticated users are routed directly to `auth_graph` (`SignInScreen`) and cannot access Home, Workouts, Cycle, Diet, AI Coach, Progress, or Profile.
2. **Zero Demo or Fake Login:** All demo login buttons (`Continue as Sarah`), pre-filled demo credentials, offline fake login bypasses, and local password seeding have been completely removed.
3. **Real Supabase Auth Integration:** Sign In, Sign Up, Token Verification, Token Refresh, Password Recovery, and Sign Out connect directly to the Supabase GoTrue REST API (`/auth/v1`) using Retrofit and Moshi.
4. **No Plain-Text Passwords or Sensitive Logs:** Passwords are never stored locally or logged to Logcat. Authentication sessions persist JWT `access_token`, `refresh_token`, `userId`, and `expiresAtMillis` securely in **Jetpack DataStore** (`SessionDataStore`).
5. **Expired & Invalid Session Protection:** On app restart, expired tokens trigger a Supabase refresh request (`/auth/v1/token?grant_type=refresh_token`); invalid or revoked tokens (`401`/`403`) immediately wipe DataStore and lock the user back onto `SignInScreen`.
6. **Per-User Data Isolation:** Local Room fitness records (`daily_progress`, `daily_goals`, `weight_logs`, `activity_history`, `logged_meals`, `users`) remain strictly filtered by the authenticated Supabase `userId`.

---

## 2. Modified & Created Files

| File | Status | Purpose |
|---|---|---|
| `.env.example` | Modified | Added `SUPABASE_URL` and `SUPABASE_ANON_KEY` configuration placeholders for `BuildConfig` via Secrets Gradle Plugin |
| `supabase_schema.sql` | Created | PostgreSQL schema (`public.profiles` including cycle tracking & profile stats) and Row-Level Security (RLS) policies ready to execute in Supabase SQL Editor |
| `app/src/main/AndroidManifest.xml` | Modified | Added `android:windowSoftInputMode="adjustResize"` for reliable keyboard scrolling on auth screens |
| `app/src/main/java/com/example/model/Models.kt` | Modified | Cleaned default `UserProfile` fields (`id`, `name`, `email`) so no demo persona values appear for new users |
| `app/src/main/java/com/example/data/remote/SupabaseModels.kt` | Created | Moshi `@JsonClass` DTOs with `@param:Json` targets (zero compiler warnings) for Supabase Auth and PostgREST profile sync |
| `app/src/main/java/com/example/data/remote/SupabaseAuthApi.kt` | Created | Retrofit service interface for `/auth/v1` and `/rest/v1/profiles`, plus `SupabaseConfig` and `SupabaseClientProvider` |
| `app/src/main/java/com/example/data/security/SessionDataStore.kt` | Modified | Jetpack DataStore storage for `userId`, `email`, `fullName`, `accessToken`, `refreshToken`, and `expiresAtMillis` |
| `app/src/main/java/com/example/data/security/AuthSessionManager.kt` | Deleted | Removed duplicate `SharedPreferences` session manager so `SessionDataStore` is the single source of truth |
| `app/src/main/java/com/example/data/AuthRepository.kt` | Modified | Clean `AuthRepository` contract (`SessionState`, `AuthResult`, `ResetRequestResult`, `AuthField`, `syncProfileUpdate`) |
| `app/src/main/java/com/example/data/SupabaseAuthRepository.kt` | Created | Real Supabase Auth + two-way PostgREST profile/cycle sync repository with token refresh, email validation, and error mapping |
| `app/src/main/java/com/example/data/AppContainer.kt` | Created | Singleton container providing shared `SessionDataStore`, `SupabaseAuthRepository`, and `FitnessRepository` with profile sync wiring |
| `app/src/main/java/com/example/data/FitnessRepository.kt` | Modified | Removed demo credential seeding and duplicate auth methods; scoped all user fitness flows strictly to `authRepository.activeUserId` and wired profile/weight sync |
| `app/src/main/java/com/example/ui/AuthViewModel.kt` | Created | MVVM ViewModel managing `sessionState`, `AuthUiState` (loading/field errors/info banner), `signIn`, `signUp`, `requestPasswordReset`, `signOut` |
| `app/src/main/java/com/example/ui/FitnessViewModel.kt` | Modified | Removed duplicate auth logic; added `resetUiStateOnSignOut()` |
| `app/src/main/java/com/example/ui/screens/DashboardScreen.kt` | Modified | Added safe non-blank top-bar user name fallback |
| `app/src/main/java/com/example/ui/screens/SignInScreen.kt` | Modified | Feminine pink/purple/white M3 UI, editable high-contrast Email & Password fields, Show/Hide password icon, top `[Sign In | Sign Up]` switcher, Forgot Password link, loading state, no demo account |
| `app/src/main/java/com/example/ui/screens/SignUpScreen.kt` | Modified | Full Name, Email, Password (min 6 chars), Confirm Password with eye toggles, validation errors, verification/success banner, and Sign In navigation |
| `app/src/main/java/com/example/ui/screens/ForgotPasswordScreen.kt` | Modified | Connected to Supabase `/auth/v1/recover` email recovery endpoint |
| `app/src/main/java/com/example/ui/screens/ProfileScreen.kt` | Modified | Safe name fallback, numeric/decimal keyboard options on stats edit dialog, and single "Sign Out" button at bottom with `"Sign out of WomanFit?"` confirmation dialog |
| `app/src/main/java/com/example/MainActivity.kt` | Modified | `NavHost` with `SPLASH`, `AUTH_GRAPH`, and guarded `MAIN_GRAPH` enforcing `popUpTo(inclusive = true)` and `LaunchedEffect(sessionState)` guard |
| `app/src/test/java/com/example/AuthenticationTest.kt` | Modified | Expanded unit + Robolectric Compose UI test suite covering authentication, session, token refresh, profile/cycle sync, recovery, and UI inputs |

---

## 3. Authentication Architecture & Flow

### Navigation & Auth Guard (`MainActivity.kt`)
```text
App Launch -> NavRoutes.SPLASH (SplashScreen)
                  |
                  |-- SessionState.LoggedIn  --> NavRoutes.MAIN_GRAPH (popUpTo SPLASH inclusive)
                  '-- SessionState.LoggedOut --> NavRoutes.AUTH_GRAPH (popUpTo SPLASH inclusive)
                                                      |-- SIGN_IN (BackHandler blocks bypass)
                                                      |-- SIGN_UP
                                                      '-- FORGOT_PASSWORD
```
Inside `NavRoutes.MAIN_GRAPH`, an active `LaunchedEffect(sessionState)` guard observes `authViewModel.sessionState`. If `sessionState` ever transitions to `SessionState.LoggedOut` (on Sign Out or expired/revoked session), `FitnessViewModel` state is reset and navigation clears the entire `MAIN_GRAPH` back stack via `popUpTo(NavRoutes.MAIN_GRAPH) { inclusive = true }`.

### Input Field Fix Details
- Explicit dark text (`Color(0xFF0F172A)`), rose cursor, and slate placeholder/label colors are applied to every `OutlinedTextField` so text remains 100% visible regardless of system Dark/Light mode.
- `enabled = true` and `readOnly = uiState.isLoading` preserve touch focus, IME cursor movement, deletion, and replacement.
- `android:windowSoftInputMode="adjustResize"` combined with `.imePadding()` and `.verticalScroll(...)` guarantees the keyboard opens cleanly without obscuring inputs or action buttons.

---

## 4. Connecting Your Supabase Project (When Ready)

1. Create a project in [Supabase](https://supabase.com) and run `supabase_schema.sql` in the **SQL Editor** to create `public.profiles` with Row-Level Security.
2. Copy your **Project URL** (`https://<your-ref>.supabase.co`) and **Anon Public Key** from **Project Settings -> API**.
3. Open the **Secrets panel in AI Studio** and set:
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
4. Rebuild the app — `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY` are injected automatically by the Secrets Gradle Plugin.

---

## 5. Build & Automated Test Verification

Executed command:
```bash
gradle :app:testDebugUnitTest
```

| Test Suite | Tests | Passed | Failures | Notes |
|---|---|---|---|---|
| `com.example.AuthenticationTest` | 15 | 15 | 0 | Covers cold-launch gate, Supabase sign-in, invalid password rejection, sign-up & duplicate-email check, invalid email, <6 char & mismatched passwords, password reset, sign-out + DataStore clearance, restart verification, expired/invalid token rejection, refresh token renewal, unconfigured fail-closed check, two-way profile/cycle sync, and Robolectric Compose UI typing/visibility/no-demo verification |
| `com.example.data.local.DatabaseMigrationTest` | 3 | 3 | 0 | Verifies non-destructive Room migrations `1->2`, `2->3`, and `3->4` |
| `com.example.ExampleRobolectricTest` | 4 | 4 | 0 | Verifies app name, hydration calculator, and 28/35-day cycle phase calculations |
| `com.example.ExampleUnitTest` | 1 | 1 | 0 | Verifies base unit environment |
| **Total** | **23** | **23** | **0** | **BUILD SUCCESSFUL** |

Android compilation (`compile_applet` and `:app:compileDebugKotlin --rerun-tasks`) succeeded with **0 errors and 0 compiler warnings**.
