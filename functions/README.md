# 🌸 WomanFit — Firebase Cloud Functions Deployment Guide

Firebase Cloud Functions v2 par **Coach Maya AI Proxy** deploy karne ka step-by-step tareeqa.

---

## 🔒 Security Architecture
- **0% Android APK Key Exposure:** `GEMINI_API_KEY` hamesha server-side Firebase environment/secrets mein rehti hai.
- **Firebase Auth & App Check:** Unauthorized access aur bot abuse se bachata hai.
- **Auto-Scale to Zero:** Jab koi user query nahi karta, cost $0 hoti hai (Firebase free tier covers millions of invocations/month).

---

## 🛠️ Prerequisites
1. **Node.js** (v20 LTS recommended) aapke system par install hona chahiye.
2. **Firebase CLI** install karein:
   ```bash
   npm install -g firebase-tools
   ```
3. Firebase account mein login karein:
   ```bash
   firebase login
   ```

---

## 🚀 Step-by-Step Deployment Instructions

### Step 1: Firebase Project Select Karein
Root folder mein `.firebaserc` file mein apna project ID likhein, ya command run karein:
```bash
firebase use --add
```
*(Apna Firebase Project ID select karein).*

### Step 2: Gemini API Key Configure Karein
Firebase Secrets Manager use karke Gemini API key securely store karein:
```bash
firebase functions:secrets:set GEMINI_API_KEY
```
*(Prompts aane par Google AI Studio se hasil shuda Gemini API key paste karein).*

Alternately, `functions/.env` file bana kar us mein likhein:
```env
GEMINI_API_KEY=AIzaSy...your_gemini_api_key...
```

### Step 3: Functions Folder mein Dependencies Install Karein
```bash
cd functions
npm install
cd ..
```

### Step 4: Deploy Cloud Functions
Root folder se command run karein:
```bash
firebase deploy --only functions
```

Deployment complete hone ke baad Firebase terminal mein function ka HTTPS URL dega:
```
✔  functions[coachMaya(us-central1)]: Successful create operation.
Function URL (coachMaya(us-central1)): https://us-central1-your-project-id.cloudfunctions.net/coachMaya
```

---

## 📱 Step 5: Android App Connect Karein

Ab jo Function URL aapko mila hai, use WomanFit Android app mein set karein:

1. Project ke `.env` file mein:
   ```env
   BACKEND_PROXY_URL=https://us-central1-your-project-id.cloudfunctions.net/coachMaya
   BACKEND_PROXY_KEY=DEFAULT_EMPTY
   ```
2. Android build regenerate karein:
   ```bash
   gradle bundleRelease
   ```
3. Ab Coach Maya real Google Gemini 2.5 Flash model se connect ho kar live, personalized answers provide karegi!

---

## 🛡️ Testing & Health Check

Function deploy hone ke baad browser mein URL open karke test karein:
`https://us-central1-your-project-id.cloudfunctions.net/coachMaya`

Aapko yeh JSON response milega:
```json
{
  "status": "healthy",
  "service": "womanfit-cloud-functions",
  "version": "1.2.4",
  "geminiConfigured": true,
  "timestamp": 1741080000000
}
```
Agar internet band ho ya service down ho, to Android app foran built-in **Offline Wellness Engine** par switch ho jati hai!
