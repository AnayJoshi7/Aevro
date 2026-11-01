package com.anay.fitnesstracker.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.data.SessionManager
import com.anay.fitnesstracker.data.WaterLogEntry
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class WaterReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "fitness_tracker_reminders"
        const val CHANNEL_NAME = "Daily Reminders"

        const val ACTION_LOG_WATER = "com.anay.fitnesstracker.ACTION_LOG_WATER"
        const val ACTION_SNOOZE_WORKOUT = "com.anay.fitnesstracker.ACTION_SNOOZE_WORKOUT"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"

        const val TYPE_WATER = "type_water"
        const val TYPE_WORKOUT = "type_workout"

        const val NOTIFICATION_ID_WATER = 1001
        const val NOTIFICATION_ID_WORKOUT = 1002
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        when (intent.action) {
            ACTION_LOG_WATER -> {
                val amountMl = intent.getIntExtra(EXTRA_AMOUNT_ML, 250)
                val sessionManager = SessionManager(context)
                val username = sessionManager.getUsername()?.trim()?.lowercase()

                if (!username.isNullOrBlank()) {
                    val firestore = FirebaseFirestore.getInstance()
                    val newEntry = mapOf(
                        "timestamp" to System.currentTimeMillis(),
                        "amountMl" to amountMl
                    )
                    firestore.collection("users").document(username)
                        .update("waterLogs", FieldValue.arrayUnion(newEntry))
                }

                // Dismiss notification once quick-logged
                notificationManager.cancel(NOTIFICATION_ID_WATER)
            }

            ACTION_SNOOZE_WORKOUT -> {
                notificationManager.cancel(NOTIFICATION_ID_WORKOUT)
                ReminderScheduler.scheduleWorkoutSnooze(context, 60)
            }

            else -> {
                val type = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) ?: TYPE_WATER
                if (type == TYPE_WATER) {
                    showWaterNotification(context, notificationManager)
                    // Auto-schedule the next interval
                    ReminderScheduler.rescheduleNextWaterReminder(context)
                } else if (type == TYPE_WORKOUT) {
                    showWorkoutNotification(context, notificationManager)
                }
            }
        }
    }

    private fun showWaterNotification(context: Context, manager: NotificationManager) {
        val action200 = createWaterAction(context, 200, 1)
        val action300 = createWaterAction(context, 300, 2)
        val action500 = createWaterAction(context, 500, 3)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("Time to drink water 💧")
            .setContentText("Stay hydrated! Tap to log your intake directly:")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(action200)
            .addAction(action300)
            .addAction(action500)
            .build()

        manager.notify(NOTIFICATION_ID_WATER, notification)
    }

    private fun showWorkoutNotification(context: Context, manager: NotificationManager) {
        val snoozeIntent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_WORKOUT
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            10,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeAction = NotificationCompat.Action.Builder(
            0,
            "Remind me in 1 hr",
            snoozePendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("Start your workout 🏋️")
            .setContentText("Your scheduled workout session is waiting for you!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(snoozeAction)
            .build()

        manager.notify(NOTIFICATION_ID_WORKOUT, notification)
    }

    private fun createWaterAction(context: Context, amountMl: Int, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_LOG_WATER
            putExtra(EXTRA_AMOUNT_ML, amountMl)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(
            0,
            "+${amountMl}ml",
            pendingIntent
        ).build()
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Hydration and workout reminders"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}