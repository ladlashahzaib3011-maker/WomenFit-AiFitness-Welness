package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.data.local.AppDatabase
import com.example.data.remote.*
import com.example.data.security.AuthSession
import com.example.data.security.SessionDataStore
import com.example.ui.AuthViewModel
import com.example.ui.FitnessViewModel
import com.example.ui.theme.WomanFitTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

/**
 * Fake Supabase GoTrue + PostgREST API for deterministic Robolectric testing.
 * Simulates real Supabase HTTP responses including valid tokens, invalid credentials,
 * email registration, expired/revoked tokens, refresh tokens, and sign out.
 */
class FakeSupabaseAuthApi : SupabaseAuthApi {
    private val registeredUsers = mutableMapOf<String, Pair<String, SupabaseUser>>() // email -> (password, user)
    private val validAccessTokens = mutableMapOf<String, SupabaseUser>() // token -> user
    private val validRefreshTokens = mutableMapOf<String, SupabaseUser>() // refreshToken -> user
    private val profiles = mutableMapOf<String, SupabaseProfileDto>()

    var signOutCalledCount = 0
    var refreshCalledCount = 0
    var recoverCalledCount = 0
    var tokenExpirationSeconds = 3600L

    fun seedExistingUser(id: String, email: String, password: String, fullName: String) {
        val normalized = email.trim().lowercase()
        val user = SupabaseUser(
            id = id,
            email = normalized,
            userMetadata = mapOf("full_name" to fullName)
        )
        registeredUsers[normalized] = password to user
        profiles[id] = SupabaseProfileDto(id = id, fullName = fullName, email = normalized)
    }

    fun invalidateAccessToken(token: String) {
        validAccessTokens.remove(token)
    }

    fun invalidateRefreshToken(token: String) {
        validRefreshTokens.remove(token)
    }

    override suspend fun signUp(apiKey: String, request: SupabaseSignUpRequest): Response<SupabaseAuthResponse> {
        val email = request.email.trim().lowercase()
        if (registeredUsers.containsKey(email)) {
            return Response.error(
                422,
                """{"error":"user_already_exists","msg":"User already registered"}"""
                    .toResponseBody("application/json".toMediaType())
            )
        }
        val uid = "sb-uuid-${registeredUsers.size + 1}"
        val user = SupabaseUser(
            id = uid,
            email = email,
            userMetadata = mapOf("full_name" to request.data.fullName)
        )
        registeredUsers[email] = request.password to user
        val accessToken = "access-token-$uid"
        val refreshToken = "refresh-token-$uid"
        validAccessTokens[accessToken] = user
        validRefreshTokens[refreshToken] = user

        return Response.success(
            SupabaseAuthResponse(
                accessToken = accessToken,
                tokenType = "bearer",
                expiresIn = tokenExpirationSeconds,
                refreshToken = refreshToken,
                user = user
            )
        )
    }

    override suspend fun signInWithPassword(
        apiKey: String,
        grantType: String,
        request: SupabaseSignInRequest
    ): Response<SupabaseAuthResponse> {
        val email = request.email.trim().lowercase()
        val entry = registeredUsers[email]
        if (entry == null || entry.first != request.password) {
            return Response.error(
                400,
                """{"error":"invalid_grant","error_description":"Invalid login credentials"}"""
                    .toResponseBody("application/json".toMediaType())
            )
        }
        val user = entry.second
        val accessToken = "access-token-${user.id}"
        val refreshToken = "refresh-token-${user.id}"
        validAccessTokens[accessToken] = user
        validRefreshTokens[refreshToken] = user

        return Response.success(
            SupabaseAuthResponse(
                accessToken = accessToken,
                tokenType = "bearer",
                expiresIn = tokenExpirationSeconds,
                refreshToken = refreshToken,
                user = user
            )
        )
    }

    override suspend fun refreshSession(
        apiKey: String,
        grantType: String,
        request: SupabaseRefreshTokenRequest
    ): Response<SupabaseAuthResponse> {
        refreshCalledCount++
        val user = validRefreshTokens[request.refreshToken]
            ?: return Response.error(
                400,
                """{"error":"invalid_grant","error_description":"Invalid Refresh Token"}"""
                    .toResponseBody("application/json".toMediaType())
            )
        val newAccess = "refreshed-access-${user.id}"
        val newRefresh = "refreshed-refresh-${user.id}"
        validAccessTokens[newAccess] = user
        validRefreshTokens[newRefresh] = user
        return Response.success(
            SupabaseAuthResponse(
                accessToken = newAccess,
                tokenType = "bearer",
                expiresIn = tokenExpirationSeconds,
                refreshToken = newRefresh,
                user = user
            )
        )
    }

    override suspend fun getCurrentUser(
        apiKey: String,
        authorization: String
    ): Response<SupabaseUser> {
        val token = authorization.removePrefix("Bearer ").trim()
        val user = validAccessTokens[token]
            ?: return Response.error(
                401,
                """{"error":"unauthorized","message":"Invalid JWT"}"""
                    .toResponseBody("application/json".toMediaType())
            )
        return Response.success(user)
    }

    override suspend fun signOut(apiKey: String, authorization: String): Response<Unit> {
        signOutCalledCount++
        val token = authorization.removePrefix("Bearer ").trim()
        validAccessTokens.remove(token)
        return Response.success(Unit)
    }

    override suspend fun sendPasswordReset(
        apiKey: String,
        request: SupabaseRecoverRequest
    ): Response<Unit> {
        recoverCalledCount++
        return Response.success(Unit)
    }

    override suspend fun getUserProfile(
        apiKey: String,
        authorization: String,
        idFilter: String,
        select: String
    ): Response<List<SupabaseProfileDto>> {
        val uid = idFilter.removePrefix("eq.")
        val dto = profiles[uid]
        return Response.success(if (dto != null) listOf(dto) else emptyList())
    }

    override suspend fun upsertUserProfile(
        apiKey: String,
        authorization: String,
        profile: SupabaseProfileDto
    ): Response<List<SupabaseProfileDto>> {
        profiles[profile.id] = profile
        return Response.success(listOf(profile))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthenticationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var sessionDataStore: SessionDataStore
    private lateinit var fakeApi: FakeSupabaseAuthApi
    private var fakeNowMillis: Long = 1_700_000_000_000L
    private lateinit var authRepository: SupabaseAuthRepository
    private lateinit var fitnessRepository: FitnessRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDataStore = SessionDataStore(context)
        runBlocking { sessionDataStore.clearSession() }

        fakeApi = FakeSupabaseAuthApi().apply {
            seedExistingUser(
                id = "sb-user-100",
                email = "amina@womanfit.app",
                password = "StrongPassword123",
                fullName = "Amina Khan"
            )
        }

        authRepository = SupabaseAuthRepository(
            sessionDataStore = sessionDataStore,
            userDao = database.userDao(),
            apiProvider = { fakeApi },
            anonKeyProvider = { "test-anon-key" },
            nowMillisProvider = { fakeNowMillis }
        )

        fitnessRepository = FitnessRepository(
            database = database,
            activeUserIdFlow = authRepository.activeUserId,
            onProfileSync = authRepository::syncProfileUpdate
        )
    }

    @After
    fun tearDown() {
        runBlocking { sessionDataStore.clearSession() }
        database.close()
    }

    @Test
    fun `1 and 2 - fresh installation starts logged out and blocks dashboard access`() = runBlocking {
        val state = authRepository.restoreAndVerifySession()
        assertEquals(SessionState.LoggedOut, state)
        assertNull(authRepository.activeUserId.value)
        assertNull(sessionDataStore.getSession())
    }

    @Test
    fun `3 - valid existing account can sign in via Supabase Auth`() = runBlocking {
        val result = authRepository.signIn("Amina@womanfit.app ", "StrongPassword123")
        assertTrue(result is AuthResult.Success)
        val success = result as AuthResult.Success
        assertEquals("sb-user-100", success.userId)
        assertEquals("amina@womanfit.app", success.email)
        assertEquals(SessionState.LoggedIn("sb-user-100"), authRepository.sessionState.value)

        // Session persisted in Jetpack DataStore
        val saved = sessionDataStore.getSession()
        assertNotNull(saved)
        assertEquals("access-token-sb-user-100", saved?.accessToken)
    }

    @Test
    fun `4 - invalid password displays error and never creates account or grants access`() = runBlocking {
        val result = authRepository.signIn("amina@womanfit.app", "WrongPassword999")
        assertTrue(result is AuthResult.Error)
        assertEquals(SessionState.Loading, authRepository.sessionState.value) // unchanged from initial, or LoggedOut after restore
        assertNull(authRepository.activeUserId.value)
        assertNull(sessionDataStore.getSession())
    }

    @Test
    fun `5 - new user can create account via Supabase and enters authenticated state`() = runBlocking {
        val result = authRepository.signUp(
            fullNameInput = "Zara Malik",
            emailInput = "zara@example.com",
            passwordInput = "Secure678",
            confirmPasswordInput = "Secure678"
        )
        assertTrue(result is AuthResult.Success)
        val success = result as AuthResult.Success
        assertEquals("zara@example.com", success.email)
        assertEquals("Zara Malik", success.fullName)
        assertTrue(authRepository.sessionState.value is SessionState.LoggedIn)

        val profile = fitnessRepository.userProfile.first()
        assertEquals(success.userId, profile.id)
        assertEquals("Zara Malik", profile.name)
    }

    @Test
    fun `6 - invalid email is rejected for both sign in and sign up`() = runBlocking {
        val signInRes = authRepository.signIn("not-an-email", "password123")
        assertTrue(signInRes is AuthResult.Error)
        assertEquals(AuthField.EMAIL, (signInRes as AuthResult.Error).field)

        val signUpRes = authRepository.signUp("Sara", "bad@", "password123", "password123")
        assertTrue(signUpRes is AuthResult.Error)
        assertEquals(AuthField.EMAIL, (signUpRes as AuthResult.Error).field)
    }

    @Test
    fun `7 - password mismatch and short password under 6 chars are rejected`() = runBlocking {
        val shortPass = authRepository.signUp("Sara", "sara@example.com", "12345", "12345")
        assertTrue(shortPass is AuthResult.Error)
        assertEquals(AuthField.PASSWORD, (shortPass as AuthResult.Error).field)

        val mismatch = authRepository.signUp("Sara", "sara@example.com", "123456", "654321")
        assertTrue(mismatch is AuthResult.Error)
        assertEquals(AuthField.CONFIRM_PASSWORD, (mismatch as AuthResult.Error).field)
    }

    @Test
    fun `9 - sign out calls Supabase logout, clears DataStore session, and locks app`() = runBlocking {
        authRepository.signIn("amina@womanfit.app", "StrongPassword123")
        assertTrue(authRepository.sessionState.value is SessionState.LoggedIn)

        authRepository.signOut()

        assertEquals(1, fakeApi.signOutCalledCount)
        assertEquals(SessionState.LoggedOut, authRepository.sessionState.value)
        assertNull(authRepository.activeUserId.value)
        assertNull(sessionDataStore.getSession())
    }

    @Test
    fun `11 - restarting app verifies saved session against Supabase`() = runBlocking {
        authRepository.signIn("amina@womanfit.app", "StrongPassword123")

        // Simulate app restart with fresh repository reading same DataStore
        val restartedRepo = SupabaseAuthRepository(
            sessionDataStore = sessionDataStore,
            userDao = database.userDao(),
            apiProvider = { fakeApi },
            anonKeyProvider = { "test-anon-key" },
            nowMillisProvider = { fakeNowMillis }
        )
        val restoredState = restartedRepo.restoreAndVerifySession()
        assertEquals(SessionState.LoggedIn("sb-user-100"), restoredState)
    }

    @Test
    fun `12 - expired or invalid session does not grant unauthorized access`() = runBlocking {
        // Save an expired session with an invalid refresh token
        sessionDataStore.saveSession(
            AuthSession(
                userId = "sb-user-100",
                email = "amina@womanfit.app",
                fullName = "Amina Khan",
                accessToken = "expired-token",
                refreshToken = "invalid-refresh-token",
                expiresAtMillis = fakeNowMillis - 10_000L
            )
        )

        val state = authRepository.restoreAndVerifySession()
        assertEquals(SessionState.LoggedOut, state)
        assertNull(sessionDataStore.getSession())
        assertTrue(fakeApi.refreshCalledCount > 0)
    }

    @Test
    fun `12b - valid refresh token renews expired access token seamlessly`() = runBlocking {
        authRepository.signIn("amina@womanfit.app", "StrongPassword123")
        // Advance clock past expiration
        fakeNowMillis += 4000_000L

        val state = authRepository.restoreAndVerifySession()
        assertEquals(SessionState.LoggedIn("sb-user-100"), state)
        val refreshedSession = sessionDataStore.getSession()
        assertEquals("refreshed-access-sb-user-100", refreshedSession?.accessToken)
    }

    @Test
    fun `13 - unconfigured Supabase refuses login with no fake offline fallback`() = runBlocking {
        val unconfiguredRepo = SupabaseAuthRepository(
            sessionDataStore = sessionDataStore,
            userDao = database.userDao(),
            apiProvider = { null },
            anonKeyProvider = { "DEFAULT_EMPTY" }
        )

        val result = unconfiguredRepo.signIn("amina@womanfit.app", "StrongPassword123")
        assertTrue(result is AuthResult.Error)
        assertNull(unconfiguredRepo.activeUserId.value)
    }

    @Test
    fun `14 - UI renders Sign In screen first without any Demo button and accepts text input`() {
        val appContext = ApplicationProvider.getApplicationContext<android.app.Application>()
        val authVm = AuthViewModel(appContext)
        val fitnessVm = FitnessViewModel(appContext)

        composeRule.setContent {
            WomanFitTheme {
                WomanFitApp(authViewModel = authVm, fitnessViewModel = fitnessVm)
            }
        }

        composeRule.waitForIdle()

        // Confirm Sign In screen appears and no demo account text exists
        composeRule.onNodeWithTag("login_email_input").assertExists()
        composeRule.onNodeWithTag("login_password_input").assertExists()
        composeRule.onNodeWithTag("sign_in_submit_button").assertExists()
        composeRule.onNodeWithText("Continue as Sarah", substring = true, ignoreCase = true).assertDoesNotExist()
        composeRule.onNodeWithText("Quick Demo Testing", substring = true, ignoreCase = true).assertDoesNotExist()

        // Test typing into Email & Password fields
        composeRule.onNodeWithTag("login_email_input").performTextInput("user@womanfit.app")
        composeRule.onNodeWithTag("login_password_input").performTextInput("MyPass123")
        composeRule.onNodeWithTag("toggle_password_visibility").performClick()

        composeRule.onNodeWithTag("login_email_input").assertTextContains("user@womanfit.app")
        composeRule.onNodeWithTag("login_password_input").assertTextContains("MyPass123")

        // Switch to Sign Up tab and verify fields
        composeRule.onNodeWithTag("tab_sign_up").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("signup_name_input").assertExists()
        composeRule.onNodeWithTag("signup_email_input").assertExists()
        composeRule.onNodeWithTag("signup_password_input").assertExists()
        composeRule.onNodeWithTag("signup_confirm_password_input").assertExists()
        composeRule.onNodeWithTag("create_account_button").assertExists()
    }

    @Test
    fun `15 - duplicate email registration is rejected cleanly on email field`() = runBlocking {
        val result = authRepository.signUp(
            fullNameInput = "Amina Two",
            emailInput = "amina@womanfit.app",
            passwordInput = "StrongPassword123",
            confirmPasswordInput = "StrongPassword123"
        )
        assertTrue(result is AuthResult.Error)
        val err = result as AuthResult.Error
        assertEquals(AuthField.EMAIL, err.field)
        assertTrue(err.message.contains("already exists", ignoreCase = true))
    }

    @Test
    fun `16 - forgot password sends recover request to Supabase`() = runBlocking {
        val invalid = authRepository.requestPasswordReset("invalid")
        assertTrue(invalid is ResetRequestResult.Error)

        val valid = authRepository.requestPasswordReset("amina@womanfit.app")
        assertTrue(valid is ResetRequestResult.Success)
        assertEquals(1, fakeApi.recoverCalledCount)
    }

    @Test
    fun `17 - updating profile and logging weight sync to Supabase PostgreSQL profile`() = runBlocking {
        val signInRes = authRepository.signIn("amina@womanfit.app", "StrongPassword123")
        assertTrue(signInRes is AuthResult.Success)

        val initial = fitnessRepository.userProfile.first()
        val updated = initial.copy(
            fitnessGoal = "Strength & Glutes",
            cycleLengthDays = 30,
            lastPeriodStartTimestamp = 1_700_000_000_000L
        )
        fitnessRepository.updateUser(updated)
        fitnessRepository.logWeight(weight = 59.5f, waist = 68f, hips = 94f, note = "Synced")

        // Verify remote Supabase profile received synced updates
        val remoteResponse = fakeApi.getUserProfile(
            apiKey = "test-anon-key",
            authorization = "Bearer access-token-sb-user-100",
            idFilter = "eq.sb-user-100"
        )
        val remote = remoteResponse.body()?.firstOrNull()
        assertNotNull(remote)
        assertEquals("Strength & Glutes", remote?.fitnessGoal)
        assertEquals(30, remote?.cycleLengthDays)
        assertEquals(1_700_000_000_000L, remote?.lastPeriodStartTimestamp)
        assertEquals(59.5f, remote?.currentWeightKg ?: 0f, 0.01f)
    }

    @Test
    fun `18 - SupabaseConfig resolves both Supabase API and backend-proxy PostgreSQL URLs cleanly`() {
        val direct = SupabaseConfig.resolveEndpoint(
            supabaseUrl = "https://abc.supabase.co",
            supabaseAnonKey = "anon-key-123",
            backendProxyUrl = "https://proxy.example.com/api/coach/ask",
            backendProxyKey = "proxy-secret"
        )
        assertEquals("https://abc.supabase.co" to "anon-key-123", direct)

        val viaProxy = SupabaseConfig.resolveEndpoint(
            supabaseUrl = "DEFAULT_EMPTY",
            supabaseAnonKey = "DEFAULT_EMPTY",
            backendProxyUrl = "https://proxy.example.com/api/coach/ask",
            backendProxyKey = "proxy-secret"
        )
        assertEquals("https://proxy.example.com" to "proxy-secret", viaProxy)

        val unconfigured = SupabaseConfig.resolveEndpoint(
            supabaseUrl = "DEFAULT_EMPTY",
            supabaseAnonKey = "DEFAULT_EMPTY",
            backendProxyUrl = "DEFAULT_EMPTY",
            backendProxyKey = "DEFAULT_EMPTY"
        )
        assertNull(unconfigured)
    }
}
