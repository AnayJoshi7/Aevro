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
import java.time.temporal.ChronoUnit
import java.util.Locale

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"
    private const val PREFS_NAME = "water_reminder_local_prefs"

    private const val KEY_WATER_ENABLED = "water_enabled"
    private const val KEY_WAKE_TIME = "wake_time"
    private const val KEY_BED_TIME = "bed_time"
    private const val KEY_INTERVAL_MINS = "interval_mins"
    private const val KEY_WORKOUT_ENABLED = "workout_enabled"
    private const val KEY_NEXT_TRIGGER_EPOCH = "next_trigger_epoch"

    private val TIME_FORMATTER = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("[h:mm a][hh:mm a][H:mm][HH:mm]")
        .toFormatter(Locale.US)

    private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    fun saveSettingsLocally(context: Context, settings: ReminderSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean(KEY_WATER_ENABLED, settings.waterReminderEnabled)
            putString(KEY_WAKE_TIME, settings.wakeUpTime)
            putString(KEY_BED_TIME, settings.bedtime)
            putInt(KEY_INTERVAL_MINS, settings.intervalMins)
            putBoolean(KEY_WORKOUT_ENABLED, settings.workoutReminderEnabled)
            apply()
        }
    }

    fun loadSettingsLocally(context: Context): ReminderSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ReminderSettings(
            waterReminderEnabled = prefs.getBoolean(KEY_WATER_ENABLED, false),
            wakeUpTime = prefs.getString(KEY_WAKE_TIME, "05:00 AM") ?: "05:00 AM",
            bedtime = prefs.getString(KEY_BED_TIME, "10:00 PM") ?: "10:00 PM",
            intervalMins = prefs.getInt(KEY_INTERVAL_MINS, 30),
            workoutReminderEnabled = prefs.getBoolean(KEY_WORKOUT_ENABLED, false)
        )
    }

    fun getSavedNextTriggerEpoch(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_NEXT_TRIGGER_EPOCH, 0L)
    }

    fun parseTime(timeStr: String): LocalTime {
        val clean = timeStr.replace("\u00A0", " ").trim()
        return try {
            LocalTime.parse(clean, TIME_FORMATTER).truncatedTo(ChronoUnit.MINUTES)
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing '$timeStr', defaulting to 05:00 AM", e)
            LocalTime.of(5, 0)
        }
    }

    fun formatTime(time: LocalTime): String {
        return time.format(DISPLAY_FORMATTER)
    }

    /**
     * Computes the exact next LocalDateTime slot between wakeUpTime and bedtime.
     */
    fun computeNextTriggerDateTime(
        settings: ReminderSettings,
        referenceNow: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime {
        val wakeTime = parseTime(settings.wakeUpTime)
        val bedTime = parseTime(settings.bedtime)
        val interval = settings.intervalMins.toLong().coerceAtLeast(15L)

        // Helper to build a day's [wakeDateTime, bedDateTime] window
        fun buildWindowForDate(date: LocalDate): Pair<LocalDateTime, LocalDateTime> {
            val start = LocalDateTime.of(date, wakeTime)
            val end = if (bedTime.isAfter(wakeTime)) {
                LocalDateTime.of(date, bedTime)
            } else {
                // Handles overnight bedtimes (e.g., Wake 05:00 AM, Bed 12:30 AM next day)
                LocalDateTime.of(date.plusDays(1), bedTime)
            }
            return start to end
        }

        // Check yesterday's window first (in case bedtime crosses midnight into today)
        val (yStart, yEnd) = buildWindowForDate(referenceNow.toLocalDate().minusDays(1))
        if (!referenceNow.isAfter(yEnd) && !referenceNow.isBefore(yStart)) {
            var slot = yStart
            while (!slot.isAfter(yEnd)) {
                if (slot.isAfter(referenceNow)) return slot
                slot = slot.plusMinutes(interval)
            }
        }

        // Check today's window
        val today = referenceNow.toLocalDate()
        val (todayStart, todayEnd) = buildWindowForDate(today)

        if (referenceNow.isBefore(todayStart)) {
            return todayStart
        }

        if (!referenceNow.isAfter(todayEnd)) {
            var slot = todayStart
            while (!slot.isAfter(todayEnd)) {
                if (slot.isAfter(referenceNow)) return slot
                slot = slot.plusMinutes(interval)
            }
        }

        // Past today's bedtime -> Tomorrow's wake-up time
        return LocalDateTime.of(today.plusDays(1), wakeTime)
    }

    /**
     * Checks if the current clock time is sitting right on an active slot (within the first 59 seconds).
     * Useful when testing by manually setting the system clock to 05:00 AM.
     */
    fun isCurrentlyOnActiveSlot(
        settings: ReminderSettings,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        if (!settings.waterReminderEnabled) return false
        val wakeTime = parseTime(settings.wakeUpTime)
        val bedTime = parseTime(settings.bedtime)
        val interval = settings.intervalMins.toLong().coerceAtLeast(15L)

        val todayStart = LocalDateTime.of(now.toLocalDate(), wakeTime)
        val todayEnd = if (bedTime.isAfter(wakeTime)) {
            LocalDateTime.of(now.toLocalDate(), bedTime)
        } else {
            LocalDateTime.of(now.toLocalDate().plusDays(1), bedTime)
        }

        val nowTruncated = now.truncatedTo(ChronoUnit.MINUTES)
        if (nowTruncated.isBefore(todayStart) || nowTruncated.isAfter(todayEnd)) return false

        val minutesFromWake = ChronoUnit.MINUTES.between(todayStart, nowTruncated)
        return minutesFromWake >= 0 && (minutesFromWake % interval == 0L)
    }

    fun getNextReminderTime(settings: ReminderSettings): String {
        if (!settings.waterReminderEnabled) return "Disabled"
        val now = LocalDateTime.now()
        val nextDateTime = computeNextTriggerDateTime(settings, now)
        val formatted = formatTime(nextDateTime.toLocalTime())
        return if (nextDateTime.toLocalDate().isAfter(now.toLocalDate())) {
            "$formatted (Tomorrow)"
        } else {
            formatted
        }
    }

    fun scheduleWaterReminders(
        context: Context,
        settings: ReminderSettings,
        skipCurrentMinute: Boolean = false
    ) {
        // Persist locally immediately so BroadcastReceiver always has fresh settings offline
        saveSettingsLocally(context, settings)

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

        // Always cancel any stale alarm before setting a new one
        alarmManager.cancel(pendingIntent)

        if (!settings.waterReminderEnabled) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_NEXT_TRIGGER_EPOCH, 0L).apply()
            Log.d(TAG, "Water reminder disabled and cancelled.")
            return
        }

        // If called from onReceive(), advance reference time by 30 seconds so we never re-schedule the same minute
        val referenceNow = if (skipCurrentMinute) {
            LocalDateTime.now().plusSeconds(30)
        } else {
            LocalDateTime.now()
        }

        val triggerDateTime = computeNextTriggerDateTime(settings, referenceNow)
        val triggerEpoch = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_NEXT_TRIGGER_EPOCH, triggerEpoch).apply()

        Log.d(TAG, "Scheduled next water reminder for: $triggerDateTime ($triggerEpoch)")
        setAlarm(alarmManager, triggerEpoch, pendingIntent)
    }

    fun scheduleWorkoutReminder(context: Context, enabled: Boolean) {
        val current = loadSettingsLocally(context)
        saveSettingsLocally(context, current.copy(workoutReminderEnabled = enabled))

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

        alarmManager.cancel(pendingIntent)
        if (!enabled) return

        var triggerTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 0))
        if (!LocalDateTime.now().isBefore(triggerTime)) {
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
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException setting exact alarm, falling back", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
        }
    }
}