package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthField
import com.example.ui.AuthViewModel
import com.example.ui.components.CloudSyncBadge
import com.example.ui.theme.*

/**
 * Mandatory Sign In Screen for WomanFit.
 *
 * - Feminine pink, purple & white Material 3 design.
 * - Fully editable email & password inputs with explicit high-contrast text colors
 *   and reliable IME/keyboard handling.
 * - Zero demo accounts, zero pre-filled credentials, zero auth bypass.
 */
@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val uiState by authViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Prevent bypassing authentication gate via back navigation
    BackHandler {
        // Stay on authentication gate
    }

    val inputTextColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        disabledTextColor = Color(0xFF475569),
        cursorColor = WomanFitRose,
        focusedBorderColor = WomanFitRose,
        unfocusedBorderColor = Color(0xFFE2D5E0),
        errorBorderColor = Color(0xFFDC2626),
        focusedLabelColor = WomanFitRose,
        unfocusedLabelColor = Color(0xFF64748B),
        focusedPlaceholderColor = Color(0xFF94A3B8),
        unfocusedPlaceholderColor = Color(0xFF94A3B8),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color(0xFFF8FAFC)
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFFF8FA)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFF0F6),
                            Color(0xFFFFF8FA),
                            Color(0xFFF5F3FF)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // WomanFit Logo Crest
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(WomanFitRose, WomanFitViolet))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🌸",
                        fontSize = 38.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "WomanFit",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your Fitness & Wellness Journey Starts Here",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Supabase connection indicator ("● Cloud sync active")
                CloudSyncBadge()

                Spacer(modifier = Modifier.height(20.dp))

                // Side-by-Side Mode Switcher: Sign In | Sign Up
                AuthModeSwitcher(
                    isSignInSelected = true,
                    onSelectSignIn = {},
                    onSelectSignUp = {
                        authViewModel.clearErrorsAndMessages()
                        onNavigateToSignUp()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Card container for login form
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sign in to access your workouts, cycle sync & Coach Maya.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Info Banner (e.g. email verification or password reset sent)
                        AnimatedVisibility(visible = !uiState.infoMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Text(
                                    text = uiState.infoMessage.orEmpty(),
                                    color = Color(0xFF065F46),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // General Error Banner
                        AnimatedVisibility(visible = !uiState.generalError.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                                    .testTag("signin_general_error")
                            ) {
                                Text(
                                    text = uiState.generalError.orEmpty(),
                                    color = Color(0xFF991B1B),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Email Address Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { newValue ->
                                email = newValue
                                if (uiState.emailError != null || uiState.generalError != null) {
                                    authViewModel.clearFieldError(AuthField.EMAIL)
                                }
                            },
                            enabled = true,
                            readOnly = uiState.isLoading,
                            label = { Text("Email Address") },
                            placeholder = { Text("Enter your email address") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = WomanFitRose)
                            },
                            isError = uiState.emailError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                                autoCorrectEnabled = false
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = inputTextColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input")
                        )

                        if (uiState.emailError != null) {
                            Text(
                                text = uiState.emailError!!,
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 4.dp)
                                    .testTag("email_error_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { newValue ->
                                password = newValue
                                if (uiState.passwordError != null || uiState.generalError != null) {
                                    authViewModel.clearFieldError(AuthField.PASSWORD)
                                }
                            },
                            enabled = true,
                            readOnly = uiState.isLoading,
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = WomanFitRose)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("toggle_password_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = uiState.passwordError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                                autoCorrectEnabled = false
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    authViewModel.signIn(email, password, onSignInSuccess)
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = inputTextColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        if (uiState.passwordError != null) {
                            Text(
                                text = uiState.passwordError!!,
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 4.dp)
                                    .testTag("password_error_text")
                            )
                        }

                        // Forgot Password link
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = {
                                    authViewModel.clearErrorsAndMessages()
                                    onNavigateToForgotPassword()
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    color = WomanFitViolet,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sign In Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                authViewModel.signIn(email, password, onSignInSuccess)
                            },
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WomanFitRose,
                                disabledContainerColor = WomanFitRose.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("sign_in_submit_button")
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "Sign In",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AuthModeSwitcher(
    isSignInSelected: Boolean,
    onSelectSignIn: () -> Unit,
    onSelectSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF3E8F2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tab 1: Sign In
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSignInSelected) {
                            Brush.horizontalGradient(listOf(WomanFitRose, WomanFitViolet))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable(onClick = onSelectSignIn)
                    .testTag("tab_sign_in"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    color = if (isSignInSelected) Color.White else Color(0xFF475569),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Tab 2: Sign Up
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (!isSignInSelected) {
                            Brush.horizontalGradient(listOf(WomanFitRose, WomanFitViolet))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable(onClick = onSelectSignUp)
                    .testTag("tab_sign_up"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign Up",
                    color = if (!isSignInSelected) Color.White else Color(0xFF475569),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
