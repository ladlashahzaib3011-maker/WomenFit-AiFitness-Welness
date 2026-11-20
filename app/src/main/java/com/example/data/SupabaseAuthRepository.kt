package com.example.data

import com.example.data.local.UserDao
import com.example.data.remote.*
import com.example.data.security.AuthSession
import com.example.data.security.SessionDataStore
import com.example.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Production Supabase Authentication & PostgreSQL Profile Repository.
 *
 * - Authenticates strictly via Supabase Auth GoTrue REST API (/auth/v1).
 * - Syncs user profiles with Supabase PostgreSQL (/rest/v1/profiles) and caches locally in Room.
 * - Persists access tokens, refresh tokens, and expiration timestamps in Jetpack DataStore.
 * - Never stores plain-text passwords, never logs secrets/passwords, and never allows demo/fake login.
 */
class SupabaseAuthRepository(
    private val sessionDataStore: SessionDataStore,
    private val userDao: UserDao,
    private val apiProvider: () -> SupabaseAuthApi? = {
        val url = SupabaseConfig.getBaseUrl()
        val key = SupabaseConfig.getAnonKey()
        if (SupabaseConfig.isConfigured(url, key)) SupabaseClientProvider.createApi(url) else null
    },
    private val anonKeyProvider: () -> String = { SupabaseConfig.getAnonKey() },
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() }
) : AuthRepository {

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        const val MIN_PASSWORD_LENGTH = 6
    }

    private val _activeUserId = MutableStateFlow<String?>(null)
    override val activeUserId: StateFlow<String?> = _activeUserId.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override suspend fun restoreAndVerifySession(): SessionState {
        _sessionState.value = SessionState.Loading
        val storedSession = sessionDataStore.getSession()
        if (storedSession == null) {
            _activeUserId.value = null
            _sessionState.value = SessionState.LoggedOut
            return SessionState.LoggedOut
        }

        val api = apiProvider()
        val anonKey = anonKeyProvider()
        if (api == null || anonKey.isBlank() || anonKey == "DEFAULT_EMPTY") {
            // Backend not configured: cannot verify session token, so require sign in
            sessionDataStore.clearSession()
            _activeUserId.value = null
            _sessionState.value = SessionState.LoggedOut
            return SessionState.LoggedOut
        }

        val now = nowMillisProvider()
        if (storedSession.isExpired(now)) {
            return attemptTokenRefresh(api, anonKey, storedSession)
        }

        // Token is not expired; verify it with Supabase /auth/v1/user
        return try {
            val response = api.getCurrentUser(
                apiKey = anonKey,
                authorization = "Bearer ${storedSession.accessToken}"
            )
            when {
                response.isSuccessful && response.body() != null -> {
                    val user = response.body()!!
                    ensureLocalProfileExists(
                        userId = user.id,
                        email = user.email ?: storedSession.email,
                        fullName = extractFullName(user.userMetadata, storedSession.fullName, user.email)
                    )
                    _activeUserId.value = user.id
                    val state = SessionState.LoggedIn(user.id)
                    _sessionState.value = state
                    state
                }
                response.code() == 401 || response.code() == 403 -> {
                    // Token rejected/revoked; try refresh token once
                    attemptTokenRefresh(api, anonKey, storedSession)
                }
                else -> {
                    // Unrecognized server error on verification: fail closed
                    sessionDataStore.clearSession()
                    _activeUserId.value = null
                    _sessionState.value = SessionState.LoggedOut
                    SessionState.LoggedOut
                }
            }
        } catch (_: IOException) {
            // Network unavailable while token is still strictly within its valid expiration window
            _activeUserId.value = storedSession.userId
            val state = SessionState.LoggedIn(storedSession.userId)
            _sessionState.value = state
            state
        } catch (_: Exception) {
            sessionDataStore.clearSession()
            _activeUserId.value = null
            _sessionState.value = SessionState.LoggedOut
            SessionState.LoggedOut
        }
    }

    private suspend fun attemptTokenRefresh(
        api: SupabaseAuthApi,
        anonKey: String,
        storedSession: AuthSession
    ): SessionState {
        if (storedSession.refreshToken.isBlank()) {
            sessionDataStore.clearSession()
            _activeUserId.value = null
            _sessionState.value = SessionState.LoggedOut
            return SessionState.LoggedOut
        }

        return try {
            val refreshResponse = api.refreshSession(
                apiKey = anonKey,
                request = SupabaseRefreshTokenRequest(refreshToken = storedSession.refreshToken)
            )
            val body = refreshResponse.body()
            if (refreshResponse.isSuccessful && body != null && !body.accessToken.isNullOrBlank() && body.user != null) {
                val updatedSession = buildSessionFromAuthResponse(body, storedSession.fullName)
                sessionDataStore.saveSession(updatedSession)
                ensureLocalProfileExists(updatedSession.userId, updatedSession.email, updatedSession.fullName)
                _activeUserId.value = updatedSession.userId
                val state = SessionState.LoggedIn(updatedSession.userId)
                _sessionState.value = state
                state
            } else {
                sessionDataStore.clearSession()
                _activeUserId.value = null
                _sessionState.value = SessionState.LoggedOut
                SessionState.LoggedOut
            }
        } catch (_: Exception) {
            sessionDataStore.clearSession()
            _activeUserId.value = null
            _sessionState.value = SessionState.LoggedOut
            SessionState.LoggedOut
        }
    }

    override suspend fun signIn(
        emailInput: String,
        passwordInput: String
    ): AuthResult {
        val email = emailInput.trim().lowercase()
        val password = passwordInput

        if (email.isBlank() || !EMAIL_REGEX.matches(email)) {
            return AuthResult.Error("Please enter a valid email address.", AuthField.EMAIL)
        }
        if (password.isBlank()) {
            return AuthResult.Error("Password is required.", AuthField.PASSWORD)
        }

        val api = apiProvider()
        val anonKey = anonKeyProvider()
        if (api == null || anonKey.isBlank() || anonKey == "DEFAULT_EMPTY") {
            return AuthResult.Error(
                "Backend authentication is not configured yet. Configure SUPABASE_URL + SUPABASE_ANON_KEY or BACKEND_PROXY_URL in Secrets to sign in.",
                AuthField.GENERAL
            )
        }

        return try {
            val response = api.signInWithPassword(
                apiKey = anonKey,
                request = SupabaseSignInRequest(email = email, password = password)
            )

            if (response.isSuccessful) {
                val body = response.body()
                val user = body?.user
                val accessToken = body?.accessToken
                if (body == null || user == null || accessToken.isNullOrBlank()) {
                    return AuthResult.Error(
                        "Invalid authentication response from server. Please try again.",
                        AuthField.GENERAL
                    )
                }

                val session = buildSessionFromAuthResponse(body, fallbackName = "")
                sessionDataStore.saveSession(session)

                // Sync profile from Supabase PostgreSQL & cache locally
                syncProfileOnSignIn(api, anonKey, session)

                _activeUserId.value = session.userId
                _sessionState.value = SessionState.LoggedIn(session.userId)

                AuthResult.Success(
                    userId = session.userId,
                    email = session.email,
                    fullName = session.fullName
                )
            } else {
                val rawError = response.errorBody()?.string()
                val parsed = SupabaseClientProvider.parseErrorMessage(rawError)
                val friendlyMessage = mapSupabaseError(response.code(), parsed, isSignUp = false)
                AuthResult.Error(friendlyMessage, AuthField.GENERAL)
            }
        } catch (_: IOException) {
            AuthResult.Error(
                "Unable to connect to authentication server. Please check your internet connection.",
                AuthField.GENERAL
            )
        } catch (_: Exception) {
            AuthResult.Error(
                "Sign in failed unexpectedly. Please try again.",
                AuthField.GENERAL
            )
        }
    }

    override suspend fun signUp(
        fullNameInput: String,
        emailInput: String,
        passwordInput: String,
        confirmPasswordInput: String
    ): AuthResult {
        val fullName = fullNameInput.trim()
        val email = emailInput.trim().lowercase()
        val password = passwordInput
        val confirmPassword = confirmPasswordInput

        if (fullName.length < 2) {
            return AuthResult.Error("Please enter your full name.", AuthField.FULL_NAME)
        }
        if (email.isBlank() || !EMAIL_REGEX.matches(email)) {
            return AuthResult.Error("Please enter a valid email address.", AuthField.EMAIL)
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return AuthResult.Error("Password must be at least 6 characters.", AuthField.PASSWORD)
        }
        if (password != confirmPassword) {
            return AuthResult.Error("Passwords do not match.", AuthField.CONFIRM_PASSWORD)
        }

        val api = apiProvider()
        val anonKey = anonKeyProvider()
        if (api == null || anonKey.isBlank() || anonKey == "DEFAULT_EMPTY") {
            return AuthResult.Error(
                "Backend authentication is not configured yet. Configure SUPABASE_URL + SUPABASE_ANON_KEY or BACKEND_PROXY_URL in Secrets to create an account.",
                AuthField.GENERAL
            )
        }

        return try {
            val response = api.signUp(
                apiKey = anonKey,
                request = SupabaseSignUpRequest(
                    email = email,
                    password = password,
                    data = SupabaseSignUpMetadata(fullName = fullName)
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                val user = body?.user
                val accessToken = body?.accessToken

                if (user != null && !accessToken.isNullOrBlank()) {
                    // Immediate active session returned by Supabase
                    val session = buildSessionFromAuthResponse(body, fallbackName = fullName)
                    sessionDataStore.saveSession(session)

                    // Create profile in Supabase PostgreSQL & local cache
                    upsertRemoteAndLocalProfile(api, anonKey, session)

                    _activeUserId.value = session.userId
                    _sessionState.value = SessionState.LoggedIn(session.userId)

                    AuthResult.Success(
                        userId = session.userId,
                        email = session.email,
                        fullName = session.fullName
                    )
                } else if (user != null) {
                    // Email confirmation required by Supabase project configuration
                    AuthResult.EmailVerificationRequired(
                        email = email,
                        message = "Account created! Please verify your email address ($email) via the confirmation link sent to your inbox, then sign in."
                    )
                } else {
                    AuthResult.Error(
                        "Unable to complete registration. Please try again.",
                        AuthField.GENERAL
                    )
                }
            } else {
                val rawError = response.errorBody()?.string()
                val parsed = SupabaseClientProvider.parseErrorMessage(rawError)
                val friendlyMessage = mapSupabaseError(response.code(), parsed, isSignUp = true)
                val field = if (friendlyMessage.contains("email", ignoreCase = true) ||
                    friendlyMessage.contains("exists", ignoreCase = true)
                ) {
                    AuthField.EMAIL
                } else {
                    AuthField.GENERAL
                }
                AuthResult.Error(friendlyMessage, field)
            }
        } catch (_: IOException) {
            AuthResult.Error(
                "Unable to connect to registration server. Please check your internet connection.",
                AuthField.GENERAL
            )
        } catch (_: Exception) {
            AuthResult.Error(
                "Registration failed unexpectedly. Please try again.",
                AuthField.GENERAL
            )
        }
    }

    override suspend fun requestPasswordReset(emailInput: String): ResetRequestResult {
        val email = emailInput.trim().lowercase()
        if (email.isBlank() || !EMAIL_REGEX.matches(email)) {
            return ResetRequestResult.Error("Please enter a valid email address.")
        }

        val api = apiProvider()
        val anonKey = anonKeyProvider()
        if (api == null || anonKey.isBlank() || anonKey == "DEFAULT_EMPTY") {
            return ResetRequestResult.Error(
                "Backend authentication is not configured yet. Configure SUPABASE_URL + SUPABASE_ANON_KEY or BACKEND_PROXY_URL in Secrets first."
            )
        }

        return try {
            val response = api.sendPasswordReset(
                apiKey = anonKey,
                request = SupabaseRecoverRequest(email = email)
            )
            if (response.isSuccessful) {
                ResetRequestResult.Success(
                    "Password reset instructions have been sent to $email. Please check your inbox."
                )
            } else {
                val parsed = SupabaseClientProvider.parseErrorMessage(response.errorBody()?.string())
                ResetRequestResult.Error(parsed ?: "Unable to send password reset email. Please try again.")
            }
        } catch (_: IOException) {
            ResetRequestResult.Error("Network error. Please check your internet connection.")
        } catch (_: Exception) {
            ResetRequestResult.Error("Failed to request password reset. Please try again.")
        }
    }

    override suspend fun syncProfileUpdate(userProfile: UserProfile) {
        val session = sessionDataStore.getSession() ?: return
        if (session.userId != userProfile.id) return
        val api = apiProvider() ?: return
        val anonKey = anonKeyProvider()
        if (anonKey.isBlank() || anonKey == "DEFAULT_EMPTY") return

        try {
            api.upsertUserProfile(
                apiKey = anonKey,
                authorization = "Bearer ${session.accessToken}",
                profile = SupabaseProfileDto(
                    id = session.userId,
                    fullName = userProfile.name.ifBlank { session.fullName },
                    email = userProfile.email.ifBlank { session.email },
                    fitnessGoal = userProfile.fitnessGoal,
                    fitnessLevel = userProfile.fitnessLevel,
                    age = userProfile.age,
                    heightCm = userProfile.heightCm,
                    currentWeightKg = userProfile.currentWeightKg,
                    targetWeightKg = userProfile.targetWeightKg,
                    dailyWaterGoalMl = userProfile.dailyWaterGoalMl,
                    cycleLengthDays = userProfile.cycleLengthDays,
                    lastPeriodStartTimestamp = userProfile.lastPeriodStartTimestamp
                )
            )
        } catch (_: Exception) {
            // Keep local Room state authoritative offline until backend sync succeeds
        }
    }

    override suspend fun signOut() {
        val session = sessionDataStore.getSession()
        val api = apiProvider()
        val anonKey = anonKeyProvider()

        if (session != null && api != null && anonKey.isNotBlank() && anonKey != "DEFAULT_EMPTY") {
            try {
                api.signOut(
                    apiKey = anonKey,
                    authorization = "Bearer ${session.accessToken}"
                )
            } catch (_: Exception) {
                // Local session teardown must always succeed even if network call fails
            }
        }

        sessionDataStore.clearSession()
        _activeUserId.value = null
        _sessionState.value = SessionState.LoggedOut
    }

    private fun buildSessionFromAuthResponse(
        body: SupabaseAuthResponse,
        fallbackName: String
    ): AuthSession {
        val user = body.user!!
        val now = nowMillisProvider()
        val expiresAtMillis = when {
            body.expiresAt != null && body.expiresAt > 0L -> body.expiresAt * 1000L
            body.expiresIn != null && body.expiresIn > 0L -> now + (body.expiresIn * 1000L)
            else -> now + (3600L * 1000L)
        }
        val email = user.email.orEmpty()
        val fullName = extractFullName(user.userMetadata, fallbackName, email)

        return AuthSession(
            userId = user.id,
            email = email,
            fullName = fullName,
            accessToken = body.accessToken.orEmpty(),
            refreshToken = body.refreshToken.orEmpty(),
            expiresAtMillis = expiresAtMillis
        )
    }

    private fun extractFullName(
        metadata: Map<String, Any?>?,
        fallbackName: String,
        email: String?
    ): String {
        val metaName = (metadata?.get("full_name") as? String)?.trim()
        if (!metaName.isNullOrBlank()) return metaName
        if (fallbackName.isNotBlank()) return fallbackName.trim()
        val emailPrefix = email?.substringBefore("@")?.replace(".", " ")?.trim()
        return if (!emailPrefix.isNullOrBlank()) {
            emailPrefix.replaceFirstChar { it.uppercase() }
        } else {
            "WomanFit Member"
        }
    }

    private suspend fun syncProfileOnSignIn(
        api: SupabaseAuthApi,
        anonKey: String,
        session: AuthSession
    ) {
        try {
            val response = api.getUserProfile(
                apiKey = anonKey,
                authorization = "Bearer ${session.accessToken}",
                idFilter = "eq.${session.userId}"
            )
            val remoteProfile = response.body()?.firstOrNull()
            if (response.isSuccessful && remoteProfile != null) {
                val existing = userDao.getUserDirectById(session.userId)
                userDao.insertOrUpdate(
                    UserProfile(
                        id = session.userId,
                        name = remoteProfile.fullName.ifBlank { session.fullName },
                        email = remoteProfile.email.ifBlank { session.email },
                        fitnessGoal = remoteProfile.fitnessGoal,
                        fitnessLevel = remoteProfile.fitnessLevel,
                        age = remoteProfile.age,
                        heightCm = remoteProfile.heightCm,
                        currentWeightKg = remoteProfile.currentWeightKg,
                        targetWeightKg = remoteProfile.targetWeightKg,
                        dailyWaterGoalMl = remoteProfile.dailyWaterGoalMl,
                        cycleLengthDays = remoteProfile.cycleLengthDays,
                        lastPeriodStartTimestamp = if (remoteProfile.lastPeriodStartTimestamp > 0L) {
                            remoteProfile.lastPeriodStartTimestamp
                        } else {
                            existing?.lastPeriodStartTimestamp ?: 0L
                        }
                    )
                )
                return
            }
        } catch (_: Exception) {
            // Fall through to ensure local profile exists
        }
        ensureLocalProfileExists(session.userId, session.email, session.fullName)
    }

    private suspend fun upsertRemoteAndLocalProfile(
        api: SupabaseAuthApi,
        anonKey: String,
        session: AuthSession
    ) {
        try {
            api.upsertUserProfile(
                apiKey = anonKey,
                authorization = "Bearer ${session.accessToken}",
                profile = SupabaseProfileDto(
                    id = session.userId,
                    fullName = session.fullName,
                    email = session.email
                )
            )
        } catch (_: Exception) {
            // Profile table may not have RLS/table created yet; keep local cache synced
        }
        ensureLocalProfileExists(session.userId, session.email, session.fullName)
    }

    private suspend fun ensureLocalProfileExists(
        userId: String,
        email: String,
        fullName: String
    ) {
        val existing = userDao.getUserDirectById(userId)
        if (existing == null) {
            userDao.insertOrUpdate(
                UserProfile(
                    id = userId,
                    name = fullName.ifBlank { "WomanFit Member" },
                    email = email,
                    lastPeriodStartTimestamp = 0L
                )
            )
        }
    }

    private fun mapSupabaseError(code: Int, rawMessage: String?, isSignUp: Boolean): String {
        val msg = rawMessage.orEmpty()
        return when {
            msg.contains("Invalid login credentials", ignoreCase = true) ->
                "Incorrect email or password. Please check your credentials and try again."
            msg.contains("Email not confirmed", ignoreCase = true) ->
                "Please verify your email address before signing in."
            msg.contains("already registered", ignoreCase = true) ||
                msg.contains("already exists", ignoreCase = true) ->
                "An account with this email already exists. Please sign in instead."
            msg.contains("Password should be at least", ignoreCase = true) ->
                "Password must be at least 6 characters."
            msg.contains("Unable to validate email", ignoreCase = true) ->
                "Please enter a valid email address."
            code == 429 ->
                "Too many attempts. Please wait a moment and try again."
            msg.isNotBlank() -> msg
            isSignUp -> "Could not create account (error $code). Please try again."
            else -> "Could not sign in (error $code). Please verify your credentials."
        }
    }
}
