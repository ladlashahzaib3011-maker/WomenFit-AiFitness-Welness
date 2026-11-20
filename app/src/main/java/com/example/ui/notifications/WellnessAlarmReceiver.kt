package com.example.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class WellnessAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Device rebooted or app updated: restore scheduled reminders if enabled
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val user = db.userDao().getUserDirect()
                        // Default to enabled if user hasn't explicitly disabled
                        val isEnabled = user?.reminderNotifications ?: true
                        if (isEnabled) {
                            WellnessNotificationManager.schedulePeriodicReminders(context)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_HYDRATION_REMINDER -> {
                // Enforce Quiet Hours: Only notify during waking hours (8:00 AM to 9:00 PM)
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (currentHour in WAKING_START_HOUR until WAKING_END_HOUR) {
                    WellnessNotificationManager.sendHydrationAlert(context, 1250)
                }
            }

            ACTION_MORNING_WORKOUT -> {
                WellnessNotificationManager.sendWorkoutNudge(context, "Morning Hormone Harmony Workout")
            }
        }
    }

    companion object {
        const val ACTION_HYDRATION_REMINDER = "com.example.womanfit.ACTION_HYDRATION_REMINDER"
        const val ACTION_MORNING_WORKOUT = "com.example.womanfit.ACTION_MORNING_WORKOUT"

        // Waking hours: 8:00 AM (8) up to 9:00 PM (21)
        const val WAKING_START_HOUR = 8
        const val WAKING_END_HOUR = 21
    }
}
