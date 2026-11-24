package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FitnessViewModel
import com.example.ui.components.CloudSyncBadge
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: FitnessViewModel,
    onSignOutConfirmed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val user by viewModel.userProfile.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Edit profile state fields
    var editName by remember { mutableStateOf(user.name) }
    var editAge by remember { mutableStateOf(user.age.toString()) }
    var editHeight by remember { mutableStateOf(user.heightCm.toString()) }
    var editWeight by remember { mutableStateOf(user.currentWeightKg.toString()) }
    var editTargetWeight by remember { mutableStateOf(user.targetWeightKg.toString()) }
    var editGoal by remember { mutableStateOf(user.fitnessGoal) }
    var soundEnabled by remember { mutableStateOf(user.soundEnabled) }
    var notificationsEnabled by remember { mutableStateOf(user.reminderNotifications) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Profile Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(WomanFitRose, WomanFitViolet))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.ifBlank { "W" }.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user.name.ifBlank { "WomanFit Member" },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )

                    if (user.email.isNotBlank()) {
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WomanFitLightRose
                    ) {
                        Text(
                            text = "👑 ${user.fitnessGoal} • ${user.fitnessLevel}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WomanFitDeepRose,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Supabase connection indicator ("● Cloud sync active")
                    CloudSyncBadge()

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            editName = user.name
                            editAge = user.age.toString()
                            editHeight = user.heightCm.toString()
                            editWeight = user.currentWeightKg.toString()
                            editTargetWeight = user.targetWeightKg.toString()
                            editGoal = user.fitnessGoal
                            showEditProfileDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Personal Stats")
                    }
                }
            }
        }

        // Stats Matrix
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Current", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text(text = "${user.currentWeightKg} kg", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Target", style = MaterialTheme.typography.labelSmall.copy(color = WomanFitRose))
                        Text(text = "${user.targetWeightKg} kg", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WomanFitRose)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Height", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text(text = "${user.heightCm.toInt()} cm", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Age", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text(text = "${user.age} yrs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Settings & App Preferences
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Preferences & App Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Workout Audio Chimes & Haptics", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(text = "Timer countdown sounds & interval beeps", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = {
                                    soundEnabled = it
                                    viewModel.updateProfile(user.copy(soundEnabled = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WomanFitRose)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF3E8F0), thickness = 0.5.dp)

                        val context = androidx.compose.ui.platform.LocalContext.current
                        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            notificationsEnabled = isGranted
                            viewModel.updateProfile(user.copy(reminderNotifications = isGranted))
                            if (isGranted) {
                                com.example.ui.notifications.WellnessNotificationManager.schedulePeriodicReminders(context)
                                com.example.ui.notifications.WellnessNotificationManager.sendHydrationAlert(context, 1250)
                            } else {
                                com.example.ui.notifications.WellnessNotificationManager.cancelPeriodicReminders(context)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Daily Hydration & Workout Reminders", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(text = "Scheduled 8:00 AM workout & 2-hr hydration alerts", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            notificationsEnabled = true
                                            viewModel.updateProfile(user.copy(reminderNotifications = true))
                                            com.example.ui.notifications.WellnessNotificationManager.schedulePeriodicReminders(context)
                                        }
                                    } else {
                                        notificationsEnabled = false
                                        viewModel.updateProfile(user.copy(reminderNotifications = false))
                                        com.example.ui.notifications.WellnessNotificationManager.cancelPeriodicReminders(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WomanFitRose)
                            )
                        }

                        if (notificationsEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    com.example.ui.notifications.WellnessNotificationManager.sendHydrationAlert(context, 1250)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Test Hydration Alert", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Clear Sign Out Action at bottom of profile
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("sign_out_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEE2E2),
                        contentColor = Color(0xFFDC2626)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign out of WomanFit?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "You will need to sign in again to access your fitness records, cycle calendar, and Coach Maya.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOutConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                    modifier = Modifier.testTag("cancel_sign_out_button")
                ) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        val goalsList = listOf("Tone & Sculpt", "Weight Loss", "Hormone Health & Stress Relief", "Strength & Glutes")
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Personal Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editAge,
                            onValueChange = { editAge = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Age") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editHeight,
                            onValueChange = { editHeight = it },
                            label = { Text("Height (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editWeight,
                            onValueChange = { editWeight = it },
                            label = { Text("Current (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editTargetWeight,
                            onValueChange = { editTargetWeight = it },
                            label = { Text("Target (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Primary Goal:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        goalsList.forEach { g ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = editGoal == g,
                                    onClick = { editGoal = g },
                                    colors = RadioButtonDefaults.colors(selectedColor = WomanFitRose)
                                )
                                Text(text = g, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ageVal = editAge.toIntOrNull() ?: user.age
                        val heightVal = editHeight.toFloatOrNull() ?: user.heightCm
                        val weightVal = editWeight.toFloatOrNull() ?: user.currentWeightKg
                        val targetVal = editTargetWeight.toFloatOrNull() ?: user.targetWeightKg

                        viewModel.updateProfile(
                            user.copy(
                                name = editName,
                                age = ageVal,
                                heightCm = heightVal,
                                currentWeightKg = weightVal,
                                targetWeightKg = targetVal,
                                fitnessGoal = editGoal
                            )
                        )
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
