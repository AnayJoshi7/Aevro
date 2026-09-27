package com.anay.fitnesstracker.data

import com.anay.fitnesstracker.data.WorkoutScheduleHelper.WorkoutScheduleHelper.getTodayWorkout
import com.anay.fitnesstracker.data.WorkoutScheduleHelper.getTodayWorkout
import kotlin.math.roundToInt
import java.time.DayOfWeek
import java.time.LocalDate
import com.anay.fitnesstracker.Routes


object FitnessCalculator {
    const val CURRENT_YEAR = 2026

    fun generateUsername(name: String, birthYearStr: String, contactNo: String): String {
        val initials = name.trim().take(2).uppercase().padEnd(2, 'X')
        val cleanYear = birthYearStr.filter { it.isDigit() }.takeLast(4).ifEmpty { "2000" }
        val last4Digits = contactNo.filter { it.isDigit() }.takeLast(4).ifEmpty { "0000" }
        return "$initials$cleanYear$last4Digits"
    }

    fun calculateBmi(heightCm: Double, weightKg: Double): Pair<Double, String> {
        if (heightCm <= 0.0) return 0.0 to "Invalid"
        val heightM = heightCm / 100.0
        val bmi = (weightKg / (heightM * heightM) * 10.0).roundToInt() / 10.0
        val category = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
        return bmi to category
    }
    fun getTodayTargetExerciseCount(workoutFrequency: String, splitName: String): Int {
        val info = getTodayWorkout(workoutFrequency, splitName)
        return when (info.title) {
            "Push Day" -> 8
            "Pull Day" -> 9
            "Leg Day" -> 8
            "Upper Body" -> 9
            "Lower Body" -> 7
            "Full Body" -> 9
            else -> 0 // Rest Day
        }
    }


    fun calculateMaintenanceCalories(
        gender: String,
        birthYear: Int,
        heightCm: Double,
        weightKg: Double
    ): Int {
        val age = (CURRENT_YEAR - birthYear).coerceAtLeast(10)
        val bmr = if (gender.trim().equals("Female", ignoreCase = true)) {
            (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
        } else {
            (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
        }
        return (bmr * 1.4).roundToInt()
    }

    fun calculateTargets(maintenanceCalories: Int, weightKg: Double, goal: String): Pair<Int, Int> {
        return when (goal) {
            "Lose Weight" -> Pair(
                (maintenanceCalories - 500).coerceAtLeast(1200),
                (weightKg * 2.0).roundToInt()
            )

            "Gain Weight" -> Pair(
                maintenanceCalories + 400,
                (weightKg * 1.8).roundToInt()
            )

            else -> Pair(
                maintenanceCalories,
                (weightKg * 1.6).roundToInt()
            )
        }
    }

    fun getSplitForFrequency(frequency: String): String {
        return when (frequency) {
            "3 Days a Week" -> "Full Body"
            "4 Days a Week" -> "Upper Body, Lower Body"
            "5 Days a Week" -> "Upper, Lower + Push Pull Legs"
            "6 Days a Week" -> "Push Pull Legs"
            else -> "Push Pull Legs"
        }
    }
}
data class TodayWorkoutInfo(
    val title: String,
    val muscleGroups: String,
    val exerciseCountText: String,
    val route: String? = null
)

object WorkoutScheduleHelper {
    fun getTodayWorkout(user: UserProfile?): TodayWorkoutInfo {
        val dayOfWeek =
            java.time.LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

        // If user defined a custom schedule for today
        val customDay =
            user?.customSchedule?.find { it.dayName.equals(dayOfWeek, ignoreCase = true) }
        if (customDay != null && customDay.muscleGroup.isNotBlank()) {
            val count = customDay.exercises.size
            return TodayWorkoutInfo(
                title = customDay.muscleGroup,
                muscleGroups = customDay.muscleGroup,
                exerciseCountText = "$count Exercises",
                route = Routes.LOG_WORKOUT
            )
        }

        // Fallback to preset split calculation
        val info = getTodayWorkout(
            user?.workoutFrequency ?: "6 Days a Week",
            user?.workoutSplit ?: "Push Pull Legs"
        )
        return TodayWorkoutInfo(
            title = info.title,
            muscleGroups = info.muscleGroups,
            exerciseCountText = info.exerciseCountText,
            route = info.route
        )
    }

    fun getTodayTargetExerciseCount(user: UserProfile?): Int {
        val dayOfWeek =
            java.time.LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val customDay =
            user?.customSchedule?.find { it.dayName.equals(dayOfWeek, ignoreCase = true) }
        if (customDay != null && customDay.muscleGroup.isNotBlank()) {
            return customDay.exercises.size
        }

        val info = getTodayWorkout(
            user?.workoutFrequency ?: "6 Days a Week",
            user?.workoutSplit ?: "Push Pull Legs"
        )
        return when (info.title) {
            "Push Day" -> 8
            "Pull Day" -> 9
            "Leg Day" -> 8
            "Upper Body" -> 9
            "Lower Body" -> 7
            "Full Body" -> 9
            else -> 0
        }
    }

    data class WorkoutDayInfo(
        val title: String,
        val muscleGroups: String,
        val exerciseCountText: String,
        val route: String?
    )

    object WorkoutScheduleHelper {
        fun getTodayWorkout(workoutFrequency: String, splitName: String): WorkoutDayInfo {
            val today = LocalDate.now().dayOfWeek


            return when (workoutFrequency) {
                "3 Days a Week" -> { // Full Body schedule: Mon, Wed, Fri (Tue, Thu, Sat, Sun Rest)
                    when (today) {
                        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY ->
                            WorkoutDayInfo(
                                "Full Body",
                                "Full Body Compound",
                                "9 Exercises",
                                "workout_full_body"
                            )

                        else ->
                            WorkoutDayInfo(
                                "No workout today",
                                "Rest and recover",
                                "0 Exercises",
                                null
                            )
                    }
                }

                "4 Days a Week" -> { // Upper/Lower: Mon (Upper), Tue (Lower), Thu (Upper), Fri (Lower)
                    when (today) {
                        DayOfWeek.MONDAY, DayOfWeek.THURSDAY ->
                            WorkoutDayInfo(
                                "Upper Body",
                                "Chest, Back, Arms, Shoulders",
                                "9 Exercises",
                                "workout_upper_body"
                            )

                        DayOfWeek.TUESDAY, DayOfWeek.FRIDAY ->
                            WorkoutDayInfo(
                                "Lower Body",
                                "Quads, Hamstrings, Calves, Abs",
                                "7 Exercises",
                                "workout_lower_body"
                            )

                        else ->
                            WorkoutDayInfo(
                                "No workout today",
                                "Rest and recover",
                                "0 Exercises",
                                null
                            )
                    }
                }

                "5 Days a Week" -> { // Mon: Upper, Tue: Lower, Wed: Rest, Thu: Push, Fri: Pull, Sat: Legs
                    when (today) {
                        DayOfWeek.MONDAY -> WorkoutDayInfo(
                            "Upper Body",
                            "Chest, Back, Arms",
                            "9 Exercises",
                            "workout_upper_body"
                        )

                        DayOfWeek.TUESDAY -> WorkoutDayInfo(
                            "Lower Body",
                            "Legs and Core",
                            "7 Exercises",
                            "workout_lower_body"
                        )

                        DayOfWeek.THURSDAY -> WorkoutDayInfo(
                            "Push Day",
                            "Chest, Shoulder, Triceps",
                            "7 Exercises",
                            "workout_push_day"
                        )

                        DayOfWeek.FRIDAY -> WorkoutDayInfo(
                            "Pull Day",
                            "Back, Biceps, Forearms",
                            "8 Exercises",
                            "workout_pull_day"
                        )

                        DayOfWeek.SATURDAY -> WorkoutDayInfo(
                            "Leg Day",
                            "Legs, Abs",
                            "8 Exercises",
                            "workout_leg_day"
                        )

                        else -> WorkoutDayInfo(
                            "No workout today",
                            "Rest and recover",
                            "0 Exercises",
                            null
                        )
                    }
                }

                else -> { // 6 Days a Week (Push Pull Legs): Mon/Thu (Push), Tue/Fri (Pull), Wed/Sat (Legs), Sun (Rest)
                    when (today) {
                        DayOfWeek.MONDAY, DayOfWeek.THURSDAY ->
                            WorkoutDayInfo(
                                "Push Day",
                                "Chest, Shoulder, Triceps",
                                "7 Exercises",
                                "workout_push_day"
                            )

                        DayOfWeek.TUESDAY, DayOfWeek.FRIDAY ->
                            WorkoutDayInfo(
                                "Pull Day",
                                "Back, Biceps, Forearms",
                                "8 Exercises",
                                "workout_pull_day"
                            )

                        DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY ->
                            WorkoutDayInfo("Leg Day", "Legs, Abs", "8 Exercises", "workout_leg_day")

                        else ->
                            WorkoutDayInfo(
                                "No workout today",
                                "Rest and recover",
                                "0 Exercises",
                                null
                            )
                    }
                }
            }
        }
    }
}