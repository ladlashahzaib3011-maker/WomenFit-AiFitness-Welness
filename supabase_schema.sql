-- =========================================================================
-- WOMENFIT AI FITNESS & WELLNESS - SUPABASE SCHEMA + ROW LEVEL SECURITY
--
-- This app shares the SAME self-hosted Supabase server as the original
-- WomenFit app (https://womenfit-api.marketsync.cloud). To keep the two apps'
-- data apart, every table here uses the "womenfit_ai_" prefix. The original
-- app's tables (profiles, workout_logs, water_logs, ...) are NOT touched.
--
-- Run this whole script once in the Supabase SQL Editor on the VPS.
-- It is safe to re-run: tables use IF NOT EXISTS and policies are dropped first.
-- =========================================================================

-- 1. USER PROFILE (one row per auth user)
create table if not exists public.womenfit_ai_users (
    id uuid primary key references auth.users(id) on delete cascade,
    full_name text not null default '',
    email text not null default '',
    fitness_goal text not null default 'Tone & Sculpt',
    fitness_level text not null default 'Intermediate',
    age integer not null default 28,
    height_cm real not null default 168.0,
    current_weight_kg real not null default 62.5,
    target_weight_kg real not null default 58.0,
    daily_water_goal_ml integer not null default 2500,
    cycle_length_days integer not null default 28,
    last_period_start_timestamp bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- 2. DAILY PROGRESS (water, calories, active minutes - one row per user per day)
create table if not exists public.womenfit_ai_daily_progress (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    log_date date not null,
    water_intake_ml integer not null default 0,
    calories_burned integer not null default 0,
    active_minutes integer not null default 0,
    completed_workouts_count integer not null default 0,
    streak_days integer not null default 1,
    updated_at timestamptz not null default now(),
    unique (user_id, log_date)
);

-- 3. COMPLETED WORKOUTS
create table if not exists public.womenfit_ai_workout_logs (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    workout_title text not null,
    duration_minutes integer not null,
    calories_burned integer not null,
    category text not null default '',
    logged_at_ms bigint not null,
    created_at timestamptz not null default now()
);

-- 4. WEIGHT & BODY MEASUREMENTS
create table if not exists public.womenfit_ai_weight_logs (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    date_label text not null,
    weight_kg real not null,
    waist_cm real,
    hips_cm real,
    note text not null default '',
    created_at timestamptz not null default now()
);

-- 5. LOGGED MEALS
create table if not exists public.womenfit_ai_meal_logs (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    category text not null,
    calories integer not null,
    protein integer not null,
    logged_at_ms bigint not null,
    created_at timestamptz not null default now()
);

-- 6. DAILY GOALS / CHECKLIST
create table if not exists public.womenfit_ai_daily_goals (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    category text not null default 'Wellness',
    is_completed boolean not null default false,
    -- Soft delete: the app sets this instead of deleting, so other phones also remove the goal
    -- (a hard delete would be re-uploaded by any phone that still has the goal cached).
    is_deleted boolean not null default false,
    created_at timestamptz not null default now()
);
alter table public.womenfit_ai_daily_goals add column if not exists is_deleted boolean not null default false;

create index if not exists womenfit_ai_workout_logs_user_idx on public.womenfit_ai_workout_logs (user_id);
create index if not exists womenfit_ai_weight_logs_user_idx on public.womenfit_ai_weight_logs (user_id);
create index if not exists womenfit_ai_meal_logs_user_idx on public.womenfit_ai_meal_logs (user_id);
create index if not exists womenfit_ai_daily_goals_user_idx on public.womenfit_ai_daily_goals (user_id);

-- =========================================================================
-- ROW LEVEL SECURITY: each user can only read/write her own rows
-- =========================================================================
alter table public.womenfit_ai_users enable row level security;
alter table public.womenfit_ai_daily_progress enable row level security;
alter table public.womenfit_ai_workout_logs enable row level security;
alter table public.womenfit_ai_weight_logs enable row level security;
alter table public.womenfit_ai_meal_logs enable row level security;
alter table public.womenfit_ai_daily_goals enable row level security;

-- Profile (keyed by id = auth.uid())
drop policy if exists "womenfit_ai_users_select_own" on public.womenfit_ai_users;
create policy "womenfit_ai_users_select_own" on public.womenfit_ai_users
    for select using (auth.uid() = id);
drop policy if exists "womenfit_ai_users_insert_own" on public.womenfit_ai_users;
create policy "womenfit_ai_users_insert_own" on public.womenfit_ai_users
    for insert with check (auth.uid() = id);
drop policy if exists "womenfit_ai_users_update_own" on public.womenfit_ai_users;
create policy "womenfit_ai_users_update_own" on public.womenfit_ai_users
    for update using (auth.uid() = id) with check (auth.uid() = id);

-- All other tables (keyed by user_id = auth.uid()); full CRUD on own rows.
-- UPDATE is required by the app's upserts (Prefer: resolution=merge-duplicates).
do $$
declare
    t text;
begin
    foreach t in array array[
        'womenfit_ai_daily_progress',
        'womenfit_ai_workout_logs',
        'womenfit_ai_weight_logs',
        'womenfit_ai_meal_logs',
        'womenfit_ai_daily_goals'
    ] loop
        execute format('drop policy if exists %I on public.%I', t || '_select_own', t);
        execute format('create policy %I on public.%I for select using (auth.uid() = user_id)', t || '_select_own', t);
        execute format('drop policy if exists %I on public.%I', t || '_insert_own', t);
        execute format('create policy %I on public.%I for insert with check (auth.uid() = user_id)', t || '_insert_own', t);
        execute format('drop policy if exists %I on public.%I', t || '_update_own', t);
        execute format('create policy %I on public.%I for update using (auth.uid() = user_id) with check (auth.uid() = user_id)', t || '_update_own', t);
        execute format('drop policy if exists %I on public.%I', t || '_delete_own', t);
        execute format('create policy %I on public.%I for delete using (auth.uid() = user_id)', t || '_delete_own', t);
    end loop;
end $$;

-- Make the new tables visible to the REST API (PostgREST) for signed-in users.
grant select, insert, update, delete on
    public.womenfit_ai_users,
    public.womenfit_ai_daily_progress,
    public.womenfit_ai_workout_logs,
    public.womenfit_ai_weight_logs,
    public.womenfit_ai_meal_logs,
    public.womenfit_ai_daily_goals
to authenticated;

-- Ask PostgREST to reload its schema cache so the new tables work immediately.
notify pgrst, 'reload schema';

-- NOTE: no auth.users trigger is added here on purpose. The original WomenFit app
-- already has one (on_auth_user_created -> public.profiles). This app creates its
-- own womenfit_ai_users row from the client on sign-up / first sign-in instead.
