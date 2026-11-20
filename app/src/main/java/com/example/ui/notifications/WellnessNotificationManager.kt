package com.example.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object WellnessNotificationManager {

    const val CHANNEL_ID = "womanfit_wellness_channel"
    const val CHANNEL_NAME = "WomanFit Wellness & Hydration"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Daily cycle, hydration, and workout reminders for WomanFit"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun sendHydrationAlert(context: Context, remainingMl: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle("💧 Hydration Check-In")
            .setContentText("Keep your skin glowing and hormones balanced! You have $remainingMl ml remaining today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(1001, builder.build())
    }

    fun sendWorkoutNudge(context: Context, workoutTitle: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle("🌸 Time to Glow: $workoutTitle")
            .setContentText("15 minutes of mindful movement lowers cortisol and boosts endorphins!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(1002, builder.build())
    }

    fun schedulePeriodicReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return

        // 2-hour repeating hydration reminder
        val hydrationIntent = Intent(context, WellnessAlarmReceiver::class.java).apply {
            action = WellnessAlarmReceiver.ACTION_HYDRATION_REMINDER
        }
        val hydrationPendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            hydrationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val twoHoursMillis = 2 * 60 * 60 * 1000L
        val triggerHydration = android.os.SystemClock.elapsedRealtime() + twoHoursMillis
        alarmManager.setInexactRepeating(
            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerHydration,
            twoHoursMillis,
            hydrationPendingIntent
        )

        // Morning 8:00 AM workout reminder
        val morningIntent = Intent(context, WellnessAlarmReceiver::class.java).apply {
            action = WellnessAlarmReceiver.ACTION_MORNING_WORKOUT
        }
        val morningPendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, 8)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarmManager.setInexactRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            android.app.AlarmManager.INTERVAL_DAY,
            morningPendingIntent
        )
    }

    fun cancelPeriodicReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return

        val hydrationIntent = Intent(context, WellnessAlarmReceiver::class.java).apply {
            action = WellnessAlarmReceiver.ACTION_HYDRATION_REMINDER
        }
        val hydrationPendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            hydrationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(hydrationPendingIntent)

        val morningIntent = Intent(context, WellnessAlarmReceiver::class.java).apply {
            action = WellnessAlarmReceiver.ACTION_MORNING_WORKOUT
        }
        val morningPendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(morningPendingIntent)
    }
}
