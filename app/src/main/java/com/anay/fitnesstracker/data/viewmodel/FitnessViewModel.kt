package com.anay.fitnesstracker.data.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anay.fitnesstracker.data.*
import com.anay.fitnesstracker.data.repository.ExerciseRepository
import com.anay.fitnesstracker.util.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FitnessViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val sessionManager = SessionManager(application)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _tempOnboarding = MutableStateFlow(UserProfile())
    val tempOnboarding: StateFlow<UserProfile> = _tempOnboarding.asStateFlow()

    private val _highlightedSplit = MutableStateFlow<String?>(null)
    val highlightedSplit: StateFlow<String?> = _highlightedSplit.asStateFlow()

    private val _isSessionChecking = MutableStateFlow(true)
    val isSessionChecking: StateFlow<Boolean> = _isSessionChecking.asStateFlow()

    init {
        checkSavedSession()
    }

    private fun checkSavedSession() {
        val savedUsername = sessionManager.getUsername()
        if (!savedUsername.isNullOrEmpty()) {
            viewModelScope.launch {
                try {
                    val snapshot = db.collection("users").document(savedUsername).get().await()
                    if (snapshot.exists()) {
                        _currentUser.value = snapshot.toObject(UserProfile::class.java)
                    } else {
                        sessionManager.clearSession()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _isSessionChecking.value = false
                }
            }
        } else {
            _isSessionChecking.value = false
        }
    }

    fun updateInitialDetails(
        name: String,
        gender: String,
        dobYear: Int,
        contact: String,
        heightCm: Double,
        weightKg: Double
    ) {
        val username = FitnessCalculator.generateUsername(name, dobYear.toString(), contact)
        val (bmi, cat) = FitnessCalculator.calculateBmi(heightCm, weightKg)
        val maintCal = FitnessCalculator.calculateMaintenanceCalories(gender, dobYear, heightCm, weightKg)

        _tempOnboarding.value = _tempOnboarding.value.copy(
            username = username,
            name = name,
            gender = gender,
            birthYear = dobYear,
            contactNo = contact,
            heightCm = heightCm,
            weightKg = weightKg,
            bmi = bmi,
            bmiCategory = cat,
            maintenanceCalories = maintCal
        )
    }
    // Inside FitnessViewModel.kt:

    fun getStartOfTodayMillis(): Long {
        return java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    // 1. Delete a Meal
    fun deleteMeal(mealId: String) {
        val user = _currentUser.value ?: return
        val updatedMeals = user.meals.filter { it.id != mealId }
        val updatedUser = user.copy(meals = updatedMeals)
        _currentUser.value = updatedUser

        viewModelScope.launch {
            try {
                db.collection("users").document(user.username)
                    .update("meals", updatedMeals)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 2. Log Exercise with Category
    fun logExercise(exerciseName: String, category: String, sets: List<WorkoutSet>) {
        val user = _currentUser.value ?: return
        val newEntry = LoggedExercise(
            id = UUID.randomUUID().toString(),
            category = category,
            exerciseName = exerciseName,
            sets = sets,
            timestamp = System.currentTimeMillis()
        )

        val updatedWorkouts = listOf(newEntry) + user.loggedWorkouts
        val newNotification = NotificationItem(
            id = UUID.randomUUID().toString(),
            message = "Logged: $exerciseName (${sets.size} sets)",
            timestamp = System.currentTimeMillis()
        )
        val updatedNotifs = listOf(newNotification) + user.notifications

        val updatedUser = user.copy(
            loggedWorkouts = updatedWorkouts,
            notifications = updatedNotifs
        )
        _currentUser.value = updatedUser

        viewModelScope.launch {
            try {
                db.collection("users").document(user.username)
                    .update(
                        mapOf(
                            "loggedWorkouts" to updatedWorkouts,
                            "notifications" to updatedNotifs
                        )
                    )
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 3. Delete an entire Logged Exercise Entry
    fun deleteLoggedExercise(exerciseId: String) {
        val user = _currentUser.value ?: return
        val updatedWorkouts = user.loggedWorkouts.filter { it.id != exerciseId }
        val updatedUser = user.copy(loggedWorkouts = updatedWorkouts)
        _currentUser.value = updatedUser

        viewModelScope.launch {
            try {
                db.collection("users").document(user.username)
                    .update("loggedWorkouts", updatedWorkouts)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateGoal(goal: String) {
        val current = _tempOnboarding.value
        val (cals, protein) = FitnessCalculator.calculateTargets(
            current.maintenanceCalories,
            current.weightKg,
            goal
        )
        _tempOnboarding.value = current.copy(
            fitnessGoal = goal,
            dailyCalorieGoal = cals,
            dailyProteinGoal = protein
        )
    }

    fun updateFrequency(freq: String) {
        val split = FitnessCalculator.getSplitForFrequency(freq)
        _tempOnboarding.value = _tempOnboarding.value.copy(
            workoutFrequency = freq,
            workoutSplit = split
        )
        _highlightedSplit.value = split
    }

    fun selectExplicitSplit(split: String) {
        _tempOnboarding.value = _tempOnboarding.value.copy(workoutSplit = split)
        _highlightedSplit.value = split
    }
    fun logExercise(exerciseName: String, sets: List<WorkoutSet>) {
        val user = _currentUser.value ?: return
        val newEntry = LoggedExercise(
            id = UUID.randomUUID().toString(),
            exerciseName = exerciseName,
            sets = sets,
            timestamp = System.currentTimeMillis()
        )

        val updatedWorkouts = listOf(newEntry) + user.loggedWorkouts
        val newNotification = NotificationItem(
            id = UUID.randomUUID().toString(),
            message = "Logged: $exerciseName (${sets.size} sets)",
            timestamp = System.currentTimeMillis()
        )
        val updatedNotifs = listOf(newNotification) + user.notifications

        val updatedUser = user.copy(
            loggedWorkouts = updatedWorkouts,
            notifications = updatedNotifs
        )
        _currentUser.value = updatedUser

        viewModelScope.launch {
            try {
                db.collection("users").document(user.username)
                    .update(
                        mapOf(
                            "loggedWorkouts" to updatedWorkouts,
                            "notifications" to updatedNotifs
                        )
                    )
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun confirmAndSaveProfile(onComplete: () -> Unit) {
        val initialNotification = NotificationItem(
            id = UUID.randomUUID().toString(),
            message = "Account created successfully! Welcome to Fitness Tracker.",
            timestamp = System.currentTimeMillis()
        )
        val profile = _tempOnboarding.value.copy(notifications = listOf(initialNotification))

        viewModelScope.launch {
            try {
                db.collection("users").document(profile.username).set(profile).await()
                sessionManager.saveUsername(profile.username)
                _currentUser.value = profile
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                sessionManager.saveUsername(profile.username)
                _currentUser.value = profile
                onComplete()
            }
        }
    }

    fun login(username: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(username.trim()).get().await()
                if (snapshot.exists()) {
                    var profile = snapshot.toObject(UserProfile::class.java)!!

                    // Add dynamic login notification
                    val loginNotif = NotificationItem(
                        id = UUID.randomUUID().toString(),
                        message = "Recent Login to your Account",
                        timestamp = System.currentTimeMillis()
                    )
                    val updatedNotifs = listOf(loginNotif) + profile.notifications
                    profile = profile.copy(notifications = updatedNotifs)

                    db.collection("users").document(profile.username)
                        .update("notifications", updatedNotifs)
                        .await()

                    sessionManager.saveUsername(profile.username)
                    _currentUser.value = profile
                    onSuccess()
                } else {
                    onError("Username not found!")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error during login")
            }
        }
    }
    fun updateWorkoutPreferences(frequency: String, split: String) {
        val user = _currentUser.value ?: return
        val updatedUser = user.copy(
            workoutFrequency = frequency,
            workoutSplit = split
        )
        _currentUser.value = updatedUser

        viewModelScope.launch {
            try {
                db.collection("users").document(user.username)
                    .update(
                        mapOf(
                            "workoutFrequency" to frequency,
                            "workoutSplit" to split
                        )
                    )
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        sessionManager.clearSession()
        _currentUser.value = null
        onLoggedOut()
    }

    fun updateProfile(
        name: String,
        birthYear: Int,
        contactNo: String,
        avatarUri: Uri? = null
    ) {
        val current = _currentUser.value ?: return
        val base64Str = if (avatarUri != null) {
            ImageUtils.uriToBase64(getApplication(), avatarUri) ?: current.avatarBase64
        } else {
            current.avatarBase64
        }

        val updated = current.copy(
            name = name,
            birthYear = birthYear,
            contactNo = contactNo,
            avatarBase64 = base64Str
        )
        _currentUser.value = updated

        viewModelScope.launch {
            try {
                db.collection("users").document(current.username).set(updated).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // In FitnessViewModel.kt:

    fun updateNutritionGoals(calories: Int, protein: Int) {
        val user = _currentUser.value ?: return
        val updated = user.copy(
            dailyCalorieGoal = calories,
            dailyProteinGoal = protein
        )
        _currentUser.value = updated

        viewModelScope.launch {
            try {
                db.collection("users").document(user.username)
                    .update(
                        mapOf(
                            "dailyCalorieGoal" to calories,
                            "dailyProteinGoal" to protein
                        )
                    ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateCustomDayMuscleGroup(dayName: String, muscleGroup: String) {
        val user = _currentUser.value ?: return
        val daysList = (if (user.customSchedule.isEmpty()) getDefaultCustomDays() else user.customSchedule).toMutableList()
        val index = daysList.indexOfFirst { it.dayName.equals(dayName, ignoreCase = true) }

        if (index != -1) {
            daysList[index] = daysList[index].copy(muscleGroup = muscleGroup)
        } else {
            daysList.add(CustomDayWorkout(dayName = dayName, muscleGroup = muscleGroup))
        }

        val updated = user.copy(customSchedule = daysList)
        _currentUser.value = updated

        viewModelScope.launch {
            try {
                db.collection("users").document(user.username)
                    .update("customSchedule", daysList)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addExerciseToDay(dayName: String, exerciseName: String) {
        val user = _currentUser.value ?: return
        val trimmed = exerciseName.trim()
        if (trimmed.isBlank()) return

        val daysList = (if (user.customSchedule.isEmpty()) getDefaultCustomDays() else user.customSchedule).toMutableList()
        val index = daysList.indexOfFirst { it.dayName.equals(dayName, ignoreCase = true) }

        if (index != -1) {
            val existingExercises = daysList[index].exercises.toMutableList()
            if (!existingExercises.contains(trimmed)) {
                existingExercises.add(trimmed)
                daysList[index] = daysList[index].copy(exercises = existingExercises)
            }
        }

        val customExList = user.customExercises.toMutableList()
        if (!customExList.contains(trimmed)) {
            customExList.add(trimmed)
        }

        val updated = user.copy(customSchedule = daysList, customExercises = customExList)
        _currentUser.value = updated

        viewModelScope.launch {
            try {
                db.collection("users").document(user.username)
                    .update(
                        mapOf(
                            "customSchedule" to daysList,
                            "customExercises" to customExList
                        )
                    ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteExerciseFromDay(dayName: String, exerciseName: String) {
        val user = _currentUser.value ?: return
        val daysList = user.customSchedule.toMutableList()
        val index = daysList.indexOfFirst { it.dayName.equals(dayName, ignoreCase = true) }

        if (index != -1) {
            val existing = daysList[index].exercises.filter { it != exerciseName }
            daysList[index] = daysList[index].copy(exercises = existing)
            val updated = user.copy(customSchedule = daysList)
            _currentUser.value = updated

            viewModelScope.launch {
                try {
                    db.collection("users").document(user.username)
                        .update("customSchedule", daysList)
                        .await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getAllAvailableExercises(): List<String> {
        val master = ExerciseRepository.allExercises
        val custom = _currentUser.value?.customExercises ?: emptyList()
        return (master + custom).distinct().sorted()
    }

    private fun getDefaultCustomDays(): List<CustomDayWorkout> {
        return listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").map {
            CustomDayWorkout(dayName = it, muscleGroup = "")
        }
    }

    fun addMeal(name: String, calories: Int, protein: Int) {
        val user = _currentUser.value ?: return
        val newMeal = MealItem(
            id = UUID.randomUUID().toString(),
            name = name,
            calories = calories,
            protein = protein
        )
        val updatedMeals = listOf(newMeal) + user.meals

        // Add dynamic meal notification
        val mealNotif = NotificationItem(
            id = UUID.randomUUID().toString(),
            message = "You logged a meal: $name ($calories kcal)",
            timestamp = System.currentTimeMillis()
        )
        val updatedNotifs = listOf(mealNotif) + user.notifications

        val updatedUser = user.copy(meals = updatedMeals, notifications = updatedNotifs)
        _currentUser.value = updatedUser

        viewModelScope.launch {
            try {
                db.collection("users").document(user.username)
                    .update(
                        mapOf(
                            "meals" to updatedMeals,
                            "notifications" to updatedNotifs
                        )
                    )
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}