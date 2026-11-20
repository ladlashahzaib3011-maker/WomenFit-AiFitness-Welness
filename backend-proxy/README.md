# 🌸 WomanFit Secure AI Backend Proxy

Production-ready, serverless-compatible API Proxy for the **WomanFit Android Application**.

## Overview
This proxy acts as a hardened security gateway between the WomanFit Android app and Google Gemini 2.5 Flash:
1. **0% APK Key Exposure**: Keeps the `GEMINI_API_KEY` strictly on the cloud server. The Android APK never bundles, contains, or decompiles to reveal the Gemini key.
2. **Firebase Auth & App Check Verification**: Verifies `Authorization: Bearer <ID_TOKEN>` via Firebase Admin and `X-Firebase-AppCheck` via Google Play Integrity.
3. **Server-Side Persona Injection**: Guarantees Coach Maya's certified female fitness coach persona cannot be modified by client manipulation.
4. **Anti-Abuse Rate Limiting**: Enforces a 20 request/minute rate limit to prevent bill shocks and API abuse.
5. **Input Sanitization & Length Guardrails**: Validates questions, trims whitespace, limits payload size to 64KB, and prevents prompt injection.

---

## 🚀 Quick Local Run

1. Navigate to the proxy directory:
   ```bash
   cd backend-proxy
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Copy environment configuration:
   ```bash
   cp .env.example .env
   ```
4. Set your PostgreSQL database string (`SUPABASE_DB_WOMENFIT`), signing secret, and optional Gemini key in `.env` (server-side only):
   ```env
   SUPABASE_DB_WOMENFIT=postgresql://USER:PASSWORD@HOST:PORT/postgres
   AUTH_JWT_SECRET=your_random_jwt_signing_secret
   PG_SSL=false
   GEMINI_API_KEY=AIzaSy...
   APP_CLIENT_SECRET=my_womanfit_secret_2026
   ```
5. Start server:
   ```bash
   npm start
   ```

The proxy automatically initializes `public.womanfit_users` and `public.profiles` and exposes `/auth/v1/*` and `/rest/v1/profiles` endpoints so the WomanFit Android app connects safely over HTTPS without storing database passwords in the APK.

---

## ☁️ 1-Click Deployment Options

### Option 1: Google Cloud Run (Recommended for Android / Google Cloud)
Cloud Run automatically scales down to zero (free tier covers 2 million requests/month):
```bash
# Build and deploy from source
gcloud run deploy womanfit-ai-proxy \
  --source . \
  --platform managed \
  --region asia-south1 \
  --allow-unauthenticated \
  --set-env-vars GEMINI_API_KEY="your_gemini_api_key",FIREBASE_PROJECT_ID="your_project_id"
```

### Option 2: Render / Railway / Heroku
1. Push this folder to your GitHub repo.
2. Link the repository to Render / Railway.
3. Add the Environment Variables:
   - `GEMINI_API_KEY`
   - `FIREBASE_PROJECT_ID`
   - `APP_CLIENT_SECRET`
4. The service will be live on an `https://...` URL.

---

## 📱 Android Client Connection
In the WomanFit Android app:
- Set `BACKEND_PROXY_URL=https://your-cloud-run-service.a.run.app/api/coach/ask` in `.env`.
- When users sign in, the app automatically passes `Authorization: Bearer <Firebase-ID-Token>` and `X-Firebase-AppCheck`.
- If the proxy is not configured or offline, WomanFit automatically falls back to its built-in offline Coach Maya engine.
