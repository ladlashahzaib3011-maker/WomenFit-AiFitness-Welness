package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
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
import com.example.ui.theme.WomanFitRose
import com.example.ui.theme.WomanFitViolet

/**
 * Create Account (Sign Up) Screen for WomanFit.
 *
 * Collects Full Name, Email Address, Password (min 6 chars), and Confirm Password.
 * Authenticates/registers via Supabase Auth via AuthViewModel.
 */
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onNavigateToSignIn: () -> Unit,
    onSignUpSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val uiState by authViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

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
                    text = "Create Your Personal Wellness Account",
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
                    isSignInSelected = false,
                    onSelectSignIn = {
                        authViewModel.clearErrorsAndMessages()
                        onNavigateToSignIn()
                    },
                    onSelectSignUp = {}
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                            text = "Join WomanFit",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Track workouts, hormone cycles, hydration & AI coaching securely.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Success / Verification Info Banner
                        AnimatedVisibility(visible = !uiState.infoMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                                    .testTag("signup_info_banner")
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
                                    .testTag("signup_general_error")
                            ) {
                                Text(
                                    text = uiState.generalError.orEmpty(),
                                    color = Color(0xFF991B1B),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // 1. Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { newValue ->
                                fullName = newValue
                                if (uiState.nameError != null || uiState.generalError != null) {
                                    authViewModel.clearFieldError(AuthField.FULL_NAME)
                                }
                            },
                            enabled = true,
                            readOnly = uiState.isLoading,
                            label = { Text("Full Name") },
                            placeholder = { Text("Enter your full name") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = WomanFitRose)
                            },
                            isError = uiState.nameError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = inputTextColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_name_input")
                        )
                        if (uiState.nameError != null) {
                            Text(
                                text = uiState.nameError!!,
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 4.dp)
                                    .testTag("signup_name_error")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Email Address
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
                                .testTag("signup_email_input")
                        )
                        if (uiState.emailError != null) {
                            Text(
                                text = uiState.emailError!!,
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 4.dp)
                                    .testTag("signup_email_error")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Password (at least 6 characters)
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
                            label = { Text("Password (min 6 characters)") },
                            placeholder = { Text("Create a password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = WomanFitRose)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("signup_toggle_password")
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
                                .testTag("signup_password_input")
                        )
                        if (uiState.passwordError != null) {
                            Text(
                                text = uiState.passwordError!!,
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 4.dp)
                                    .testTag("signup_password_error")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Confirm Password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { newValue ->
                                confirmPassword = newValue
                                if (uiState.confirmPasswordError != null || uiState.generalError != null) {
                                    authViewModel.clearFieldError(AuthField.CONFIRM_PASSWORD)
                                }
                            },
                            enabled = true,
                            readOnly = uiState.isLoading,
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter your password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = WomanFitRose)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("signup_toggle_confirm_password")
                                ) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide confirm password" else "Show confirm password",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = uiState.confirmPasswordError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                                autoCorrectEnabled = false
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    authViewModel.signUp(fullName, email, password, confirmPassword, onSignUpSuccess)
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = inputTextColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_confirm_password_input")
                        )
                        if (uiState.confirmPasswordError != null) {
                            Text(
                                text = uiState.confirmPasswordError!!,
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 4.dp)
                                    .testTag("signup_confirm_password_error")
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Create Account Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                authViewModel.signUp(
                                    fullName = fullName,
                                    email = email,
                                    password = password,
                                    confirmPassword = confirmPassword,
                                    onAuthenticated = onSignUpSuccess
                                )
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
                                .testTag("create_account_button")
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "Create Account",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Navigate Back to Sign In
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Already have an account? ",
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = {
                                    authViewModel.clearErrorsAndMessages()
                                    onNavigateToSignIn()
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("navigate_to_signin_button")
                            ) {
                                Text(
                                    text = "Sign In",
                                    color = WomanFitRose,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
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
