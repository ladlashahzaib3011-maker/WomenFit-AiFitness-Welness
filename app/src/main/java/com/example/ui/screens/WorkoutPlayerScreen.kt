package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WorkoutPlan
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class PlayerPhase {
    EXERCISE,
    REST,
    COMPLETED
}

@Composable
fun WorkoutPlayerScreen(
    plan: WorkoutPlan,
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.closeWorkoutPlayer()
    }

    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(PlayerPhase.EXERCISE) }
    var isPaused by remember { mutableStateOf(false) }

    val exercises = plan.exercises
    val currentExercise = exercises.getOrNull(currentExerciseIndex) ?: exercises.first()

    var secondsRemaining by remember(currentExerciseIndex, phase) {
        mutableIntStateOf(
            if (phase == PlayerPhase.EXERCISE) currentExercise.durationSeconds else currentExercise.restSeconds
        )
    }

    val totalDurationForPhase = if (phase == PlayerPhase.EXERCISE) currentExercise.durationSeconds else currentExercise.restSeconds

    val context = androidx.compose.ui.platform.LocalContext.current
    val soundEffects = remember { com.example.ui.audio.WorkoutSoundEffects(context) }
    val user by viewModel.userProfile.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            soundEffects.release()
        }
    }

    // Countdown Timer Loop
    LaunchedEffect(isPaused, phase, currentExerciseIndex, secondsRemaining) {
        if (!isPaused && phase != PlayerPhase.COMPLETED) {
            if (secondsRemaining > 0) {
                if (secondsRemaining in 1..3) {
                    soundEffects.playCountdownBeep(user.soundEnabled)
                }
                delay(1000L)
                secondsRemaining -= 1
            } else {
                // Transition phase
                if (phase == PlayerPhase.EXERCISE) {
                    if (currentExerciseIndex < exercises.size - 1) {
                        phase = PlayerPhase.REST
                        soundEffects.playRestChime(user.soundEnabled)
                    } else {
                        phase = PlayerPhase.COMPLETED
                        soundEffects.playCelebrationFanfare(user.soundEnabled)
                        viewModel.finishWorkout(plan)
                    }
                } else if (phase == PlayerPhase.REST) {
                    currentExerciseIndex += 1
                    phase = PlayerPhase.EXERCISE
                    soundEffects.playExerciseStartChime(user.soundEnabled)
                }
            }
        }
    }

    val progressFraction = if (totalDurationForPhase > 0) {
        (secondsRemaining.toFloat() / totalDurationForPhase.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        label = "timer_ring"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (phase == PlayerPhase.COMPLETED) {
            // Celebration screen
            WorkoutCompletionView(
                plan = plan,
                onFinish = { viewModel.closeWorkoutPlayer() }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.closeWorkoutPlayer() },
                        modifier = Modifier.testTag("exit_workout_player_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Workout")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Exercise ${currentExerciseIndex + 1} of ${exercises.size}",
                            style = MaterialTheme.typography.labelSmall.copy(color = WomanFitRose)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (phase == PlayerPhase.EXERCISE) WomanFitLightRose else WomanFitLightViolet
                    ) {
                        Text(
                            text = if (phase == PlayerPhase.EXERCISE) "ACTIVE" else "REST",
                            color = if (phase == PlayerPhase.EXERCISE) WomanFitDeepRose else WomanFitDarkViolet,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Circular Timer and Exercise Name
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .testTag("timer_countdown_display"),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = if (phase == PlayerPhase.EXERCISE) Color(0xFFFCE7F3) else Color(0xFFEDE9FE),
                        strokeWidth = 14.dp,
                    )

                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (phase == PlayerPhase.EXERCISE) WomanFitRose else WomanFitViolet,
                        strokeWidth = 14.dp,
                        strokeCap = StrokeCap.Round
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%02d:%02d".format(secondsRemaining / 60, secondsRemaining % 60),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 48.sp,
                                color = if (phase == PlayerPhase.EXERCISE) WomanFitRose else WomanFitViolet
                            )
                        )
                        Text(
                            text = if (phase == PlayerPhase.EXERCISE) currentExercise.repsOrHold else "Next Up...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Exercise Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (phase == PlayerPhase.EXERCISE) currentExercise.name else "Up Next: ${exercises.getOrNull(currentExerciseIndex + 1)?.name ?: "Finish!"}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WomanFitLightViolet
                        ) {
                            Text(
                                text = "🎯 ${currentExercise.targetMuscles}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WomanFitDarkViolet,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = currentExercise.instructions,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFFBEB)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "💡 ", fontSize = 12.sp)
                                Text(
                                    text = currentExercise.formTip,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Player Control Buttons (Prev, Play/Pause, Next)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    FilledTonalIconButton(
                        onClick = {
                            if (currentExerciseIndex > 0) {
                                currentExerciseIndex -= 1
                                phase = PlayerPhase.EXERCISE
                            }
                        },
                        enabled = currentExerciseIndex > 0,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("player_prev_button")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                    }

                    // Play / Pause Primary Button
                    FilledIconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("player_play_pause_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = WomanFitRose)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Skip / Next Button
                    FilledTonalIconButton(
                        onClick = {
                            if (currentExerciseIndex < exercises.size - 1) {
                                currentExerciseIndex += 1
                                phase = PlayerPhase.EXERCISE
                            } else {
                                phase = PlayerPhase.COMPLETED
                                viewModel.finishWorkout(plan)
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("player_next_button")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutCompletionView(
    plan: WorkoutPlan,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(WomanFitRose, WomanFitDeepRose))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🎉", fontSize = 54.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Radiant Workout Finished!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        )

        Text(
            text = "Incredible job showing up for your body today, queen!",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⏱ Time", style = MaterialTheme.typography.labelSmall.copy(color = WomanFitViolet))
                    Text(
                        text = "${plan.durationMinutes}m",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔥 Burned", style = MaterialTheme.typography.labelSmall.copy(color = WomanFitRose))
                    Text(
                        text = "${plan.estimatedCalories} kcal",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "💪 Moves", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF0284C7)))
                    Text(
                        text = "${plan.exercises.size}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("workout_completion_finish_button"),
            colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save & Return to Dashboard", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
