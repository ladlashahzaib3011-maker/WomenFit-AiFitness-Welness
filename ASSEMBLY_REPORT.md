# 🌸 WomanFit – Complete Project Assembly Report

**Application:** WomanFit – AI Fitness & Wellness  
**Package / Application ID:** `com.aistudio.womanfit.kqvxzl` (`namespace = "com.example"`)  
**Toolchain:** AGP `9.1.1`, Kotlin `2.2.10`, KSP `2.3.6`, Compose BOM `2024.09.00`, Room `2.7.0` (Schema v5), DataStore `1.1.7`, Retrofit `2.12.0` + Moshi `1.15.2`, OkHttp `4.10.0`  
**Backend:** Self-hosted Supabase (`https://womenfit-api.marketsync.cloud`) — GoTrue `/auth/v1` + PostgREST `/rest/v1/womenfit_ai_*` with Row Level Security  

---

## 1. Current Build, Lint & Test Status

| Check | Command / Tool | Result |
|---|---|---|
| **App Compilation** | `compile_applet` | **PASS** (0 errors) |
| **Debug & Release APKs** | `gradle :app:assembleDebug :app:assembleRelease` | **PASS** — generated signed APKs (`app-debug.apk`, `app-release.apk`) |
| **Android Lint** | `lint_applet` | **PASS** (0 errors; `UseKtx` and `RedundantLabel` warnings fixed) |
| **Unit & Robolectric Tests** | `gradle :app:testDebugUnitTest` | **25 / 25 PASSED** |

### Test Breakdown (25 Total)
- `com.example.AuthenticationTest` — **16 / 16 passed** (cold-launch gate, sign-in, invalid credentials, sign-up & duplicate email check, password rules, password recovery, sign-out + DataStore clearance, session refresh/expiration, fail-closed checks, profile/cycle sync, Robolectric Compose UI typing/visibility/no-demo verification)
- `com.example.data.local.DatabaseMigrationTest` — **4 / 4 passed** (non-destructive Room migrations `1->2`, `2->3`, `3->4`, `4->5` including `remoteId TEXT`)
- `com.example.ExampleRobolectricTest` — **4 / 4 passed** (app name, hydration calculator, 28/35-day cycle phase calculations)
- `com.example.ExampleUnitTest` — **1 / 1 passed**

### Generated Installable APK Outputs
- `app/build/outputs/apk/release/app-release.apk` (signed with `debug.keystore` fallback)
- `app/build/outputs/apk/debug/app-debug.apk`
- `.build-outputs/app-debug.apk`

---

## 2. Architecture & Implemented Features

### A. Core Fitness & Wellness Modules
1. **Home Dashboard (`DashboardScreen`):** Daily greeting, workout streak, hydration ring & water intake tracker, active calories/minutes summary, daily goals checklist, and cycle sync card.
2. **Workouts & Player (`WorkoutsScreen`, `WorkoutPlayerScreen`):** Female-focused workout plans with guided exercise timer, rest countdowns, form tips, audio/haptic cues, and workout history logging.
3. **Menstrual Cycle Sync (`CycleScreen`, `CycleCalculator`):** Variable cycle lengths (e.g., 28–35 days), Menstrual / Follicular / Ovulatory / Luteal phase calculation, and phase-aware recommendations.
4. **Nutrition & Hydration (`NutritionScreen`, `NutritionData`):** Curated recipes with macro breakdowns, hormone benefits, meal logging, and personalized daily water goal calculation.
5. **Progress & Achievements (`ProgressScreen`):** Weight, waist, and hip logging, workout history, and achievement badges.
6. **Coach Maya (`AiCoachScreen`, `GeminiCoachService`):** Interactive wellness coach (offline physiology engine + optional `BACKEND_PROXY_URL` hook; chat history is in-memory only).
7. **Profile & Notifications (`ProfileScreen`, `WellnessNotificationManager`):** Profile/stats editor, audio & notification toggles, `POST_NOTIFICATIONS` runtime permission flow, and single Sign Out button with confirmation dialog.

### B. Mandatory Authentication & Security (`auth_graph` / `main_graph`)
1. **Strict Entry Gate (`MainActivity`, `SplashScreen`):** Verifies saved JWT session on startup before showing internal screens; unauthenticated users stay locked in `auth_graph` (`SignInScreen`, `SignUpScreen`, `ForgotPasswordScreen`).
2. **Zero Demo Bypasses:** No demo login buttons or pre-filled demo credentials.
3. **Supabase GoTrue Auth (`SupabaseAuthApi`, `SupabaseAuthRepository`, `SessionDataStore`):** Sign Up, Sign In, Token Refresh (`/auth/v1/token?grant_type=refresh_token`), Password Recovery (`/auth/v1/recover`), and Logout persisted via Jetpack DataStore (`SessionDataStore`).

### C. Two-Way Supabase Cloud Sync (`womenfit_ai_*` Tables)
Shares the VPS Supabase server (`https://womenfit-api.marketsync.cloud`) with the first WomenFit app while isolating all data in dedicated `womenfit_ai_*` tables (`supabase_schema.sql`):
- `womenfit_ai_users` ↔ Room `users` (`UserProfile`)
- `womenfit_ai_daily_progress` ↔ Room `daily_progress` (`DailyProgress`)
- `womenfit_ai_workout_logs` ↔ Room `activity_history` (`ActivityRecord`)
- `womenfit_ai_weight_logs` ↔ Room `weight_logs` (`WeightLogEntry`)
- `womenfit_ai_meal_logs` ↔ Room `logged_meals` (`LoggedMeal`)
- `womenfit_ai_daily_goals` ↔ Room `daily_goals` (`DailyGoalItem`, soft delete via `is_deleted`)

**Sync & Storage Files:**
- `app/src/main/java/com/example/data/remote/WomenFitAiCloudClient.kt` — OkHttp + `org.json` PostgREST client (`upsertRows`, `fetchRows`, `updateRow`, `deleteRow`) with automatic token refresh on expiry or HTTP 401.
- `app/src/main/java/com/example/data/FitnessCloudSync.kt` — Room ↔ Supabase bidirectional sync manager (`syncAll(userId)`, per-entity push methods, `SharedPreferences` offline soft-delete queue using `androidx.core.content.edit`).
- `app/src/main/java/com/example/data/FitnessRepository.kt` — Writes to Room first (offline-safe UI source of truth), pushes to cloud in background, triggers `syncAll` on sign-in, and emits save alerts.
- `app/src/main/java/com/example/data/local/AppDatabase.kt` — Room version **5** with non-destructive `MIGRATION_1_2` through `MIGRATION_4_5` (`remoteId TEXT` on synced tables).

### D. Live Cloud Sync Status Badge & Save Alerts
- `app/src/main/java/com/example/data/CloudStatus.kt` — App-wide `connection: StateFlow<CloudConnection>` (`CHECKING`, `ONLINE`, `OFFLINE`, `NOT_CONFIGURED`) and `alerts: SharedFlow<CloudAlert>`.
- `app/src/main/java/com/example/ui/components/CloudSyncUi.kt` —
  - `CloudSyncBadge()` (shown on `SignInScreen`, `SignUpScreen`, `ProfileScreen`): pings `GET /auth/v1/health` on display and every 30 seconds; shows green pulsing `"Cloud sync active"` when reachable or amber `"Offline mode – saving on phone"` when unreachable.
  - `CloudAlertHost()` (mounted in `MainActivity` over `NavHost`): shows immediate snackbar feedback after every save and sign-in sync (latest alert replaces previous).

---

## 3. Recent Fixes Applied in AI Studio Workspace

1. **KSP Headless Crash Fix (`gradle/libs.versions.toml`):** Upgraded `googleDevtoolsKsp` from `2.3.5` to `2.3.6` to resolve the `AWT-EventQueue-0` `NullPointerException` in `ApplicationManager.getApplication()`.
2. **Lint Warning Fixes:**
   - `app/src/main/java/com/example/data/FitnessCloudSync.kt`: Replaced manual `.edit() ... .apply()` with `androidx.core.content.edit { ... }` (`UseKtx`).
   - `app/src/main/AndroidManifest.xml`: Removed redundant `android:label="@string/app_name"` from `<activity android:name=".MainActivity">` (`RedundantLabel`).

---

## 4. Key Configuration & Rules to Preserve

- **`.env.example`:** Contains `SUPABASE_URL` (`https://womenfit-api.marketsync.cloud`) and the public `SUPABASE_ANON_KEY`, exposed via Secrets Gradle Plugin (`BuildConfig`). Never add a `service_role` key.
- **`app/build.gradle.kts`:** Keep `signingConfigs` unchanged; `applicationId` remains `com.aistudio.womanfit.kqvxzl`.
- **`AI_STUDIO_HANDOFF.md`:** Full backend handoff document for AI Studio / Claude collaboration.

---

## 5. Pending Deployment / Verification Items (Outside App Code)

1. **Supabase VPS SQL Setup:** Ensure `supabase_schema.sql` has been executed in the VPS Supabase SQL Editor (including `notify pgrst, 'reload schema';`) so all 6 `womenfit_ai_*` tables are visible to PostgREST without HTTP 404 (`PGRST205`).
2. **End-to-End Device Test:** Install `app-release.apk`, sign up / sign in, log water/workout/meal/weight/goal, confirm `"saved to cloud ✓"` alerts and rows in Supabase, then reinstall and sign in again to verify full cloud restore.
3. **GitHub Sync:** Push from one single source of truth (AI Studio or local GitHub repo) to avoid merge conflicts.
