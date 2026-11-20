import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import rateLimit from 'express-rate-limit';
import dotenv from 'dotenv';
import admin from 'firebase-admin';
import crypto from 'crypto';
import pg from 'pg';

// Load environment variables from .env file if available
dotenv.config();

const { Pool } = pg;
const app = express();
const PORT = process.env.PORT || 8080;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';
const APP_CLIENT_SECRET = process.env.APP_CLIENT_SECRET || '';
const REQUIRE_FIREBASE_AUTH = process.env.REQUIRE_FIREBASE_AUTH === 'true';
const REQUIRE_APP_CHECK = process.env.REQUIRE_APP_CHECK === 'true';
const FIREBASE_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || '';
const SUPABASE_DB_WOMENFIT = (process.env.SUPABASE_DB_WOMENFIT || '').trim();
const PG_SSL = process.env.PG_SSL === 'true';
const AUTH_JWT_SECRET =
  (process.env.AUTH_JWT_SECRET || '').trim() ||
  crypto.createHash('sha256').update(APP_CLIENT_SECRET || 'womanfit-dev-secret-change-in-prod').digest('hex');

// Initialize PostgreSQL Pool (if SUPABASE_DB_WOMENFIT is set)
let pgPool = null;
let dbSchemaInitialized = false;

if (SUPABASE_DB_WOMENFIT && !SUPABASE_DB_WOMENFIT.includes('USER:PASSWORD')) {
  pgPool = new Pool({
    connectionString: SUPABASE_DB_WOMENFIT,
    ssl: PG_SSL ? { rejectUnauthorized: false } : false,
    connectionTimeoutMillis: 10_000,
    idleTimeoutMillis: 30_000,
    max: 10
  });
  pgPool.on('error', (err) => {
    console.error('[PostgreSQL Pool] Unexpected idle client error:', err.message);
  });
  console.log('🗄️ PostgreSQL pool configured from SUPABASE_DB_WOMENFIT.');
} else {
  console.log('ℹ️ SUPABASE_DB_WOMENFIT not configured. Auth/profile endpoints require SUPABASE_DB_WOMENFIT in server .env.');
}

async function ensureDatabaseSchema() {
  if (!pgPool) {
    throw new Error('PostgreSQL database is not configured.');
  }
  if (dbSchemaInitialized) return;

  await pgPool.query(`
    CREATE TABLE IF NOT EXISTS public.womanfit_users (
      id UUID PRIMARY KEY,
      email TEXT NOT NULL UNIQUE,
      full_name TEXT NOT NULL DEFAULT '',
      password_hash TEXT NOT NULL,
      password_salt TEXT NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  `);

  await pgPool.query(`
    CREATE TABLE IF NOT EXISTS public.profiles (
      id UUID PRIMARY KEY,
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
  `);

  dbSchemaInitialized = true;
}

// Password & HMAC Token Helpers
function hashPassword(password, salt = crypto.randomBytes(16).toString('hex')) {
  const derivedKey = crypto.scryptSync(password, salt, 64).toString('hex');
  return { salt, hash: derivedKey };
}

function verifyPassword(password, salt, expectedHash) {
  const actualBuf = crypto.scryptSync(password, salt, 64);
  const expectedBuf = Buffer.from(expectedHash, 'hex');
  if (actualBuf.length !== expectedBuf.length) return false;
  return crypto.timingSafeEqual(actualBuf, expectedBuf);
}

function base64UrlEncode(obj) {
  return Buffer.from(JSON.stringify(obj), 'utf8').toString('base64url');
}

function signToken(payload, ttlSeconds) {
  const header = base64UrlEncode({ alg: 'HS256', typ: 'JWT' });
  const nowSec = Math.floor(Date.now() / 1000);
  const body = base64UrlEncode({
    ...payload,
    iat: nowSec,
    exp: nowSec + ttlSeconds
  });
  const signature = crypto
    .createHmac('sha256', AUTH_JWT_SECRET)
    .update(`${header}.${body}`)
    .digest('base64url');
  return {
    token: `${header}.${body}.${signature}`,
    expiresAt: nowSec + ttlSeconds,
    expiresIn: ttlSeconds
  };
}

function verifyToken(token, expectedType = 'access') {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  const [header, body, signature] = parts;
  const expectedSig = crypto
    .createHmac('sha256', AUTH_JWT_SECRET)
    .update(`${header}.${body}`)
    .digest('base64url');
  const sigBuf = Buffer.from(signature);
  const expectedBuf = Buffer.from(expectedSig);
  if (sigBuf.length !== expectedBuf.length || !crypto.timingSafeEqual(sigBuf, expectedBuf)) {
    return null;
  }
  try {
    const payload = JSON.parse(Buffer.from(body, 'base64url').toString('utf8'));
    const nowSec = Math.floor(Date.now() / 1000);
    if (!payload.exp || payload.exp <= nowSec) return null;
    if (payload.type !== expectedType) return null;
    return payload;
  } catch {
    return null;
  }
}

function issueAuthSession(user) {
  const access = signToken({ sub: user.id, email: user.email, type: 'access' }, 3600);
  const refresh = signToken({ sub: user.id, email: user.email, type: 'refresh' }, 60 * 60 * 24 * 30);
  return {
    access_token: access.token,
    token_type: 'bearer',
    expires_in: access.expiresIn,
    expires_at: access.expiresAt,
    refresh_token: refresh.token,
    user: {
      id: user.id,
      email: user.email,
      user_metadata: {
        full_name: user.full_name
      }
    }
  };
}

function extractBearerUser(req) {
  const authHeader = req.headers['authorization'] || '';
  if (!authHeader.startsWith('Bearer ')) return null;
  const token = authHeader.slice(7).trim();
  return verifyToken(token, 'access');
}

// Initialize Firebase Admin SDK if credentials or project ID are provided
let firebaseInitialized = false;
try {
  if (FIREBASE_PROJECT_ID || process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    admin.initializeApp({
      projectId: FIREBASE_PROJECT_ID || undefined
    });
    firebaseInitialized = true;
    console.log(`🔥 Firebase Admin initialized successfully for project: ${FIREBASE_PROJECT_ID || 'default'}`);
  } else {
    console.log('ℹ️ Firebase Admin not initialized (FIREBASE_PROJECT_ID not set). Running with secret-key & open-dev authentication.');
  }
} catch (err) {
  console.warn('⚠️ Warning: Firebase Admin initialization failed:', err.message);
}

// Security middleware: Set secure HTTP response headers
app.use(helmet());

// Cross-Origin Resource Sharing
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'apikey', 'Prefer', 'X-Firebase-AppCheck', 'X-WomanFit-App-Key']
}));

// Parse JSON request bodies (limit payload to 64kb to prevent payload injection)
app.use(express.json({ limit: '64kb' }));

// Anti-abuse Rate Limiter for AI Coach
const aiRateLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: "Coach Maya is taking a quick breath! Please wait a moment before sending another question."
  }
});

// Rate Limiter for Auth Endpoints
const authRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 60,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    error: "too_many_requests",
    error_description: "Too many authentication attempts. Please try again later."
  }
});

// Middleware: Verify optional proxy key for DB auth endpoints
function verifyProxyClientKey(req, res, next) {
  if (APP_CLIENT_SECRET && APP_CLIENT_SECRET.trim().length > 0 && APP_CLIENT_SECRET !== 'womanfit_secure_secret_2026_change_me') {
    const apiKeyHeader = req.headers['apikey'] || req.headers['x-womanfit-app-key'];
    if (apiKeyHeader !== APP_CLIENT_SECRET) {
      return res.status(401).json({
        error: 'unauthorized',
        error_description: 'Invalid client API key.'
      });
    }
  }
  next();
}

// Middleware: Verify Firebase Authentication and App Check tokens for AI endpoint
async function authenticateAppClient(req, res, next) {
  const authHeader = req.headers['authorization'];
  const appCheckHeader = req.headers['x-firebase-appcheck'];
  const clientSecretHeader = req.headers['x-womanfit-app-key'];

  if (firebaseInitialized && (REQUIRE_APP_CHECK || appCheckHeader)) {
    if (!appCheckHeader) {
      return res.status(401).json({
        success: false,
        error: "Unauthorized: Missing X-Firebase-AppCheck token."
      });
    }
    try {
      const appCheckClaims = await admin.appCheck().verifyToken(appCheckHeader);
      req.appCheckClaims = appCheckClaims;
    } catch (err) {
      console.warn('[AI Proxy] App Check token verification failed:', err.message);
      if (REQUIRE_APP_CHECK) {
        return res.status(401).json({
          success: false,
          error: "Unauthorized: Invalid or expired Firebase App Check token."
        });
      }
    }
  }

  if (firebaseInitialized && (REQUIRE_FIREBASE_AUTH || (authHeader && authHeader.startsWith('Bearer ')))) {
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        error: "Unauthorized: Missing or invalid Authorization Bearer token."
      });
    }
    const idToken = authHeader.split('Bearer ')[1].trim();
    try {
      const decodedUser = await admin.auth().verifyIdToken(idToken);
      req.user = decodedUser;
    } catch (err) {
      console.warn('[AI Proxy] Firebase Auth token verification failed:', err.message);
      if (REQUIRE_FIREBASE_AUTH) {
        return res.status(401).json({
          success: false,
          error: "Unauthorized: Invalid or expired Firebase ID token."
        });
      }
    }
  }

  if (APP_CLIENT_SECRET && APP_CLIENT_SECRET.trim().length > 0) {
    const isSecretMatch = (clientSecretHeader === APP_CLIENT_SECRET) || (authHeader === APP_CLIENT_SECRET);
    if (!req.user && !isSecretMatch && !REQUIRE_FIREBASE_AUTH) {
      return res.status(401).json({
        success: false,
        error: "Unauthorized: Invalid WomanFit client credentials."
      });
    }
  }

  next();
}

// 1. Health Check Endpoint
app.get(['/health', '/'], async (req, res) => {
  let dbReachable = false;
  if (pgPool) {
    try {
      await pgPool.query('SELECT 1');
      dbReachable = true;
    } catch {
      dbReachable = false;
    }
  }

  res.status(200).json({
    status: 'healthy',
    service: 'womanfit-backend-proxy',
    version: '1.3.0',
    databaseConfigured: Boolean(pgPool),
    databaseReachable: dbReachable,
    firebaseAdmin: firebaseInitialized,
    requireFirebaseAuth: REQUIRE_FIREBASE_AUTH,
    requireAppCheck: REQUIRE_APP_CHECK,
    geminiConfigured: Boolean(GEMINI_API_KEY && GEMINI_API_KEY !== 'your_gemini_api_key_here'),
    timestamp: Date.now()
  });
});

// ---------------------------------------------------------------------------
// 2. Supabase-Compatible PostgreSQL Auth & Profile Endpoints
// ---------------------------------------------------------------------------

app.post('/auth/v1/signup', authRateLimiter, verifyProxyClientKey, async (req, res) => {
  try {
    if (!pgPool) {
      return res.status(503).json({
        error: 'database_unconfigured',
        error_description: 'SUPABASE_DB_WOMENFIT is not configured on backend-proxy.'
      });
    }
    await ensureDatabaseSchema();

    const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : '';
    const password = typeof req.body?.password === 'string' ? req.body.password : '';
    const fullName = typeof req.body?.data?.full_name === 'string' ? req.body.data.full_name.trim() : '';

    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({
        error: 'invalid_email',
        error_description: 'Unable to validate email address: invalid format'
      });
    }
    if (password.length < 6) {
      return res.status(400).json({
        error: 'weak_password',
        error_description: 'Password should be at least 6 characters.'
      });
    }

    const existing = await pgPool.query('SELECT id FROM public.womanfit_users WHERE email = $1', [email]);
    if (existing.rows.length > 0) {
      return res.status(422).json({
        error: 'user_already_exists',
        msg: 'User already registered'
      });
    }

    const userId = crypto.randomUUID();
    const { salt, hash } = hashPassword(password);
    const displayName = fullName || 'WomanFit Member';

    await pgPool.query(
      `INSERT INTO public.womanfit_users (id, email, full_name, password_hash, password_salt)
       VALUES ($1, $2, $3, $4, $5)`,
      [userId, email, displayName, hash, salt]
    );

    await pgPool.query(
      `INSERT INTO public.profiles (id, full_name, email)
       VALUES ($1, $2, $3)
       ON CONFLICT (id) DO NOTHING`,
      [userId, displayName, email]
    );

    return res.status(200).json(issueAuthSession({ id: userId, email, full_name: displayName }));
  } catch (err) {
    console.error('[Auth Proxy] SignUp error:', err.message);
    return res.status(500).json({
      error: 'server_error',
      error_description: 'Unable to complete registration.'
    });
  }
});

app.post('/auth/v1/token', authRateLimiter, verifyProxyClientKey, async (req, res) => {
  try {
    if (!pgPool) {
      return res.status(503).json({
        error: 'database_unconfigured',
        error_description: 'SUPABASE_DB_WOMENFIT is not configured on backend-proxy.'
      });
    }
    await ensureDatabaseSchema();

    const grantType = req.query.grant_type;
    if (grantType === 'refresh_token') {
      const refreshToken = req.body?.refresh_token;
      const decoded = verifyToken(refreshToken, 'refresh');
      if (!decoded) {
        return res.status(400).json({
          error: 'invalid_grant',
          error_description: 'Invalid Refresh Token'
        });
      }
      const userRes = await pgPool.query(
        'SELECT id, email, full_name FROM public.womanfit_users WHERE id = $1',
        [decoded.sub]
      );
      if (userRes.rows.length === 0) {
        return res.status(401).json({
          error: 'unauthorized',
          error_description: 'User no longer exists.'
        });
      }
      return res.status(200).json(issueAuthSession(userRes.rows[0]));
    }

    const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : '';
    const password = typeof req.body?.password === 'string' ? req.body.password : '';

    const userRes = await pgPool.query(
      'SELECT id, email, full_name, password_hash, password_salt FROM public.womanfit_users WHERE email = $1',
      [email]
    );
    if (userRes.rows.length === 0) {
      return res.status(400).json({
        error: 'invalid_grant',
        error_description: 'Invalid login credentials'
      });
    }

    const user = userRes.rows[0];
    if (!verifyPassword(password, user.password_salt, user.password_hash)) {
      return res.status(400).json({
        error: 'invalid_grant',
        error_description: 'Invalid login credentials'
      });
    }

    return res.status(200).json(issueAuthSession(user));
  } catch (err) {
    console.error('[Auth Proxy] Token error:', err.message);
    return res.status(500).json({
      error: 'server_error',
      error_description: 'Unable to sign in right now.'
    });
  }
});

app.get('/auth/v1/user', verifyProxyClientKey, async (req, res) => {
  try {
    if (!pgPool) {
      return res.status(503).json({ error: 'database_unconfigured' });
    }
    await ensureDatabaseSchema();

    const claims = extractBearerUser(req);
    if (!claims) {
      return res.status(401).json({ error: 'unauthorized', message: 'Invalid JWT' });
    }

    const userRes = await pgPool.query(
      'SELECT id, email, full_name FROM public.womanfit_users WHERE id = $1',
      [claims.sub]
    );
    if (userRes.rows.length === 0) {
      return res.status(401).json({ error: 'unauthorized', message: 'User not found' });
    }

    const user = userRes.rows[0];
    return res.status(200).json({
      id: user.id,
      email: user.email,
      user_metadata: { full_name: user.full_name }
    });
  } catch (err) {
    console.error('[Auth Proxy] Get user error:', err.message);
    return res.status(500).json({ error: 'server_error' });
  }
});

app.post('/auth/v1/logout', verifyProxyClientKey, (req, res) => {
  res.status(204).send();
});

app.post('/auth/v1/recover', authRateLimiter, verifyProxyClientKey, async (req, res) => {
  try {
    if (!pgPool) {
      return res.status(503).json({
        error: 'database_unconfigured',
        error_description: 'SUPABASE_DB_WOMENFIT is not configured on backend-proxy.'
      });
    }
    await ensureDatabaseSchema();
    // Always return 200 to prevent account enumeration
    return res.status(200).json({});
  } catch (err) {
    console.error('[Auth Proxy] Recover error:', err.message);
    return res.status(500).json({ error: 'server_error' });
  }
});

app.get('/rest/v1/profiles', verifyProxyClientKey, async (req, res) => {
  try {
    if (!pgPool) {
      return res.status(503).json({ error: 'database_unconfigured' });
    }
    await ensureDatabaseSchema();

    const claims = extractBearerUser(req);
    if (!claims) {
      return res.status(401).json({ error: 'unauthorized', message: 'Invalid JWT' });
    }

    const profileRes = await pgPool.query(
      `SELECT id, full_name, email, fitness_goal, fitness_level, age, height_cm,
              current_weight_kg, target_weight_kg, daily_water_goal_ml,
              cycle_length_days, last_period_start_timestamp
       FROM public.profiles
       WHERE id = $1`,
      [claims.sub]
    );
    return res.status(200).json(profileRes.rows);
  } catch (err) {
    console.error('[Auth Proxy] Get profile error:', err.message);
    return res.status(500).json({ error: 'server_error' });
  }
});

app.post('/rest/v1/profiles', verifyProxyClientKey, async (req, res) => {
  try {
    if (!pgPool) {
      return res.status(503).json({ error: 'database_unconfigured' });
    }
    await ensureDatabaseSchema();

    const claims = extractBearerUser(req);
    if (!claims) {
      return res.status(401).json({ error: 'unauthorized', message: 'Invalid JWT' });
    }

    const body = req.body || {};
    const upsertRes = await pgPool.query(
      `INSERT INTO public.profiles (
         id, full_name, email, fitness_goal, fitness_level, age, height_cm,
         current_weight_kg, target_weight_kg, daily_water_goal_ml,
         cycle_length_days, last_period_start_timestamp, updated_at
       )
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, NOW())
       ON CONFLICT (id) DO UPDATE SET
         full_name = EXCLUDED.full_name,
         email = EXCLUDED.email,
         fitness_goal = EXCLUDED.fitness_goal,
         fitness_level = EXCLUDED.fitness_level,
         age = EXCLUDED.age,
         height_cm = EXCLUDED.height_cm,
         current_weight_kg = EXCLUDED.current_weight_kg,
         target_weight_kg = EXCLUDED.target_weight_kg,
         daily_water_goal_ml = EXCLUDED.daily_water_goal_ml,
         cycle_length_days = EXCLUDED.cycle_length_days,
         last_period_start_timestamp = EXCLUDED.last_period_start_timestamp,
         updated_at = NOW()
       RETURNING id, full_name, email, fitness_goal, fitness_level, age, height_cm,
                 current_weight_kg, target_weight_kg, daily_water_goal_ml,
                 cycle_length_days, last_period_start_timestamp`,
      [
        claims.sub,
        String(body.full_name || 'WomanFit Member'),
        String(body.email || claims.email || ''),
        String(body.fitness_goal || 'Tone & Sculpt'),
        String(body.fitness_level || 'Intermediate'),
        Number.isFinite(Number(body.age)) ? parseInt(body.age, 10) : 28,
        Number.isFinite(Number(body.height_cm)) ? Number(body.height_cm) : 168.0,
        Number.isFinite(Number(body.current_weight_kg)) ? Number(body.current_weight_kg) : 62.5,
        Number.isFinite(Number(body.target_weight_kg)) ? Number(body.target_weight_kg) : 58.0,
        Number.isFinite(Number(body.daily_water_goal_ml)) ? parseInt(body.daily_water_goal_ml, 10) : 2500,
        Number.isFinite(Number(body.cycle_length_days)) ? parseInt(body.cycle_length_days, 10) : 28,
        Number.isFinite(Number(body.last_period_start_timestamp)) ? parseInt(body.last_period_start_timestamp, 10) : 0
      ]
    );

    return res.status(200).json(upsertRes.rows);
  } catch (err) {
    console.error('[Auth Proxy] Upsert profile error:', err.message);
    return res.status(500).json({ error: 'server_error' });
  }
});

// ---------------------------------------------------------------------------
// 3. Coach Maya AI Assistant Endpoint (supports /api/coach/ask, /coach, and root POST)
// ---------------------------------------------------------------------------
app.post(['/api/coach/ask', '/coach', '/'], aiRateLimiter, authenticateAppClient, async (req, res) => {
  try {
    const { question, userGoal, fitnessLevel, currentCyclePhase } = req.body;

    if (!question || typeof question !== 'string' || question.trim().length < 2) {
      return res.status(400).json({
        success: false,
        error: "Question is required and must be at least 2 characters long."
      });
    }

    if (question.length > 1000) {
      return res.status(400).json({
        success: false,
        error: "Question exceeds maximum allowed length of 1000 characters."
      });
    }

    const sanitizedQuestion = question.trim();
    const sanitizedGoal = typeof userGoal === 'string' && userGoal.trim().length > 0
      ? userGoal.trim().slice(0, 100)
      : "Overall Health, Strength & Toning";
    const sanitizedLevel = typeof fitnessLevel === 'string' && fitnessLevel.trim().length > 0
      ? fitnessLevel.trim().slice(0, 50)
      : "Intermediate";
    const sanitizedCycle = typeof currentCyclePhase === 'string' && currentCyclePhase.trim().length > 0
      ? currentCyclePhase.trim().slice(0, 50)
      : null;

    if (!GEMINI_API_KEY || GEMINI_API_KEY === 'your_gemini_api_key_here') {
      return res.status(503).json({
        success: false,
        error: "Server-side Gemini API key is not configured. Please set GEMINI_API_KEY in environment variables."
      });
    }

    const systemInstruction = `You are Coach Maya, an expert certified female fitness trainer, sports nutritionist, and holistic wellness specialist in the WomanFit mobile app.
Your tone is warm, inspiring, science-grounded, empathetic, and tailored specifically to female physiology, hormone balance, pelvic floor health, cycle-syncing, progressive resistance, and posture.

User Profile Context:
- Primary Goal: ${sanitizedGoal}
- Fitness Level: ${sanitizedLevel}
${sanitizedCycle ? `- Current Menstrual Cycle Phase: ${sanitizedCycle}` : ''}

Communication & Safety Guidelines:
1. Provide concise, highly actionable advice (2 to 4 structured paragraphs or bullet points).
2. Incorporate female-specific bio-mechanics, hydration, and recovery tips.
3. Use gentle, empowering emojis where natural (🌸, ✨, 💪, 🧘‍♀️).
4. Medical Safety Guardrail: Never diagnose medical conditions, prescribe pharmaceuticals, or offer contraceptive advice. If severe pain, injury, or eating disorders are mentioned, warmly recommend consulting a qualified physician or OB-GYN.`;

    const geminiPayload = {
      contents: [
        {
          role: "user",
          parts: [
            {
              text: `${systemInstruction}\n\nUser Question:\n${sanitizedQuestion}`
            }
          ]
        }
      ],
      generationConfig: {
        temperature: 0.7,
        topK: 40,
        topP: 0.95,
        maxOutputTokens: 1024
      }
    };

    const upstreamUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${encodeURIComponent(GEMINI_API_KEY)}`;

    const upstreamResponse = await fetch(upstreamUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(geminiPayload)
    });

    if (!upstreamResponse.ok) {
      const errorText = await upstreamResponse.text();
      console.error(`[AI Proxy] Upstream Gemini error HTTP ${upstreamResponse.status}:`, errorText);
      return res.status(upstreamResponse.status >= 500 ? 502 : 400).json({
        success: false,
        error: `Upstream AI service error (${upstreamResponse.status}). Please try again shortly.`
      });
    }

    const upstreamData = await upstreamResponse.json();
    const candidate = upstreamData?.candidates?.[0];
    const replyText = candidate?.content?.parts?.[0]?.text;

    if (!replyText || replyText.trim().length === 0) {
      return res.status(502).json({
        success: false,
        error: "Empty response received from AI model."
      });
    }

    return res.status(200).json({
      success: true,
      reply: replyText.trim(),
      model: "gemini-2.5-flash",
      timestamp: Date.now()
    });

  } catch (err) {
    console.error("[AI Proxy] Unexpected exception in /api/coach/ask:", err);
    return res.status(500).json({
      success: false,
      error: "Internal server error while processing Coach Maya request."
    });
  }
});

// Fallback for unhandled routes
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: `Route not found: ${req.method} ${req.url}`
  });
});

// Start Express Server
const server = app.listen(PORT, () => {
  console.log(`🌸 WomanFit Secure Backend Proxy listening on port ${PORT}`);
  console.log(`🗄️ PostgreSQL configured: ${Boolean(pgPool)}`);
  console.log(`🔒 Gemini API Key configured: ${Boolean(GEMINI_API_KEY && GEMINI_API_KEY !== 'your_gemini_api_key_here')}`);
});

process.on('SIGTERM', () => {
  console.log('SIGTERM signal received. Closing HTTP server gracefully...');
  server.close(async () => {
    if (pgPool) await pgPool.end().catch(() => {});
    process.exit(0);
  });
});

process.on('SIGINT', () => {
  console.log('SIGINT signal received. Closing HTTP server gracefully...');
  server.close(async () => {
    if (pgPool) await pgPool.end().catch(() => {});
    process.exit(0);
  });
});
