package com.anay.fitnesstracker.data

data class CustomDayWorkout(
    val dayName: String = "",        // e.g., "Monday", "Tuesday"
    val muscleGroup: String = "",    // e.g., "Push Day" or "Chest, Biceps"
    val exercises: List<String> = emptyList()
)

data class WorkoutSet(
    val setNumber: Int = 1,
    val weightKg: String = "",
    val reps: String = ""
)

data class LoggedExercise(
    val id: String = "",
    val category: String = "",
    val exerciseName: String = "",
    val sets: List<WorkoutSet> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class NotificationItem(
    val id: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class MealItem(
    val id: String = "",
    val name: String = "",
    val calories: Int = 0,
    val protein: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserProfile(
    val username: String = "",
    val name: String = "",
    val gender: String = "Male",
    val birthYear: Int = 2000,
    val contactNo: String = "",
    val heightCm: Double = 175.0,
    val weightKg: Double = 70.0,
    val bmi: Double = 0.0,
    val bmiCategory: String = "Normal",
    val maintenanceCalories: Int = 2000,
    val fitnessGoal: String = "Maintain Weight",
    val workoutFrequency: String = "6 Days a Week",
    val workoutSplit: String = "Push Pull Legs",
    val dailyCalorieGoal: Int = 2200,
    val dailyProteinGoal: Int = 120,
    val dailyStepGoal: Int = 10000,
    val avatarBase64: String? = null,
    val meals: List<MealItem> = emptyList(),
    val loggedWorkouts: List<LoggedExercise> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val customSchedule: List<CustomDayWorkout> = emptyList(),
    val customExercises: List<String> = emptyList()
)