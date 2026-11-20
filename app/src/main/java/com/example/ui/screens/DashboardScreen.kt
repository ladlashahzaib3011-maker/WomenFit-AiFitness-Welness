package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DailyGoalItem
import com.example.model.WorkoutPlan
import com.example.ui.FitnessViewModel
import com.example.ui.MainTab
import com.example.ui.components.InteractiveWaterTracker
import com.example.ui.components.MetricStatCard
import com.example.ui.components.WomanFitTopBar
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.userProfile.collectAsState()
    val progress by viewModel.dailyProgress.collectAsState()
    val goals by viewModel.dailyGoals.collectAsState()
    val featuredWorkout = viewModel.workouts.firstOrNull()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var newGoalTitle by remember { mutableStateOf("") }
    var newGoalCategory by remember { mutableStateOf("Wellness") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // App Bar & Welcome
        item {
            WomanFitTopBar(
                userName = user.name.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "Member",
                streakDays = progress.streakDays,
                onAvatarClick = { viewModel.selectTab(MainTab.PROFILE) }
            )
        }

        // Daily Motivation Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WomanFitLightRose),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✨", fontSize = 22.sp)
                    }
                    Column {
                        Text(
                            text = "Daily Affirmation",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = WomanFitRose,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "“Honor your body's rhythm today. Strong, resilient, and blooming with grace.”",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        // Metric Rings & Stats
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Active Burn",
                        value = "${progress.caloriesBurned} kcal",
                        subtitle = "Goal: ${user.dailyCalorieBurnGoal} kcal",
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = WomanFitRose,
                        gradientColors = listOf(Color(0xFFFDE8EF), Color(0xFFFBCFE8)),
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = "Active Time",
                        value = "${progress.activeMinutes} mins",
                        subtitle = "${progress.completedWorkoutsCount} session today",
                        icon = Icons.Default.Timer,
                        iconTint = WomanFitViolet,
                        gradientColors = listOf(Color(0xFFEDE9FE), Color(0xFFDDD6FE)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Cycle Sync Highlight Card
        item {
            val cycleStatus = remember(user.lastPeriodStartTimestamp, user.cycleLengthDays) {
                com.example.model.CycleCalculator.getStatus(user.lastPeriodStartTimestamp, user.cycleLengthDays)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { viewModel.selectTab(MainTab.CYCLE) }
                    .testTag("dashboard_cycle_sync_card"),
                colors = CardDefaults.cardColors(containerColor = WomanFitLightViolet),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isPeriodConfigured = user.lastPeriodStartTimestamp > 0L
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = if (isPeriodConfigured) cycleStatus.phase.iconEmoji else "🌸", fontSize = 22.sp)
                        }

                        Column {
                            Text(
                                text = if (isPeriodConfigured) "Day ${cycleStatus.currentCycleDay} • ${cycleStatus.phase.phaseName}" else "Cycle & Hormone Sync",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = WomanFitDarkViolet
                                )
                            )
                            Text(
                                text = if (isPeriodConfigured) "Recommended: ${cycleStatus.phase.recommendedCategory.title}" else "Set period date to personalize workouts",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    TextButton(onClick = { viewModel.selectTab(MainTab.CYCLE) }) {
                        Text(
                            text = if (isPeriodConfigured) "View Sync" else "Set Up",
                            color = WomanFitViolet,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Interactive Water Tracker
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                InteractiveWaterTracker(
                    currentMl = progress.waterIntakeMl,
                    targetMl = user.dailyWaterGoalMl,
                    onAddWater = { viewModel.addWater(it) },
                    onReset = { viewModel.resetWater() }
                )
            }
        }

        // Featured Daily Workout Card
        if (featuredWorkout != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Featured Workout",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        TextButton(
                            onClick = { viewModel.selectTab(MainTab.WORKOUTS) },
                            modifier = Modifier.testTag("see_all_workouts_button")
                        ) {
                            Text("See All", color = WomanFitRose, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { viewModel.startWorkout(featuredWorkout) }
                            .testTag("featured_workout_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_hero_workout),
                                    contentDescription = featuredWorkout.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                            )
                                        )
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = WomanFitRose,
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "DAILY PICK",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "⏱ ${featuredWorkout.durationMinutes} mins",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "🔥 ${featuredWorkout.estimatedCalories} kcal",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = featuredWorkout.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = featuredWorkout.description,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { viewModel.startWorkout(featuredWorkout) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("start_featured_workout_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Workout Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Goals Checklist
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Wellness Goals",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    IconButton(
                        onClick = { showAddGoalDialog = true },
                        modifier = Modifier.testTag("add_custom_goal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Goal",
                            tint = WomanFitRose
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        goals.forEach { goal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.toggleGoal(goal) }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = goal.isCompleted,
                                    onCheckedChange = { viewModel.toggleGoal(goal) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = WomanFitRose,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goal.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (goal.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                                            color = if (goal.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = goal.category,
                                        style = MaterialTheme.typography.labelSmall.copy(color = WomanFitViolet)
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteGoal(goal) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF3E8F0), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Add Daily Wellness Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newGoalTitle,
                        onValueChange = { newGoalTitle = it },
                        label = { Text("Goal Title") },
                        placeholder = { Text("e.g. 20m post-dinner walk") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_goal_title_input")
                    )

                    OutlinedTextField(
                        value = newGoalCategory,
                        onValueChange = { newGoalCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGoalTitle.isNotBlank()) {
                            viewModel.addGoal(newGoalTitle.trim(), newGoalCategory.trim())
                            newGoalTitle = ""
                            showAddGoalDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose),
                    modifier = Modifier.testTag("confirm_add_goal_button")
                ) {
                    Text("Add Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
