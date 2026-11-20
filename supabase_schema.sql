-- WomanFit Supabase PostgreSQL Schema & Row-Level Security (RLS)
-- Run this script in your Supabase Project -> SQL Editor after creating your project.

CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL DEFAULT '',
    fitness_goal TEXT NOT NULL DEFAULT 'Tone & Sculpt',
    fitness_level TEXT NOT NULL DEFAULT 'Intermediate',
    age INTEGER NOT NULL DEFAULT 28,
    height_cm REAL NOT NULL DEFAULT 168.0,
    current_weight_kg REAL NOT NULL DEFAULT 62.5,
    target_weight_kg REAL NOT NULL DEFAULT 58.0,
    daily_water_goal_ml INTEGER NOT NULL DEFAULT 2500,
    cycle_length_days INTEGER NOT NULL DEFAULT 28,
    last_period_start_timestamp BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Enable Row-Level Security
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Users can only read their own profile
CREATE POLICY "Users can view own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

-- Users can only insert their own profile
CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

-- Users can only update their own profile
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);
