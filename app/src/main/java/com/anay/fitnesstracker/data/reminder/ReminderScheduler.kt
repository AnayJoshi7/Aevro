package com.anay.fitnesstracker.data.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anay.fitnesstracker.data.ReminderSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReminderScheduler {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    fun parseTime(timeStr: String): LocalTime {
        return try {
            LocalTime.parse(timeStr.trim().uppercase(), TIME_FORMATTER)
        } catch (e: Exception) {
            LocalTime.of(8, 0)
        }
    }

    fun formatTime(time: LocalTime): String {
        return time.format(TIME_FORMATTER)
    }

    fun getNextReminderTime(settings: ReminderSettings): String {
        if (!settings.waterReminderEnabled) return "Disabled"

        val now = LocalTime.now()
        val wake = parseTime(settings.wakeUpTime)
        val bed = parseTime(settings.bedtime)
        val interval = settings.intervalMins.toLong()

        // If currently before wake-up, next reminder is wake-up time
        if (now.isBefore(wake)) {
            return formatTime(wake)
        }

        // If currently after bedtime, next reminder is tomorrow's wake-up time
        if (now.isAfter(bed)) {
            return "${formatTime(wake)} (Tomorrow)"
        }

        // Step from wake-up until the first slot after now
        var next = wake
        while (!next.isAfter(bed) && !next.isAfter(now)) {
            next = next.plusMinutes(interval)
        }

        return if (!next.isAfter(bed)) {
            formatTime(next)
        } else {
            "${formatTime(wake)} (Tomorrow)"
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleWaterReminders(context: Context, settings: ReminderSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            putExtra(WaterReminderReceiver.EXTRA_NOTIFICATION_TYPE, WaterReminderReceiver.TYPE_WATER)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!settings.waterReminderEnabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val now = LocalDateTime.now()
        val wake = parseTime(settings.wakeUpTime)
        val bed = parseTime(settings.bedtime)

        var nextTime = LocalDateTime.of(LocalDate.now(), wake)
        if (LocalTime.now().isAfter(wake)) {
            while (!nextTime.toLocalTime().isAfter(bed) && !nextTime.isAfter(now)) {
                nextTime = nextTime.plusMinutes(settings.intervalMins.toLong())
            }
            if (nextTime.toLocalTime().isAfter(bed)) {
                nextTime = LocalDateTime.of(LocalDate.now().plusDays(1), wake)
            }
        }

        val triggerEpoch = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
        }
    }

    fun rescheduleNextWaterReminder(context: Context) {
        val sessionManager = com.anay.fitnesstracker.data.SessionManager(context)
        val username = sessionManager.getUsername()?.trim()?.lowercase() ?: return

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(username).get()
            .addOnSuccessListener { doc ->
                val profile = doc.toObject(com.anay.fitnesstracker.data.UserProfile::class.java)
                if (profile != null && profile.reminderSettings.waterReminderEnabled) {
                    scheduleWaterReminders(context, profile.reminderSettings)
                }
            }
    }

    fun scheduleWorkoutReminder(context: Context, enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            putExtra(WaterReminderReceiver.EXTRA_NOTIFICATION_TYPE, WaterReminderReceiver.TYPE_WORKOUT)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        // Daily workout prompt at 08:00 AM
        var triggerTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 0))
        if (LocalDateTime.now().isAfter(triggerTime)) {
            triggerTime = triggerTime.plusDays(1)
        }

        val triggerEpoch = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerEpoch,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleWorkoutSnooze(context: Context, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            putExtra(WaterReminderReceiver.EXTRA_NOTIFICATION_TYPE, WaterReminderReceiver.TYPE_WORKOUT)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerEpoch = System.currentTimeMillis() + (minutes * 60 * 1000L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
        }
    }
}