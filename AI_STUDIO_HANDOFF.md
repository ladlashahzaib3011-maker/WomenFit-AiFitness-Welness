# WomanFit AI – Backend Handoff for AI Studio

> **To AI Studio:** Read this whole file before changing any code. It describes backend work
> already done in this project outside AI Studio (commit `2d728db` and later). Keep everything
> described here working. Do **not** revert, rename or "simplify" these parts unless the owner asks.

---

## 1. Goal of this work

This app (**WomanFit – AI Fitness & Wellness**, `com.aistudio.womanfit.kqvxzl`) now saves user data
to the **same self-hosted Supabase server** that the owner's first app (**WomenFit**) already uses,
with the **same connection settings** and the **same request style**, but in its **own tables**
(all prefixed `womenfit_ai_`) so the two apps' data never mix.

| Item | Value |
|---|---|
| Supabase URL | `https://womenfit-api.marketsync.cloud` |
| Anon key | Same public anon key as the first app (in `.env.example`, role `anon`) |
| Auth | Supabase GoTrue `/auth/v1` (sign up, sign in, refresh, logout, password reset) |
| Data | Supabase PostgREST `/rest/v1/womenfit_ai_*` with Row Level Security |
| Local cache | Room database (UI reads Room; works offline) |

The anon key is a **public client key**. Security comes from Row Level Security on the server
(every row is limited to `auth.uid()`). Never put a `service_role` key in the app.

---

## 2. Server tables (`supabase_schema.sql`)

Each user can read and write only their own rows (RLS). The original app's tables
(`profiles`, `workout_logs`, `water_logs`, `challenge_progress`, `custom_routines`) are
**not touched and must never be used by this app**.

| Supabase table | Room table / entity | Key | Contents |
|---|---|---|---|
| `womenfit_ai_users` | `users` / `UserProfile` | `id` = auth user id | name, email, fitness goal & level, age, height, current/target weight, water goal, cycle length, last period start |
| `womenfit_ai_daily_progress` | `daily_progress` / `DailyProgress` | unique (`user_id`, `log_date`) | water ml, calories burned, active minutes, workouts count, streak |
| `womenfit_ai_workout_logs` | `activity_history` / `ActivityRecord` | `id` uuid | workout title, duration, calories, category, `logged_at_ms` |
| `womenfit_ai_weight_logs` | `weight_logs` / `WeightLogEntry` | `id` uuid | date label, weight, waist, hips, note |
| `womenfit_ai_meal_logs` | `logged_meals` / `LoggedMeal` | `id` uuid | title, category, calories, protein, `logged_at_ms` |
| `womenfit_ai_daily_goals` | `daily_goals` / `DailyGoalItem` | `id` uuid | title, category, `is_completed`, `is_deleted` (soft delete) |

**Status:** the SQL must be run once in the Supabase SQL Editor on the VPS. Until then, these
tables return 404 and the app keeps data locally only (it does not crash).

---

## 3. Code changes (what exists now and why)

### Configuration
- **`.env.example`** – `SUPABASE_URL` and `SUPABASE_ANON_KEY` are set to the shared server values.
  The Secrets Gradle Plugin turns them into `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_ANON_KEY`.
  Keep these values unless the owner gives new ones.

### Auth & profile
- **`data/remote/SupabaseAuthApi.kt`** – profile endpoints now use
  `GET rest/v1/womenfit_ai_users` and `POST rest/v1/womenfit_ai_users?on_conflict=id`
  (with `Prefer: resolution=merge-duplicates`). **Never point these back to `profiles`.**
- **`data/SupabaseAuthRepository.kt`** – on sign-in, if the user has no `womenfit_ai_users` row
  yet (email-confirmed sign-up, or an account first made in the other app), it creates one.

### Cloud client – same method as the first WomenFit app
- **`data/remote/WomenFitAiCloudClient.kt`** (new) – plain **OkHttp + org.json** PostgREST client,
  the same approach as the first app's `SupabaseClient`:
  - Headers on every call: `apikey: <anon key>` and `Authorization: Bearer <user access token>`.
  - Reads the session from `SessionDataStore`; refreshes the token when it is about to expire
    and once more on HTTP 401 (`POST /auth/v1/token?grant_type=refresh_token`), saves the new
    tokens back to `SessionDataStore`, then retries.
  - Operations: `upsertRows`, `fetchRows`, `updateRow`, `deleteRow`.
  - Table name constants live in its `companion object` (`TABLE_USERS`, `TABLE_DAILY_PROGRESS`, …).

### Sync manager
- **`data/FitnessCloudSync.kt`** (new) – converts Room rows ⇄ Supabase JSON.
  - **Every save:** written to Room first, then pushed to Supabase in the background.
  - **On every sign-in / app start with a session:** `syncAll(userId)` uploads all local rows
    (giving older rows a uuid), then downloads rows that exist only on the server
    (new phone, reinstall).
  - **Goal delete = soft delete:** sets `is_deleted = true` on the server so other phones also
    remove it. Deletes made offline are kept in SharedPreferences (`womenfit_ai_cloud_sync`)
    and retried on the next sync. Goal upserts never send `is_deleted`, so they can't undo a delete.
  - All network errors are caught and logged; Room stays the source of truth for the UI.
- **`data/FitnessRepository.kt`** – new optional constructor parameter `cloudSync: FitnessCloudSync?`
  (null in unit tests). Calls the push functions after `incrementWater`, `resetWater`,
  `addGoal`, `toggleGoal`, `deleteGoal`, `logWeight`, `logCompletedWorkout`, `logMeal`;
  starts `syncAll` whenever `activeUserId` becomes a signed-in user.
- **`data/AppContainer.kt`** – builds `WomenFitAiCloudClient` + `FitnessCloudSync` and passes them
  into `FitnessRepository`.

### Local database
- **`model/Models.kt`** – `DailyGoalItem`, `WeightLogEntry`, `ActivityRecord`, `LoggedMeal`
  have a new field `remoteId: String? = null` (the Supabase row uuid).
- **`data/local/AppDatabase.kt`** – Room **version 5**, new `MIGRATION_4_5` adds the nullable
  `remoteId TEXT` column to `daily_goals`, `weight_logs`, `activity_history`, `logged_meals`.
  No data is lost. **Do not use destructive migration.**
- **`data/local/Daos.kt`** – new direct (non-Flow) queries and `@Update` methods used by the sync.
- **`app/src/test/.../DatabaseMigrationTest.kt`** – new test for migration 4 → 5.

---

## 4. Rules for AI Studio when editing this project

1. Keep the `womenfit_ai_` table names exactly as they are. Do not create or use `profiles`.
2. Keep `SUPABASE_URL` / `SUPABASE_ANON_KEY` in `.env.example`. Never add a `service_role` key.
3. Any new user data that should be saved online gets its **own** `womenfit_ai_*` table with
   RLS (`auth.uid() = user_id`). Add it to `supabase_schema.sql`, a push call in
   `FitnessRepository`, and a sync step in `FitnessCloudSync`.
4. Any Room schema change needs a new version and a non-destructive `Migration`. Keep
   `MIGRATION_1_2` … `MIGRATION_4_5`.
5. Keep `remoteId` on the four entities; new rows must get `FitnessCloudSync.newRemoteId()`.
6. Network calls must never crash the app or block the UI; Room is written first.
7. Never log passwords, access tokens or refresh tokens.
8. Don't change the `signingConfigs` block in `app/build.gradle.kts`.
9. `backend-proxy/` (Node.js) is only a fallback when `SUPABASE_URL` is empty. Its SQL creates
   `public.profiles`, so **never run it against the shared WomenFit database**.

---

## 5. Still to do

| # | Task | Who |
|---|---|---|
| 1 | Run `supabase_schema.sql` in the Supabase SQL Editor on the VPS | Owner |
| 2 | Build the app and run the unit tests (`DatabaseMigrationTest`, `AuthenticationTest`); fix any compile errors **without changing the behaviour above** | AI Studio / Android Studio |
| 3 | Test on a phone: sign up, then log water, a workout, a meal, weight and a goal; check the rows in Supabase; reinstall and sign in again to confirm the data comes back | Owner |
| 4 | Push the commits to GitHub | Owner |

---

## 6. Things to know

- Both apps share one Supabase Auth user list, so the same email and password work in both apps.
  Their data stays separate.
- The first app has an `auth.users` trigger that creates a row in its own `profiles` table for every
  new account, including accounts made in this app. This is harmless.
- Coach Maya chat history is still in memory only (not saved to Supabase).
