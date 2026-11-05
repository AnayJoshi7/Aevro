package com.anay.fitnesstracker.data.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anay.fitnesstracker.MainActivity
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.data.SessionManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime

class WaterReminderReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "WaterReminderReceiver"
        const val CHANNEL_ID = "fitness_tracker_reminders_v2"
        const val CHANNEL_NAME = "Hydration & Workout Reminders"

        const val ACTION_TRIGGER_WATER = "com.anay.fitnesstracker.ACTION_TRIGGER_WATER"
        const val ACTION_TRIGGER_WORKOUT = "com.anay.fitnesstracker.ACTION_TRIGGER_WORKOUT"
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
        val action = intent.action
        Log.d(TAG, "onReceive triggered with action: $action")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val localSettings = ReminderScheduler.loadSettingsLocally(context)

        when (action) {
            // 1. System Time / Date / Timezone / Boot changed
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                if (localSettings.waterReminderEnabled) {
                    val now = LocalDateTime.now()
                    if (action == Intent.ACTION_TIME_CHANGED &&
                        ReminderScheduler.isCurrentlyOnActiveSlot(localSettings, now)
                    ) {
                        // User just set the clock to an exact slot (e.g., 05:00 AM) -> fire notification & schedule next!
                        showWaterNotification(context, notificationManager)
                        ReminderScheduler.scheduleWaterReminders(context, localSettings, skipCurrentMinute = true)
                    } else {
                        // Recalculate alarm for the new date/time so it never gets stranded in the future
                        ReminderScheduler.scheduleWaterReminders(context, localSettings, skipCurrentMinute = false)
                    }
                }
                if (localSettings.workoutReminderEnabled) {
                    ReminderScheduler.scheduleWorkoutReminder(context, true)
                }
            }

            // 2. Quick-log pill clicked (+200ml, +300ml, +500ml)
            ACTION_LOG_WATER -> {
                val amountMl = intent.getIntExtra(EXTRA_AMOUNT_ML, 250)
                notificationManager.cancel(NOTIFICATION_ID_WATER)

                val session = SessionManager(context)
                val username = session.getUsername()?.trim()?.lowercase()
                if (!username.isNullOrBlank()) {
                    val newEntry = mapOf(
                        "timestamp" to System.currentTimeMillis(),
                        "amountMl" to amountMl
                    )
                    // Writes immediately to local Firestore SQLite cache & syncs when online
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(username)
                        .update("waterLogs", FieldValue.arrayUnion(newEntry))
                }
            }

            // 3. Snooze Workout clicked
            ACTION_SNOOZE_WORKOUT -> {
                notificationManager.cancel(NOTIFICATION_ID_WORKOUT)
                ReminderScheduler.scheduleWorkoutSnooze(context, 60)
            }

            // 4. Daily Workout Reminder triggered
            ACTION_TRIGGER_WORKOUT -> {
                showWorkoutNotification(context, notificationManager)
                if (localSettings.workoutReminderEnabled) {
                    ReminderScheduler.scheduleWorkoutReminder(context, true)
                }
            }

            // 5. Water Reminder Alarm triggered
            ACTION_TRIGGER_WATER -> {
                if (!localSettings.waterReminderEnabled) return

                // Display notification
                showWaterNotification(context, notificationManager)

                // Immediately schedule the next slot synchronously from local settings (0 network delay)
                ReminderScheduler.scheduleWaterReminders(
                    context = context,
                    settings = localSettings,
                    skipCurrentMinute = true
                )
            }
        }
    }

    private fun showWaterNotification(context: Context, manager: NotificationManager) {
        // Cancel existing notification first so the new one always alerts with sound & heads-up banner
        manager.cancel(NOTIFICATION_ID_WATER)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val action200 = createWaterAction(context, 200, 1)
        val action300 = createWaterAction(context, 300, 2)
        val action500 = createWaterAction(context, 500, 3)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("Time to drink water 💧")
            .setContentText("Stay hydrated! Tap a pill below to log directly:")
            .setContentIntent(tapPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .addAction(action200)
            .addAction(action300)
            .addAction(action500)
            .build()

        manager.notify(NOTIFICATION_ID_WATER, notification)
    }

    private fun showWorkoutNotification(context: Context, manager: NotificationManager) {
        manager.cancel(NOTIFICATION_ID_WORKOUT)

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
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(false)
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
                description = "Hydration and workout alerts"
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}