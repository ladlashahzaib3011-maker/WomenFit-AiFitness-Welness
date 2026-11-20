package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.AchievementBadge
import com.example.model.ActivityRecord
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*

@Composable
fun ProgressScreen(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.userProfile.collectAsState()
    val weightLogs by viewModel.weightLogs.collectAsState()
    val activityRecords by viewModel.activityRecords.collectAsState()
    val badges by viewModel.badges.collectAsState()

    var showLogWeightDialog by remember { mutableStateOf(false) }
    var inputWeight by remember { mutableStateOf(user.currentWeightKg.toString()) }
    var inputWaist by remember { mutableStateOf("70") }
    var inputHips by remember { mutableStateOf("97") }
    var inputNote by remember { mutableStateOf("") }

    val weeklyCalories = listOf(
        Pair("Mon", 340),
        Pair("Tue", 410),
        Pair("Wed", 290),
        Pair("Thu", 450),
        Pair("Fri", 380),
        Pair("Sat", 420),
        Pair("Sun", 320)
    )
    val maxCal = 500f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Screen Title
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Fitness Progress & Analytics",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Weekly calories, body measurements & unlocked trophies",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Weekly Calorie Burn Bar Chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Weekly Burn (kcal)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "2,610 kcal burned this week",
                                style = MaterialTheme.typography.bodySmall.copy(color = WomanFitRose, fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WomanFitLightRose
                        ) {
                            Text(
                                text = "Avg 372 / day",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WomanFitDeepRose,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyCalories.forEachIndexed { index, (day, cals) ->
                            val heightFraction = (cals / maxCal).coerceIn(0.15f, 1f)
                            val isToday = index == 6

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$cals",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) WomanFitRose else Color.Gray
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isToday) {
                                                Brush.verticalGradient(listOf(WomanFitRose, WomanFitDeepRose))
                                            } else {
                                                Brush.verticalGradient(listOf(Color(0xFFDDD6FE), Color(0xFFC4B5FD)))
                                            }
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) WomanFitRose else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Weight & Body Measurements Section
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
                        Column {
                            Text(
                                text = "Weight & Measurements",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Current: ${user.currentWeightKg} kg • Target: ${user.targetWeightKg} kg",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Button(
                            onClick = { showLogWeightDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("log_new_weight_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Weight", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress to Target Weight
                    val diff = (user.currentWeightKg - user.targetWeightKg)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WomanFitLightRose,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (diff > 0) "${"%.1f".format(diff)} kg to reach goal!" else "Goal Accomplished! 🎉",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = WomanFitDeepRose
                                )
                            )
                            Text(
                                text = "-1.3 kg this month",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WomanFitViolet,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recent Weight Entries List
                    weightLogs.take(3).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${entry.dateString}: ${entry.weightKg} kg",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (entry.waistCm != null && entry.hipsCm != null) {
                                    Text(
                                        text = "Waist: ${entry.waistCm}cm • Hips: ${entry.hipsCm}cm",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                    )
                                }
                            }
                            if (entry.note.isNotBlank()) {
                                Text(
                                    text = entry.note,
                                    style = MaterialTheme.typography.labelSmall.copy(color = WomanFitRose)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF3E8F0), thickness = 0.5.dp)
                    }
                }
            }
        }

        // Achievement Badges Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Achievement Badges",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    badges.chunked(2).forEach { rowBadges ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBadges.forEach { badge ->
                                BadgeCard(badge = badge, modifier = Modifier.weight(1f))
                            }
                            if (rowBadges.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Workout Activity History
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Workout History",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (activityRecords.isEmpty()) {
                            Text(
                                text = "No workouts recorded yet. Start your first session!",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                modifier = Modifier.padding(10.dp)
                            )
                        } else {
                            activityRecords.forEach { record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.workoutTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = record.category,
                                            style = MaterialTheme.typography.labelSmall.copy(color = WomanFitViolet)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "⏱ ${record.durationMinutes}m",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = WomanFitRose
                                        )
                                        Text(
                                            text = "🔥 ${record.caloriesBurned} kcal",
                                            fontSize = 11.sp,
                                            color = Color.Gray
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
    }

    // Log Weight Dialog
    if (showLogWeightDialog) {
        AlertDialog(
            onDismissRequest = { showLogWeightDialog = false },
            title = { Text("Log Body Check-In", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputWeight,
                        onValueChange = { inputWeight = it },
                        label = { Text("Current Weight (kg)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputWaist,
                        onValueChange = { inputWaist = it },
                        label = { Text("Waist circumference (cm) - optional") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputHips,
                        onValueChange = { inputHips = it },
                        label = { Text("Hips circumference (cm) - optional") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputNote,
                        onValueChange = { inputNote = it },
                        label = { Text("Note / How do you feel?") },
                        placeholder = { Text("e.g. Waist feels tighter!") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = inputWeight.toFloatOrNull() ?: user.currentWeightKg
                        val waist = inputWaist.toFloatOrNull()
                        val hips = inputHips.toFloatOrNull()
                        viewModel.logWeight(w, waist, hips, inputNote)
                        showLogWeightDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose)
                ) {
                    Text("Save Check-In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogWeightDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BadgeCard(badge: AchievementBadge, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surface else Color(0xFFF8F5F7)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (badge.isUnlocked) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (badge.isUnlocked) WomanFitLightRose else Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = badge.iconEmoji, fontSize = 20.sp)
            }

            Column {
                Text(
                    text = badge.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
                Text(
                    text = if (badge.isUnlocked) (badge.unlockedDate ?: "Unlocked") else "Locked",
                    fontSize = 10.sp,
                    color = if (badge.isUnlocked) WomanFitRose else Color.LightGray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
