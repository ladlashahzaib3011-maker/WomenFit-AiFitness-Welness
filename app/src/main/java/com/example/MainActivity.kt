package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.CloudAlertHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.data.SessionState
import com.example.ui.AuthViewModel
import com.example.ui.FitnessViewModel
import com.example.ui.MainTab
import com.example.ui.components.WomanFitBottomNav
import com.example.ui.screens.*
import com.example.ui.theme.WomanFitTheme

object NavRoutes {
    const val SPLASH = "splash_screen"

    const val AUTH_GRAPH = "auth_graph"
    const val SIGN_IN = "sign_in_screen"
    const val SIGN_UP = "sign_up_screen"
    const val FORGOT_PASSWORD = "forgot_password_screen"

    const val MAIN_GRAPH = "main_graph"
    const val HOME_CONTAINER = "home_container_screen"
}

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val fitnessViewModel: FitnessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.ui.notifications.WellnessNotificationManager.createNotificationChannel(this)
        setContent {
            WomanFitTheme {
                WomanFitApp(
                    authViewModel = authViewModel,
                    fitnessViewModel = fitnessViewModel
                )
            }
        }
    }
}

@Composable
fun WomanFitApp(
    authViewModel: AuthViewModel,
    fitnessViewModel: FitnessViewModel
) {
    val navController = rememberNavController()
    val sessionState by authViewModel.sessionState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = NavRoutes.SPLASH
        ) {
            // 1. Splash / Cold Launch Session Verification (Never renders Dashboard prior to check)
            composable(NavRoutes.SPLASH) {
                SplashScreen()

                LaunchedEffect(sessionState) {
                    when (sessionState) {
                        is SessionState.LoggedIn -> {
                            navController.navigate(NavRoutes.MAIN_GRAPH) {
                                popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        is SessionState.LoggedOut -> {
                            navController.navigate(NavRoutes.AUTH_GRAPH) {
                                popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        is SessionState.Loading -> {
                            // Hold splash until DataStore & token verification finishes
                        }
                    }
                }
            }

            // 2. Authentication Graph (SignIn, SignUp, ForgotPassword)
            navigation(
                startDestination = NavRoutes.SIGN_IN,
                route = NavRoutes.AUTH_GRAPH
            ) {
                composable(NavRoutes.SIGN_IN) {
                    SignInScreen(
                        authViewModel = authViewModel,
                        onNavigateToSignUp = {
                            navController.navigate(NavRoutes.SIGN_UP) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToForgotPassword = {
                            navController.navigate(NavRoutes.FORGOT_PASSWORD) {
                                launchSingleTop = true
                            }
                        },
                        onSignInSuccess = {
                            navController.navigate(NavRoutes.MAIN_GRAPH) {
                                popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(NavRoutes.SIGN_UP) {
                    SignUpScreen(
                        authViewModel = authViewModel,
                        onNavigateToSignIn = {
                            navController.popBackStack()
                        },
                        onSignUpSuccess = {
                            navController.navigate(NavRoutes.MAIN_GRAPH) {
                                popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(NavRoutes.FORGOT_PASSWORD) {
                    ForgotPasswordScreen(
                        authViewModel = authViewModel,
                        onBackToSignIn = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            // 3. Main App Graph (Protected by Navigation Authentication Guard)
            navigation(
                startDestination = NavRoutes.HOME_CONTAINER,
                route = NavRoutes.MAIN_GRAPH
            ) {
                composable(NavRoutes.HOME_CONTAINER) {
                    // Navigation Authentication Guard: Force redirect if session ever becomes unauthenticated
                    LaunchedEffect(sessionState) {
                        if (sessionState is SessionState.LoggedOut) {
                            fitnessViewModel.resetUiStateOnSignOut()
                            navController.navigate(NavRoutes.AUTH_GRAPH) {
                                popUpTo(NavRoutes.MAIN_GRAPH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    if (sessionState is SessionState.LoggedIn) {
                        MainContainerScreen(
                            viewModel = fitnessViewModel,
                            onSignOutConfirmed = {
                                authViewModel.signOut {
                                    fitnessViewModel.resetUiStateOnSignOut()
                                    navController.navigate(NavRoutes.AUTH_GRAPH) {
                                        popUpTo(NavRoutes.MAIN_GRAPH) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    } else {
                        SplashScreen()
                    }
                }
            }
        }

        // App-wide alerts after every save / sync ("💧 Water +250 ml — saved to cloud ✓"),
        // shown above the bottom navigation bar on every screen.
        CloudAlertHost(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 84.dp)
        )
    }
}

/**
 * Main App Container
 *
 * Hosts interactive workouts and the 7 bottom-navigation tabs.
 */
@Composable
fun MainContainerScreen(
    viewModel: FitnessViewModel,
    onSignOutConfirmed: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeWorkout by viewModel.activeWorkout.collectAsState()

    // Handle back button on secondary tabs to navigate to Home
    if (activeWorkout == null && currentTab != MainTab.HOME) {
        BackHandler {
            viewModel.selectTab(MainTab.HOME)
        }
    }

    if (activeWorkout != null) {
        WorkoutPlayerScreen(
            plan = activeWorkout!!,
            viewModel = viewModel
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                WomanFitBottomNav(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        ) { innerPadding ->
            val screenModifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())

            when (currentTab) {
                MainTab.HOME -> DashboardScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
                MainTab.CYCLE -> CycleScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
                MainTab.WORKOUTS -> WorkoutsScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
                MainTab.NUTRITION -> NutritionScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
                MainTab.COACH -> AiCoachScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
                MainTab.PROGRESS -> ProgressScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
                MainTab.PROFILE -> ProfileScreen(
                    viewModel = viewModel,
                    onSignOutConfirmed = onSignOutConfirmed,
                    modifier = screenModifier
                )
            }
        }
    }
}
