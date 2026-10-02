package com.anay.fitnesstracker.data.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anay.fitnesstracker.data.*
import com.anay.fitnesstracker.data.repository.ExerciseRepository
import com.anay.fitnesstracker.util.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FitnessViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance().apply {
        // Guarantee local disk cache is enabled so data is preserved even offline
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }
    private val sessionManager = SessionManager(application)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _tempOnboarding = MutableStateFlow(UserProfile())
    val tempOnboarding: StateFlow<UserProfile> = _tempOnboarding.asStateFlow()

    private val _highlightedSplit = MutableStateFlow<String?>(null)
    val highlightedSplit: StateFlow<String?> = _highlightedSplit.asStateFlow()

    private val _isSessionChecking = MutableStateFlow(true)
    val isSessionChecking: StateFlow<Boolean> = _isSessionChecking.asStateFlow()

    private var userSnapshotListener: ListenerRegistration? = null

    init {
        checkSavedSession()
    }

    private fun checkSavedSession() {
        val savedUsername = sessionManager.getUsername()
        if (!savedUsername.isNullOrEmpty()) {
            attachUserListener(savedUsername)
        } else {
            _isSessionChecking.value = false
        }
    }

    // Listens to local cache first, then syncs with server when available
    private fun attachUserListener(username: String) {
        userSnapshotListener?.remove()
        userSnapshotListener = db.collection("users").document(username)
            .addSnapshotListener { snapshot, error ->
                _isSessionChecking.value = false
                if (error != null) {
                    Log.w("FitnessViewModel", "Firestore listener notice: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val profile = snapshot.toObject(UserProfile::class.java)
                        _currentUser.value = profile
                    } catch (e: Exception) {
                        Log.e("FitnessViewModel", "Deserialization error", e)
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        userSnapshotListener?.remove()
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

        // Writes to local disk cache immediately without crashing on network error
        db.collection("users").document(user.username)
            .set(mapOf("meals" to updatedMeals), SetOptions.merge())
    }

    // 2. Log Exercise (Safe against offline / UnknownHostException)
    fun logExercise(exerciseName: String, category: String = "", sets: List<WorkoutSet>) {
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

        // Clean Map serialization to prevent ApiUtil.invoke crash:
        val workoutsPayload = updatedWorkouts.map { exercise ->
            mapOf(
                "id" to exercise.id,
                "category" to exercise.category,
                "exerciseName" to exercise.exerciseName,
                "timestamp" to exercise.timestamp,
                "sets" to exercise.sets.map { s ->
                    mapOf(
                        "setNumber" to s.setNumber,
                        "weightKg" to s.weightKg,
                        "reps" to s.reps
                    )
                }
            )
        }

        val notifsPayload = updatedNotifs.map { notif ->
            mapOf(
                "id" to notif.id,
                "message" to notif.message,
                "timestamp" to notif.timestamp
            )
        }

        // Write directly to local Firestore SQLite cache (no .await() to block if DNS is offline)
        db.collection("users").document(user.username)
            .set(
                mapOf(
                    "loggedWorkouts" to workoutsPayload,
                    "notifications" to notifsPayload
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                Log.d("FitnessViewModel", "Workout saved to Firestore!")
            }
            .addOnFailureListener { e ->
                Log.w("FitnessViewModel", "Write queued locally: ${e.message}")
            }
    }

    fun logExercise(exerciseName: String, sets: List<WorkoutSet>) {
        logExercise(exerciseName = exerciseName, category = "", sets = sets)
    }

    // 3. Delete a Logged Exercise Entry
    fun deleteLoggedExercise(exerciseId: String) {
        val user = _currentUser.value ?: return
        val updatedWorkouts = user.loggedWorkouts.filter { it.id != exerciseId }
        val updatedUser = user.copy(loggedWorkouts = updatedWorkouts)
        _currentUser.value = updatedUser

        db.collection("users").document(user.username)
            .set(mapOf("loggedWorkouts" to updatedWorkouts), SetOptions.merge())
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

    fun confirmAndSaveProfile(onComplete: () -> Unit) {
        val initialNotification = NotificationItem(
            id = UUID.randomUUID().toString(),
            message = "Account created successfully! Welcome to Aevro.",
            timestamp = System.currentTimeMillis()
        )
        val profile = _tempOnboarding.value.copy(notifications = listOf(initialNotification))

        sessionManager.saveUsername(profile.username)
        _currentUser.value = profile
        attachUserListener(profile.username)

        db.collection("users").document(profile.username)
            .set(profile)
            .addOnCompleteListener {
                onComplete()
            }
    }

    fun login(username: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(username.trim()).get().await()
                if (snapshot.exists()) {
                    var profile = snapshot.toObject(UserProfile::class.java)!!

                    val loginNotif = NotificationItem(
                        id = UUID.randomUUID().toString(),
                        message = "Recent Login to your Account",
                        timestamp = System.currentTimeMillis()
                    )
                    val updatedNotifs = listOf(loginNotif) + profile.notifications
                    profile = profile.copy(notifications = updatedNotifs)

                    db.collection("users").document(profile.username)
                        .set(mapOf("notifications" to updatedNotifs), SetOptions.merge())

                    sessionManager.saveUsername(profile.username)
                    _currentUser.value = profile
                    attachUserListener(profile.username)
                    onSuccess()
                } else {
                    onError("Username not found!")
                }
            } catch (e: Exception) {
                // If offline, check if session exists locally
                if (sessionManager.getUsername() == username.trim()) {
                    attachUserListener(username.trim())
                    onSuccess()
                } else {
                    onError(e.localizedMessage ?: "Error during login. Check internet connection.")
                }
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

        db.collection("users").document(user.username)
            .set(
                mapOf(
                    "workoutFrequency" to frequency,
                    "workoutSplit" to split
                ),
                SetOptions.merge()
            )
    }

    fun logout(onLoggedOut: () -> Unit) {
        userSnapshotListener?.remove()
        userSnapshotListener = null
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

        db.collection("users").document(current.username)
            .set(updated, SetOptions.merge())
    }

    fun updateNutritionGoals(calories: Int, protein: Int) {
        val user = _currentUser.value ?: return
        val updated = user.copy(
            dailyCalorieGoal = calories,
            dailyProteinGoal = protein
        )
        _currentUser.value = updated

        db.collection("users").document(user.username)
            .set(
                mapOf(
                    "dailyCalorieGoal" to calories,
                    "dailyProteinGoal" to protein
                ),
                SetOptions.merge()
            )
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

        db.collection("users").document(user.username)
            .set(mapOf("customSchedule" to daysList), SetOptions.merge())
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

        db.collection("users").document(user.username)
            .set(
                mapOf(
                    "customSchedule" to daysList,
                    "customExercises" to customExList
                ),
                SetOptions.merge()
            )
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

            db.collection("users").document(user.username)
                .set(mapOf("customSchedule" to daysList), SetOptions.merge())
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
            protein = protein,
            timestamp = System.currentTimeMillis()
        )
        val updatedMeals = listOf(newMeal) + user.meals

        val mealNotif = NotificationItem(
            id = UUID.randomUUID().toString(),
            message = "You logged a meal: $name ($calories kcal)",
            timestamp = System.currentTimeMillis()
        )
        val updatedNotifs = listOf(mealNotif) + user.notifications

        val updatedUser = user.copy(meals = updatedMeals, notifications = updatedNotifs)
        _currentUser.value = updatedUser

        db.collection("users").document(user.username)
            .set(
                mapOf(
                    "meals" to updatedMeals,
                    "notifications" to updatedNotifs
                ),
                SetOptions.merge()
            )
    }
}