package com.example.data

import com.example.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val userId: String) : SessionState
}

enum class AuthField {
    GENERAL,
    FULL_NAME,
    EMAIL,
    PASSWORD,
    CONFIRM_PASSWORD
}

sealed interface AuthResult {
    data class Success(
        val userId: String,
        val email: String,
        val fullName: String
    ) : AuthResult

    data class EmailVerificationRequired(
        val email: String,
        val message: String
    ) : AuthResult

    data class Error(
        val message: String,
        val field: AuthField = AuthField.GENERAL
    ) : AuthResult
}

sealed interface ResetRequestResult {
    data class Success(val message: String) : ResetRequestResult
    data class Error(val message: String) : ResetRequestResult
}

/**
 * Authentication Repository Contract.
 *
 * Strictly enforces backend-verified authentication, session restoration,
 * token refresh, profile synchronization, and secure sign out.
 */
interface AuthRepository {
    val activeUserId: StateFlow<String?>
    val sessionState: StateFlow<SessionState>

    suspend fun restoreAndVerifySession(): SessionState

    suspend fun signIn(
        emailInput: String,
        passwordInput: String
    ): AuthResult

    suspend fun signUp(
        fullNameInput: String,
        emailInput: String,
        passwordInput: String,
        confirmPasswordInput: String
    ): AuthResult

    suspend fun requestPasswordReset(emailInput: String): ResetRequestResult

    /** Saves the profile to Supabase (womenfit_ai_users). Returns true when it reached the cloud. */
    suspend fun syncProfileUpdate(userProfile: UserProfile): Boolean

    suspend fun signOut()
}
