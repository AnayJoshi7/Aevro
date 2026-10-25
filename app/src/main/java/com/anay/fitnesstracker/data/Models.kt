package com.anay.fitnesstracker.data
import com.google.firebase.firestore.Exclude
import java.util.UUID

data class CustomDayWorkout(
    val dayName: String = "",        // e.g., "Monday", "Tuesday"
    val muscleGroup: String = "",    // e.g., "Push Day" or "Chest, Biceps"
    val exercises: List<String> = emptyList()
)

data class WorkoutSet(
    val setNumber: Int = 1,
    val weightKg: String = "",
    val reps: String = ""
) {
    @get:Exclude
    val weight: String
        get() = weightKg
}

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

data class Meal(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<MealItemEntry> = emptyList()
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
    val meals: List<Meal> = emptyList(),
    val loggedWorkouts: List<LoggedExercise> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val customSchedule: List<CustomDayWorkout> = emptyList(),
    val customExercises: List<String> = emptyList()
)
data class FoodItem(
    val id: String = "",
    val name: String = "",
    val servingBasis: String = "100 g",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbohydrates: Double = 0.0,
    val fat: Double = 0.0
)

data class MealItemEntry(
    val name: String = "",
    val qty: Double = 0.0,
    val unit: String = "g",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
)

data class LoggedMeal(
    val id: String = "",
    val name: String = "",
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<MealItemEntry> = emptyList()
)
