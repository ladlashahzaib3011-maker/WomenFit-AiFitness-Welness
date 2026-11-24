package com.example.data.remote

import android.util.Log
import com.example.data.CloudConnection
import com.example.data.CloudStatus
import com.example.data.security.AuthSession
import com.example.data.security.SessionDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Direct Supabase PostgREST client for this app's own "womenfit_ai_*" tables.
 *
 * Built the same way as the original WomenFit app's SupabaseClient: plain OkHttp + org.json
 * against the same self-hosted server (SUPABASE_URL / SUPABASE_ANON_KEY), sending
 * `apikey: <anon key>` and `Authorization: Bearer <user access token>` on every request.
 * Tokens are refreshed proactively when about to expire and once more on a 401, then the
 * request is retried. Row Level Security on the server limits every query to the signed-in user.
 */
class WomenFitAiCloudClient(
    private val sessionDataStore: SessionDataStore,
    private val baseUrlProvider: () -> String = { SupabaseConfig.getBaseUrl() },
    private val anonKeyProvider: () -> String = { SupabaseConfig.getAnonKey() },
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "WomenFitAiCloud"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        // Table names on the shared Supabase server. Prefixed so they never collide with
        // the original WomenFit app's tables (profiles, workout_logs, water_logs, ...).
        const val TABLE_USERS = "womenfit_ai_users"
        const val TABLE_DAILY_PROGRESS = "womenfit_ai_daily_progress"
        const val TABLE_WORKOUT_LOGS = "womenfit_ai_workout_logs"
        const val TABLE_WEIGHT_LOGS = "womenfit_ai_weight_logs"
        const val TABLE_MEAL_LOGS = "womenfit_ai_meal_logs"
        const val TABLE_DAILY_GOALS = "womenfit_ai_daily_goals"
    }

    private val refreshMutex = Mutex()

    private val baseUrl: String get() = baseUrlProvider().trim().trimEnd('/')
    private val anonKey: String get() = anonKeyProvider().trim()

    fun isConfigured(): Boolean = SupabaseConfig.isConfigured(baseUrl, anonKey)

    // -------------------------------------------------------------
    // Table operations
    // -------------------------------------------------------------

    /** Bulk insert-or-update rows (POST + Prefer: resolution=merge-duplicates). */
    suspend fun upsertRows(table: String, rows: JSONArray, onConflict: String = "id"): Boolean {
        if (rows.length() == 0) return true
        val response = executeAuthenticated { token ->
            Request.Builder()
                .url("$baseUrl/rest/v1/$table?on_conflict=${encode(onConflict)}")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(rows.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        } ?: return false
        return response.useAndCheck("upsert $table")
    }

    /** Reads all of the user's rows from a table. Returns null on failure. */
    suspend fun fetchRows(table: String, userId: String, order: String? = null): JSONArray? {
        val orderParam = if (order != null) "&order=${encode(order)}" else ""
        val response = executeAuthenticated { token ->
            Request.Builder()
                .url("$baseUrl/rest/v1/$table?user_id=eq.${encode(userId)}&select=*$orderParam")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
        } ?: return null
        return response.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                Log.w(TAG, "fetch $table failed (${it.code}): ${body.take(200)}")
                null
            } else {
                try {
                    JSONArray(body)
                } catch (e: Exception) {
                    Log.w(TAG, "fetch $table returned unexpected JSON", e)
                    null
                }
            }
        }
    }

    /** PATCH a single row by its uuid. */
    suspend fun updateRow(table: String, id: String, fields: JSONObject): Boolean {
        val response = executeAuthenticated { token ->
            Request.Builder()
                .url("$baseUrl/rest/v1/$table?id=eq.${encode(id)}")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Prefer", "return=minimal")
                .patch(fields.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        } ?: return false
        return response.useAndCheck("update $table")
    }

    /** DELETE a single row by its uuid. */
    suspend fun deleteRow(table: String, id: String): Boolean {
        val response = executeAuthenticated { token ->
            Request.Builder()
                .url("$baseUrl/rest/v1/$table?id=eq.${encode(id)}")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .delete()
                .build()
        } ?: return false
        return response.useAndCheck("delete $table")
    }

    // -------------------------------------------------------------
    // Authenticated execution with token refresh (same flow as WomenFit's SupabaseClient)
    // -------------------------------------------------------------

    /**
     * 1. Refreshes the access token first if it is about to expire.
     * 2. On 401, refreshes once and retries the request.
     * Returns null when not signed in, not configured, or the network call throws.
     */
    private suspend fun executeAuthenticated(buildRequest: (token: String) -> Request): Response? =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) return@withContext null
            val session = sessionDataStore.getSession() ?: return@withContext null
            try {
                val token = if (session.isExpired()) {
                    refreshToken(session) ?: session.accessToken
                } else {
                    session.accessToken
                }
                var response = httpClient.newCall(buildRequest(token)).execute()
                if (response.code == 401) {
                    response.close()
                    val latest = sessionDataStore.getSession() ?: return@withContext null
                    val newToken = refreshToken(latest) ?: return@withContext null
                    response = httpClient.newCall(buildRequest(newToken)).execute()
                }
                // The server answered, so the cloud is reachable.
                CloudStatus.setConnection(CloudConnection.ONLINE)
                response
            } catch (e: Exception) {
                Log.w(TAG, "network error: ${e.javaClass.simpleName}: ${e.message}")
                CloudStatus.setConnection(CloudConnection.OFFLINE)
                null
            }
        }

    /**
     * Pings GET /auth/v1/health (no login needed) and updates [CloudStatus.connection].
     * Used by the "Cloud sync active" badge.
     */
    suspend fun checkConnection(): CloudConnection = withContext(Dispatchers.IO) {
        val state = if (!isConfigured()) {
            CloudConnection.NOT_CONFIGURED
        } else {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/auth/v1/health")
                    .addHeader("apikey", anonKey)
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) CloudConnection.ONLINE else CloudConnection.OFFLINE
                }
            } catch (e: Exception) {
                Log.w(TAG, "health check failed: ${e.javaClass.simpleName}")
                CloudConnection.OFFLINE
            }
        }
        CloudStatus.setConnection(state)
        state
    }

    /**
     * POST /auth/v1/token?grant_type=refresh_token. Saves the new tokens into the same
     * DataStore session the auth screens use, so the whole app stays signed in.
     */
    private suspend fun refreshToken(session: AuthSession): String? = refreshMutex.withLock {
        // Another coroutine may have refreshed while we waited for the lock.
        val current = sessionDataStore.getSession() ?: return@withLock null
        if (current.accessToken != session.accessToken && !current.isExpired()) {
            return@withLock current.accessToken
        }
        if (current.refreshToken.isBlank()) return@withLock null

        try {
            val body = JSONObject().put("refresh_token", current.refreshToken)
            val request = Request.Builder()
                .url("$baseUrl/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "token refresh failed (${response.code})")
                    return@withLock null
                }
                val json = JSONObject(text)
                val accessToken = json.optString("access_token")
                if (accessToken.isBlank()) return@withLock null
                val now = System.currentTimeMillis()
                val expiresAt = json.optLong("expires_at", 0L)
                val expiresIn = json.optLong("expires_in", 3600L)
                val expiresAtMillis = if (expiresAt > 0L) expiresAt * 1000L else now + expiresIn * 1000L
                sessionDataStore.saveSession(
                    current.copy(
                        accessToken = accessToken,
                        refreshToken = json.optString("refresh_token", current.refreshToken)
                            .ifBlank { current.refreshToken },
                        expiresAtMillis = expiresAtMillis
                    )
                )
                accessToken
            }
        } catch (e: Exception) {
            Log.w(TAG, "token refresh error: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun Response.useAndCheck(label: String): Boolean = use {
        if (!it.isSuccessful) {
            Log.w(TAG, "$label failed (${it.code}): ${it.body?.string().orEmpty().take(200)}")
        }
        it.isSuccessful
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
