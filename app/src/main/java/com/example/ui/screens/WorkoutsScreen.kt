package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.WorkoutCategory
import com.example.model.WorkoutPlan
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*

@Composable
fun WorkoutsScreen(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<WorkoutCategory?>(null) }
    var selectedPlanForDetail by remember { mutableStateOf<WorkoutPlan?>(null) }

    val filteredWorkouts = remember(selectedCategory) {
        if (selectedCategory == null) {
            viewModel.workouts
        } else {
            viewModel.workouts.filter { it.category == selectedCategory }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Workout Programs",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Crafted for female biology, tone, energy & metabolism",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Cycle Synced Tip Pill
        item {
            val user by viewModel.userProfile.collectAsState()
            val cycleStatus = remember(user.lastPeriodStartTimestamp, user.cycleLengthDays) {
                com.example.model.CycleCalculator.getStatus(user.lastPeriodStartTimestamp, user.cycleLengthDays)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { selectedCategory = cycleStatus.phase.recommendedCategory },
                color = WomanFitLightViolet
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = cycleStatus.phase.iconEmoji, fontSize = 16.sp)
                        Text(
                            text = "Cycle Sync: ${cycleStatus.phase.phaseName} (${cycleStatus.phase.recommendedCategory.title})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = WomanFitDarkViolet
                            )
                        )
                    }
                    Text(
                        text = "Auto-Filter",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WomanFitRose,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Category Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All Workouts", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WomanFitRose,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_all_workouts")
                    )
                }
                items(WorkoutCategory.values()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.title, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WomanFitRose,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_${category.name.lowercase()}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Workout Cards
        items(filteredWorkouts) { plan ->
            WorkoutCard(
                plan = plan,
                onCardClick = { selectedPlanForDetail = plan },
                onStartClick = { viewModel.startWorkout(plan) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }

    // Workout Plan Detail Dialog
    selectedPlanForDetail?.let { plan ->
        WorkoutDetailDialog(
            plan = plan,
            onDismiss = { selectedPlanForDetail = null },
            onStartWorkout = {
                selectedPlanForDetail = null
                viewModel.startWorkout(plan)
            }
        )
    }
}

@Composable
fun WorkoutCard(
    plan: WorkoutPlan,
    onCardClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable { onCardClick() }
            .testTag("workout_card_${plan.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_workout),
                    contentDescription = plan.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (plan.category) {
                        WorkoutCategory.BEGINNER -> WomanFitTeal
                        WorkoutCategory.WEIGHT_LOSS -> WomanFitRose
                        WorkoutCategory.YOGA -> WomanFitViolet
                        WorkoutCategory.CARDIO -> WomanFitCoral
                        WorkoutCategory.STRENGTH -> WomanFitDeepRose
                        WorkoutCategory.HOME -> Color(0xFF0284C7)
                    },
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = plan.category.title.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏱ ${plan.durationMinutes}m",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "🔥 ${plan.estimatedCalories} kcal",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = plan.difficulty,
                        color = Color(0xFFFDE047),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${plan.exercises.size} Exercises included",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WomanFitViolet,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Button(
                        onClick = onStartClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("start_btn_${plan.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutDetailDialog(
    plan: WorkoutPlan,
    onDismiss: () -> Unit,
    onStartWorkout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${plan.category.title} • ${plan.durationMinutes} mins • ${plan.estimatedCalories} kcal",
                    style = MaterialTheme.typography.labelSmall.copy(color = WomanFitRose)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Exercise Instructions (${plan.exercises.size} moves):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(plan.exercises) { ex ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF7F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ex.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${ex.durationSeconds}s",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = WomanFitRose,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = "Target: ${ex.targetMuscles}",
                                style = MaterialTheme.typography.labelSmall.copy(color = WomanFitViolet)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ex.instructions,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = WomanFitLightRose
                            ) {
                                Text(
                                    text = "💡 Form Tip: ${ex.formTip}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = WomanFitDeepRose,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStartWorkout,
                colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose),
                modifier = Modifier.testTag("launch_workout_player_dialog_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Launch Workout Player", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
