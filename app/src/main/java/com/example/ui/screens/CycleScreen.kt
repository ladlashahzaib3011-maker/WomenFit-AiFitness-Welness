package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CycleCalculator
import com.example.model.HormoneCyclePhase
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*

@Composable
fun CycleScreen(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.userProfile.collectAsState()

    val cycleStatus = remember(user.lastPeriodStartTimestamp, user.cycleLengthDays) {
        CycleCalculator.getStatus(user.lastPeriodStartTimestamp, user.cycleLengthDays)
    }

    var selectedPhaseView by remember(cycleStatus.phase) {
        mutableStateOf(cycleStatus.phase)
    }

    var showEditCycleDialog by remember { mutableStateOf(false) }
    var inputCycleLength by remember { mutableStateOf(user.cycleLengthDays.toString()) }
    var inputDaysAgo by remember { mutableStateOf((cycleStatus.currentCycleDay - 1).toString()) }

    val animatedFraction by animateFloatAsState(
        targetValue = cycleStatus.completionFraction,
        label = "cycle_progress"
    )

    // Recommended workout matching the phase
    val recommendedWorkout = remember(selectedPhaseView) {
        viewModel.workouts.firstOrNull { it.category == selectedPhaseView.recommendedCategory }
            ?: viewModel.workouts.first()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Screen Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hormone & Cycle Sync",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "Work with your biology, not against it",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = { showEditCycleDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("log_period_btn")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Period", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val isPeriodConfigured = user.lastPeriodStartTimestamp > 0L

        // Active Cycle Status Card OR Setup Prompt Card
        item {
            if (isPeriodConfigured) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(WomanFitLightRose),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = cycleStatus.phase.iconEmoji, fontSize = 24.sp)
                                }

                                Column {
                                    Text(
                                        text = "Day ${cycleStatus.currentCycleDay} of ${cycleStatus.totalCycleLength}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = WomanFitRose
                                        )
                                    )
                                    Text(
                                        text = cycleStatus.phase.phaseName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WomanFitLightViolet
                            ) {
                                Text(
                                    text = "${cycleStatus.daysUntilNextPeriod}d to period",
                                    color = WomanFitDarkViolet,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Progress Bar through full cycle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF3E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(WomanFitRose, WomanFitViolet)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Menstrual",
                                fontSize = 11.sp,
                                color = if (cycleStatus.phase == HormoneCyclePhase.MENSTRUAL) WomanFitRose else Color.Gray,
                                fontWeight = if (cycleStatus.phase == HormoneCyclePhase.MENSTRUAL) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "Follicular",
                                fontSize = 11.sp,
                                color = if (cycleStatus.phase == HormoneCyclePhase.FOLLICULAR) WomanFitRose else Color.Gray,
                                fontWeight = if (cycleStatus.phase == HormoneCyclePhase.FOLLICULAR) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "Ovulatory",
                                fontSize = 11.sp,
                                color = if (cycleStatus.phase == HormoneCyclePhase.OVULATORY) WomanFitRose else Color.Gray,
                                fontWeight = if (cycleStatus.phase == HormoneCyclePhase.OVULATORY) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "Luteal",
                                fontSize = 11.sp,
                                color = if (cycleStatus.phase == HormoneCyclePhase.LUTEAL) WomanFitRose else Color.Gray,
                                fontWeight = if (cycleStatus.phase == HormoneCyclePhase.LUTEAL) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WomanFitLightRose),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🌸", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Set Up Your Cycle Sync",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Log when your last period started to calculate your hormonal phase and unlock workouts matched to your energy level.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showEditCycleDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose),
                            modifier = Modifier.fillMaxWidth().testTag("setup_cycle_btn")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set Last Period Date", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Phase Explorer Selector Chips
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Explore Hormone Phases",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(HormoneCyclePhase.values()) { phase ->
                        val isSelected = selectedPhaseView == phase
                        val isCurrent = cycleStatus.phase == phase

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPhaseView = phase },
                            label = {
                                Text(
                                    text = "${phase.iconEmoji} ${phase.phaseName}${if (isCurrent) " (Today)" else ""}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WomanFitViolet,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("cycle_chip_${phase.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Selected Phase Deep Dive Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedPhaseView.phaseName} Deep Dive",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WomanFitLightRose
                        ) {
                            Text(
                                text = selectedPhaseView.getDayRangeDescription(user.cycleLengthDays),
                                color = WomanFitDeepRose,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hormonal Biology Info Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFBF8FA),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🧬 ", fontSize = 14.sp)
                                Text(
                                    text = "Hormone Biology:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WomanFitViolet
                                    )
                                )
                            }
                            Text(
                                text = selectedPhaseView.hormoneStatus,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚡ ", fontSize = 14.sp)
                                Text(
                                    text = "Energy Profile: ",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WomanFitRose
                                    )
                                )
                                Text(
                                    text = selectedPhaseView.energyLevel,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Nutrition Guidance
                    Text(
                        text = "🥗 Phase Nutrition Strategy",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedPhaseView.nutritionGuidance,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Suggested Workout Box with 1-Tap Launcher
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = WomanFitLightRose,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "💪 Synced Workout Recommendation",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = WomanFitDeepRose,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = recommendedWorkout.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "⏱ ${recommendedWorkout.durationMinutes}m • 🔥 ${recommendedWorkout.estimatedCalories} kcal",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { viewModel.startWorkout(recommendedWorkout) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("launch_cycle_workout_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Phase Workout", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Wellness Guidance & Medical Disclaimer
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFFBEB),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(text = "💡 ", fontSize = 12.sp)
                                    Text(
                                        text = "Wellness Guidance & Disclaimer: Cycle-synced suggestions are based on general hormonal science and estimation, not medical diagnosis. Ovulation timing naturally varies across individuals and cycles. This feature is designed for fitness intuition only and must NOT be used for contraception, birth control, or pregnancy planning. Always listen to your body and select whatever movement feels best for you today.",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF92400E),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { viewModel.selectTab(com.example.ui.MainTab.WORKOUTS) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Or Choose Any of the 6 Workouts Freely", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Cycle Dialog
    if (showEditCycleDialog) {
        AlertDialog(
            onDismissRequest = { showEditCycleDialog = false },
            title = { Text("Log Period & Cycle Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Customize your cycle parameters so WomanFit can personalize your workouts and nutrition recommendations.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    OutlinedTextField(
                        value = inputDaysAgo,
                        onValueChange = { inputDaysAgo = it },
                        label = { Text("How many days ago did your period start?") },
                        placeholder = { Text("e.g. 0 for today, 7 for 7 days ago") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputCycleLength,
                        onValueChange = { inputCycleLength = it },
                        label = { Text("Average Cycle Length (days)") },
                        placeholder = { Text("e.g. 28") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val daysAgo = inputDaysAgo.toLongOrNull() ?: 0L
                        val length = inputCycleLength.toIntOrNull() ?: 28
                        val newTimestamp = System.currentTimeMillis() - (daysAgo * 86400000L)

                        viewModel.updateProfile(
                            user.copy(
                                cycleLengthDays = length.coerceIn(21, 45),
                                lastPeriodStartTimestamp = newTimestamp
                            )
                        )
                        showEditCycleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose)
                ) {
                    Text("Save & Sync Cycle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCycleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
