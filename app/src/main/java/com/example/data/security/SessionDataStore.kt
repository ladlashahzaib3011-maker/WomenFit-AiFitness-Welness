package com.example.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Authenticated Supabase session snapshot persisted in Jetpack DataStore.
 */
data class AuthSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long
) {
    fun isExpired(nowMillis: Long = System.currentTimeMillis(), bufferMillis: Long = 30_000L): Boolean {
        return expiresAtMillis <= 0L || nowMillis >= (expiresAtMillis - bufferMillis)
    }
}

val Context.sessionPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "womanfit_auth_session")

/**
 * Jetpack DataStore-backed session storage for WomanFit.
 *
 * Stores the authenticated user ID, email, full name, Supabase access token,
 * refresh token, and token expiration timestamp across app restarts.
 */
class SessionDataStore(private val context: Context) {

    companion object {
        val KEY_USER_ID = stringPreferencesKey("authenticated_user_id")
        val KEY_USER_EMAIL = stringPreferencesKey("authenticated_user_email")
        val KEY_USER_NAME = stringPreferencesKey("authenticated_user_name")
        val KEY_ACCESS_TOKEN = stringPreferencesKey("supabase_access_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("supabase_refresh_token")
        val KEY_EXPIRES_AT = longPreferencesKey("supabase_expires_at_millis")
    }

    val activeUserId: Flow<String?> = context.sessionPreferencesDataStore.data.map { preferences ->
        val uid = preferences[KEY_USER_ID]
        val token = preferences[KEY_ACCESS_TOKEN]
        if (!uid.isNullOrBlank() && !token.isNullOrBlank()) uid else null
    }

    suspend fun getSession(): AuthSession? {
        val prefs = context.sessionPreferencesDataStore.data.firstOrNull() ?: return null
        val userId = prefs[KEY_USER_ID]?.takeIf { it.isNotBlank() } ?: return null
        val accessToken = prefs[KEY_ACCESS_TOKEN]?.takeIf { it.isNotBlank() } ?: return null
        val refreshToken = prefs[KEY_REFRESH_TOKEN].orEmpty()
        val email = prefs[KEY_USER_EMAIL].orEmpty()
        val fullName = prefs[KEY_USER_NAME].orEmpty()
        val expiresAt = prefs[KEY_EXPIRES_AT] ?: 0L

        return AuthSession(
            userId = userId,
            email = email,
            fullName = fullName,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = expiresAt
        )
    }

    suspend fun getActiveUserId(): String? {
        return getSession()?.userId
    }

    suspend fun saveSession(session: AuthSession) {
        context.sessionPreferencesDataStore.edit { preferences ->
            preferences[KEY_USER_ID] = session.userId
            preferences[KEY_USER_EMAIL] = session.email
            preferences[KEY_USER_NAME] = session.fullName
            preferences[KEY_ACCESS_TOKEN] = session.accessToken
            preferences[KEY_REFRESH_TOKEN] = session.refreshToken
            preferences[KEY_EXPIRES_AT] = session.expiresAtMillis
        }
    }

    suspend fun clearSession() {
        context.sessionPreferencesDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
