/**
 * 🌸 WomanFit — Firebase Cloud Functions v2
 * Serverless AI Proxy for Coach Maya (Google Gemini 2.5 Flash)
 *
 * Security Architecture:
 * - Keeps GEMINI_API_KEY 100% server-side in Firebase (0% APK exposure).
 * - Verifies Firebase App Check and Firebase Auth tokens.
 * - Enforces certified female fitness and menstrual cycle wellness guardrails.
 */

import { onRequest } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import admin from "firebase-admin";

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp();
}

/**
 * Core Coach Maya Handler function
 */
async function handleCoachMayaRequest(req, res) {
  // Set CORS headers
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Firebase-AppCheck, X-WomanFit-App-Key");

  // Handle preflight OPTIONS request
  if (req.method === "OPTIONS") {
    return res.status(204).send("");
  }

  // Health-check GET request
  if (req.method === "GET") {
    const isConfigured = Boolean(process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY !== "your_gemini_api_key_here");
    return res.status(200).json({
      status: "healthy",
      service: "womanfit-cloud-functions",
      version: "1.2.4",
      geminiConfigured: isConfigured,
      timestamp: Date.now()
    });
  }

  if (req.method !== "POST") {
    return res.status(405).json({
      success: false,
      error: "Method not allowed. Use POST to ask Coach Maya."
    });
  }

  try {
    const authHeader = req.headers["authorization"];
    const appCheckHeader = req.headers["x-firebase-appcheck"];
    const clientSecretHeader = req.headers["x-womanfit-app-key"];

    const requireAppCheck = process.env.REQUIRE_APP_CHECK === "true";
    const requireAuth = process.env.REQUIRE_FIREBASE_AUTH === "true";
    const expectedSecret = process.env.APP_CLIENT_SECRET || "";

    // 1. Verify Firebase App Check (if required or provided)
    if (requireAppCheck || appCheckHeader) {
      if (!appCheckHeader) {
        return res.status(401).json({
          success: false,
          error: "Unauthorized: Missing X-Firebase-AppCheck token."
        });
      }
      try {
        await admin.appCheck().verifyToken(appCheckHeader);
      } catch (err) {
        logger.warn("[Cloud Function] App Check verification failed:", err.message);
        if (requireAppCheck) {
          return res.status(401).json({
            success: false,
            error: "Unauthorized: Invalid or expired Firebase App Check token."
          });
        }
      }
    }

    // 2. Verify Firebase Auth ID Token (if required or provided)
    if (requireAuth || (authHeader && authHeader.startsWith("Bearer "))) {
      if (!authHeader || !authHeader.startsWith("Bearer ")) {
        return res.status(401).json({
          success: false,
          error: "Unauthorized: Missing or invalid Authorization Bearer token."
        });
      }
      const idToken = authHeader.split("Bearer ")[1].trim();
      try {
        await admin.auth().verifyIdToken(idToken);
      } catch (err) {
        logger.warn("[Cloud Function] Auth token verification failed:", err.message);
        if (requireAuth) {
          return res.status(401).json({
            success: false,
            error: "Unauthorized: Invalid or expired Firebase ID token."
          });
        }
      }
    }

    // 3. Verify App Client Secret (if configured)
    if (expectedSecret && expectedSecret.trim().length > 0) {
      const isMatch = clientSecretHeader === expectedSecret || authHeader === expectedSecret;
      if (!isMatch && !requireAuth) {
        return res.status(401).json({
          success: false,
          error: "Unauthorized: Invalid WomanFit client key."
        });
      }
    }

    // 4. Validate & Sanitize Input
    const { question, userGoal, fitnessLevel, currentCyclePhase } = req.body || {};

    if (!question || typeof question !== "string" || question.trim().length < 2) {
      return res.status(400).json({
        success: false,
        error: "Question is required and must be at least 2 characters long."
      });
    }

    if (question.length > 1000) {
      return res.status(400).json({
        success: false,
        error: "Question exceeds maximum allowed limit of 1000 characters."
      });
    }

    const sanitizedQuestion = question.trim();
    const sanitizedGoal = typeof userGoal === "string" && userGoal.trim().length > 0
      ? userGoal.trim().slice(0, 100)
      : "Overall Health, Strength & Toning";
    const sanitizedLevel = typeof fitnessLevel === "string" && fitnessLevel.trim().length > 0
      ? fitnessLevel.trim().slice(0, 50)
      : "Intermediate";
    const sanitizedCycle = typeof currentCyclePhase === "string" && currentCyclePhase.trim().length > 0
      ? currentCyclePhase.trim().slice(0, 50)
      : null;

    // 5. Verify Server-Side Gemini API Key
    const geminiApiKey = process.env.GEMINI_API_KEY;
    if (!geminiApiKey || geminiApiKey === "your_gemini_api_key_here") {
      logger.error("[Cloud Function] GEMINI_API_KEY is not configured in Firebase environment/secrets.");
      return res.status(503).json({
        success: false,
        error: "Server-side Gemini AI key is not configured. Falling back to client offline engine."
      });
    }

    // 6. Coach Maya Persona & Women's Health System Instructions
    const systemInstruction = `You are Coach Maya, an expert certified female fitness trainer, sports nutritionist, and holistic wellness specialist in the WomanFit mobile app.
Your tone is warm, inspiring, science-grounded, empathetic, and tailored specifically to female physiology, hormone balance, pelvic floor health, cycle-syncing, progressive resistance, and posture.

User Profile Context:
- Primary Goal: ${sanitizedGoal}
- Fitness Level: ${sanitizedLevel}
${sanitizedCycle ? `- Current Menstrual Cycle Phase: ${sanitizedCycle}` : ""}

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

    // Use Gemini 2.5 Flash for fast, intelligent, cost-efficient inference
    const upstreamUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${encodeURIComponent(geminiApiKey)}`;

    const response = await fetch(upstreamUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(geminiPayload)
    });

    if (!response.ok) {
      const errText = await response.text();
      logger.error(`[Cloud Function] Upstream Gemini error (${response.status}):`, errText);
      return res.status(response.status >= 500 ? 502 : 400).json({
        success: false,
        error: `AI Service temporarily unavailable (${response.status}). Please try again shortly.`
      });
    }

    const data = await response.json();
    const candidate = data?.candidates?.[0];
    const replyText = candidate?.content?.parts?.[0]?.text;

    if (!replyText || replyText.trim().length === 0) {
      return res.status(502).json({
        success: false,
        error: "Empty response received from AI model."
      });
    }

    // Success response to WomanFit Android client
    return res.status(200).json({
      success: true,
      reply: replyText.trim(),
      model: "gemini-2.5-flash",
      timestamp: Date.now()
    });

  } catch (err) {
    logger.error("[Cloud Function] Unhandled exception:", err);
    return res.status(500).json({
      success: false,
      error: "Internal server error while communicating with Coach Maya."
    });
  }
}

/**
 * Export Firebase HTTPS Cloud Functions
 *
 * URL: https://<region>-<project-id>.cloudfunctions.net/coachMaya
 */
export const coachMaya = onRequest(
  {
    cors: true,
    maxInstances: 20,
    timeoutSeconds: 30,
    memory: "256MiB"
  },
  handleCoachMayaRequest
);

// Secondary alias for backward compatibility or routing preferences
export const api = onRequest(
  {
    cors: true,
    maxInstances: 20,
    timeoutSeconds: 30,
    memory: "256MiB"
  },
  handleCoachMayaRequest
);
