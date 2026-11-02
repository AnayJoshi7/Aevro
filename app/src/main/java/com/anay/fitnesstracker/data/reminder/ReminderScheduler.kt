package com.anay.fitnesstracker.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.anay.fitnesstracker.data.ReminderSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    // Lenient formatter to parse single-digit hours and case-insensitive AM/PM
    private val TIME_FORMATTER = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("[h:mm a][hh:mm a][H:mm][HH:mm]")
        .toFormatter(Locale.US)

    private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    fun parseTime(timeStr: String): LocalTime {
        val clean = timeStr.replace("\u00A0", " ").trim()
        return try {
            LocalTime.parse(clean, TIME_FORMATTER)
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing '$timeStr', fallback to 08:00 AM", e)
            LocalTime.of(8, 0)
        }
    }

    fun formatTime(time: LocalTime): String {
        return time.format(DISPLAY_FORMATTER)
    }

    fun getNextReminderTime(settings: ReminderSettings): String {
        if (!settings.waterReminderEnabled) return "Disabled"

        val now = LocalTime.now()
        val wake = parseTime(settings.wakeUpTime)
        val bed = parseTime(settings.bedtime)
        val interval = settings.intervalMins.toLong().coerceAtLeast(15)

        return if (now.isBefore(wake)) {
            formatTime(wake)
        } else if (now.isAfter(bed)) {
            "${formatTime(wake)} (Tomorrow)"
        } else {
            var step = wake
            while (!step.isAfter(bed) && !step.isAfter(now)) {
                step = step.plusMinutes(interval)
            }
            if (!step.isAfter(bed)) {
                formatTime(step)
            } else {
                "${formatTime(wake)} (Tomorrow)"
            }
        }
    }

    fun scheduleWaterReminders(context: Context, settings: ReminderSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_TRIGGER_WATER
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
            Log.d(TAG, "Water reminder cancelled.")
            return
        }

        val now = LocalDateTime.now()
        val wake = parseTime(settings.wakeUpTime)
        val bed = parseTime(settings.bedtime)
        val interval = settings.intervalMins.toLong().coerceAtLeast(15)

        var triggerDateTime: LocalDateTime

        if (now.toLocalTime().isBefore(wake)) {
            // Today at wake-up time
            triggerDateTime = LocalDateTime.of(LocalDate.now(), wake)
        } else if (now.toLocalTime().isAfter(bed)) {
            // Tomorrow at wake-up time
            triggerDateTime = LocalDateTime.of(LocalDate.now().plusDays(1), wake)
        } else {
            // In-between: step from wake-up until the first point strictly in the future
            var step = LocalDateTime.of(LocalDate.now(), wake)
            while (!step.toLocalTime().isAfter(bed) && !step.isAfter(now)) {
                step = step.plusMinutes(interval)
            }
            triggerDateTime = if (!step.toLocalTime().isAfter(bed)) {
                step
            } else {
                LocalDateTime.of(LocalDate.now().plusDays(1), wake)
            }
        }

        val triggerEpoch = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        Log.d(TAG, "Scheduling next water reminder for: $triggerDateTime ($triggerEpoch)")

        setAlarm(alarmManager, triggerEpoch, pendingIntent)
    }

    fun scheduleWorkoutReminder(context: Context, enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_TRIGGER_WORKOUT
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

        var triggerTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 0))
        if (LocalDateTime.now().isAfter(triggerTime)) {
            triggerTime = triggerTime.plusDays(1)
        }

        val triggerEpoch = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setAlarm(alarmManager, triggerEpoch, pendingIntent)
    }

    fun scheduleWorkoutSnooze(context: Context, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_TRIGGER_WORKOUT
            putExtra(WaterReminderReceiver.EXTRA_NOTIFICATION_TYPE, WaterReminderReceiver.TYPE_WORKOUT)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerEpoch = System.currentTimeMillis() + (minutes * 60 * 1000L)
        setAlarm(alarmManager, triggerEpoch, pendingIntent)
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerEpoch: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
                } else {
                    // Fallback to inexact if exact alarm permission was denied by the user
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm permission missing, falling back", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
        }
    }
}