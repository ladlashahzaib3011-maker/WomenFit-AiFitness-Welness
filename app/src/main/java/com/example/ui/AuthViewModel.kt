package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
    val infoMessage: String? = null
)

/**
 * Dedicated Authentication ViewModel (MVVM).
 *
 * Manages session verification on launch, Sign In, Sign Up, Forgot Password,
 * and Sign Out via AuthRepository. Never stores plain-text passwords or demo credentials.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = AppContainer.getInstance(application).authRepository

    val sessionState: StateFlow<SessionState> = authRepository.sessionState

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    fun restoreSession() {
        viewModelScope.launch {
            authRepository.restoreAndVerifySession()
        }
    }

    fun clearErrorsAndMessages() {
        _uiState.update {
            it.copy(
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                generalError = null,
                infoMessage = null
            )
        }
    }

    fun clearFieldError(field: AuthField) {
        _uiState.update { state ->
            when (field) {
                AuthField.FULL_NAME -> state.copy(nameError = null, generalError = null)
                AuthField.EMAIL -> state.copy(emailError = null, generalError = null)
                AuthField.PASSWORD -> state.copy(passwordError = null, generalError = null)
                AuthField.CONFIRM_PASSWORD -> state.copy(confirmPasswordError = null, generalError = null)
                AuthField.GENERAL -> state.copy(generalError = null)
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update {
                AuthUiState(isLoading = true)
            }
            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    _uiState.update { AuthUiState(isLoading = false) }
                    onSuccess()
                }
                is AuthResult.EmailVerificationRequired -> {
                    _uiState.update {
                        AuthUiState(isLoading = false, infoMessage = result.message)
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { applyFieldError(AuthUiState(isLoading = false), result) }
                }
            }
        }
    }

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        onAuthenticated: () -> Unit
    ) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { AuthUiState(isLoading = true) }
            when (val result = authRepository.signUp(fullName, email, password, confirmPassword)) {
                is AuthResult.Success -> {
                    _uiState.update { AuthUiState(isLoading = false) }
                    onAuthenticated()
                }
                is AuthResult.EmailVerificationRequired -> {
                    _uiState.update {
                        AuthUiState(
                            isLoading = false,
                            infoMessage = result.message
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { applyFieldError(AuthUiState(isLoading = false), result) }
                }
            }
        }
    }

    fun requestPasswordReset(email: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { AuthUiState(isLoading = true) }
            when (val result = authRepository.requestPasswordReset(email)) {
                is ResetRequestResult.Success -> {
                    _uiState.update {
                        AuthUiState(isLoading = false, infoMessage = result.message)
                    }
                }
                is ResetRequestResult.Error -> {
                    _uiState.update {
                        AuthUiState(isLoading = false, emailError = result.message)
                    }
                }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState()
            onSignedOut()
        }
    }

    private fun applyFieldError(base: AuthUiState, error: AuthResult.Error): AuthUiState {
        return when (error.field) {
            AuthField.FULL_NAME -> base.copy(nameError = error.message)
            AuthField.EMAIL -> base.copy(emailError = error.message)
            AuthField.PASSWORD -> base.copy(passwordError = error.message)
            AuthField.CONFIRM_PASSWORD -> base.copy(confirmPasswordError = error.message)
            AuthField.GENERAL -> base.copy(generalError = error.message)
        }
    }
}
