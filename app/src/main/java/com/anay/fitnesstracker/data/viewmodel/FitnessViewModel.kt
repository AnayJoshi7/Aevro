package com.anay.fitnesstracker.data.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anay.fitnesstracker.components.*
import com.anay.fitnesstracker.data.*
import com.anay.fitnesstracker.data.SessionManager
import com.anay.fitnesstracker.data.repository.ExerciseRepository
import com.anay.fitnesstracker.data.repository.FoodLookupRepository
import com.anay.fitnesstracker.util.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class UsernameCheckState {
    object Idle : UsernameCheckState()
    object Checking : UsernameCheckState()
    object Available : UsernameCheckState()
    data class Unavailable(val message: String) : UsernameCheckState()
}

class FitnessViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance().apply {
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

    private val _pendingScannedFood = MutableStateFlow<FoodItem?>(null)
    val pendingScannedFood: StateFlow<FoodItem?> = _pendingScannedFood.asStateFlow()

    private var debounceJob: Job? = null
    private val _usernameCheckState = MutableStateFlow<UsernameCheckState>(UsernameCheckState.Idle)
    val usernameCheckState: StateFlow<UsernameCheckState> = _usernameCheckState.asStateFlow()

    private val _foodsList = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodsList: StateFlow<List<FoodItem>> = _foodsList.asStateFlow()

    init {
        checkSavedSession()
        loadFoodsCollection()
    }

    // --- Session & Realtime Sync Management ---

    private fun checkSavedSession() {
        val savedUsername = sessionManager.getUsername()
        if (!savedUsername.isNullOrBlank()) {
            val trimmed = savedUsername.trim()
            val lower = trimmed.lowercase()

            viewModelScope.launch {
                try {
                    // Try exact first, then lowercase
                    var doc = db.collection("users").document(trimmed).get().await()
                    if (!doc.exists()) {
                        doc = db.collection("users").document(lower).get().await()
                    }

                    if (doc.exists()) {
                        val validId = doc.id
                        attachUserListener(validId)
                    } else {
                        // Stale or deleted user account
                        sessionManager.clearSession()
                        _isSessionChecking.value = false
                    }
                } catch (e: Exception) {
                    Log.w("FitnessVM", "Could not check server session, attempting cached listener: ${e.message}")
                    // Attempt to attach anyway for offline cache support
                    attachUserListener(trimmed)
                }
            }
        } else {
            _isSessionChecking.value = false
        }
    }

    private fun attachUserListener(documentId: String) {
        userSnapshotListener?.remove()
        userSnapshotListener = db.collection("users").document(documentId)
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

    fun login(username: String, onComplete: (Boolean) -> Unit) {
        val rawInput = username.trim().removePrefix("@")
        val lowerInput = rawInput.lowercase()

        viewModelScope.launch {
            try {
                // 1. Try exact document ID
                var doc = db.collection("users").document(rawInput).get().await()

                // 2. Try lowercase document ID
                if (!doc.exists()) {
                    doc = db.collection("users").document(lowerInput).get().await()
                }

                // 3. Try querying username field
                if (!doc.exists()) {
                    val querySnapshot = db.collection("users")
                        .whereEqualTo("username", rawInput)
                        .limit(1)
                        .get()
                        .await()
                    doc = querySnapshot.documents.firstOrNull() ?: doc
                }

                if (doc.exists()) {
                    val loadedUser = doc.toObject<UserProfile>()
                    val actualDocId = doc.id

                    _currentUser.value = loadedUser

                    // Persist session to local storage
                    sessionManager.saveUsername(actualDocId)

                    // Attach realtime snapshot listener
                    attachUserListener(actualDocId)

                    onComplete(true)
                } else {
                    Log.e("FitnessVM", "User document not found for $rawInput or $lowerInput")
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("FitnessVM", "Login failed: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        userSnapshotListener?.remove()
        userSnapshotListener = null
        sessionManager.clearSession()
        _currentUser.value = null
        resetUsernameCheckState()
        onLoggedOut()
    }

    // --- Username Availability & Migration ---

    fun resetUsernameCheckState() {
        _usernameCheckState.value = UsernameCheckState.Idle
    }

    fun checkUsernameAvailability(rawInput: String) {
        debounceJob?.cancel()

        val clean = rawInput.trim().lowercase().removePrefix("@")
        val current = currentUser.value?.username?.trim()?.lowercase()?.removePrefix("@") ?: ""

        Log.d("FitnessVM", "Checking username: '$clean', current: '$current'")

        if (clean.length < 3) {
            _usernameCheckState.value = UsernameCheckState.Unavailable("Must be at least 3 characters")
            return
        }

        if (clean.length > 20) {
            _usernameCheckState.value = UsernameCheckState.Unavailable("Max 20 characters allowed")
            return
        }

        val usernameRegex = Regex("^[a-zA-Z0-9_.]+$")
        if (!usernameRegex.matches(clean)) {
            _usernameCheckState.value = UsernameCheckState.Unavailable("Only letters, numbers, _, and . allowed")
            return
        }

        if (clean == current) {
            _usernameCheckState.value = UsernameCheckState.Unavailable("This is already your current username")
            return
        }

        _usernameCheckState.value = UsernameCheckState.Checking

        debounceJob = viewModelScope.launch {
            delay(300)
            try {
                // Check if target document exists
                val targetDoc = db.collection("users").document(clean).get().await()
                val currentDocId = currentUser.value?.username?.trim()?.lowercase()

                if (targetDoc.exists() && targetDoc.id != currentDocId) {
                    _usernameCheckState.value = UsernameCheckState.Unavailable("Username is already taken")
                } else {
                    _usernameCheckState.value = UsernameCheckState.Available
                }
                Log.d("FitnessVM", "Availability set to: ${_usernameCheckState.value}")
            } catch (e: Exception) {
                Log.e("FitnessVM", "Error checking users collection: ${e.message}", e)
                _usernameCheckState.value = UsernameCheckState.Unavailable(e.localizedMessage ?: "Availability check failed")
            }
        }
    }

    fun updateUsername(
        newUsername: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserData = currentUser.value ?: return

        val oldDocId = currentUserData.username.trim()
        val displayUsername = newUsername.trim().removePrefix("@")
        val newDocId = displayUsername.lowercase()

        if (newDocId.isBlank()) {
            onError("Username cannot be empty")
            return
        }

        if (newDocId == oldDocId.lowercase() && displayUsername == currentUserData.username) {
            onError("Please choose a different username")
            return
        }

        viewModelScope.launch {
            try {
                // Check if target ID is taken
                val targetDoc = db.collection("users").document(newDocId).get().await()
                if (targetDoc.exists() && newDocId != oldDocId.lowercase()) {
                    onError("Username is already taken")
                    return@launch
                }

                // Fetch original document
                var sourceDoc = db.collection("users").document(oldDocId).get().await()
                var actualExistingDocId = oldDocId

                if (!sourceDoc.exists()) {
                    val fallbackDoc = db.collection("users").document(oldDocId.lowercase()).get().await()
                    if (fallbackDoc.exists()) {
                        sourceDoc = fallbackDoc
                        actualExistingDocId = oldDocId.lowercase()
                    } else {
                        onError("Original user record not found")
                        return@launch
                    }
                }

                val data = sourceDoc.data?.toMutableMap() ?: mutableMapOf()
                data["username"] = displayUsername

                val batch = db.batch()
                val newDocRef = db.collection("users").document(newDocId)
                batch.set(newDocRef, data)

                if (actualExistingDocId != newDocId) {
                    val oldDocRef = db.collection("users").document(actualExistingDocId)
                    batch.delete(oldDocRef)
                }

                batch.commit().await()

                // Update session & listener to point to new doc ID
                sessionManager.saveUsername(newDocId)
                attachUserListener(newDocId)

                _currentUser.value = currentUserData.copy(username = displayUsername)
                _usernameCheckState.value = UsernameCheckState.Idle

                onSuccess()
            } catch (e: Exception) {
                Log.e("FitnessVM", "Error updating username: ${e.message}", e)
                onError(e.localizedMessage ?: "Failed to update username")
            }
        }
    }

    // --- Account Deletion ---

    fun deleteAccount(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserData = currentUser.value ?: run {
            onError("No active user session")
            return
        }

        val docId = currentUserData.username.trim()

        viewModelScope.launch {
            try {
                val userRef = db.collection("users").document(docId)
                val doc = userRef.get().await()

                if (doc.exists()) {
                    userRef.delete().await()
                } else {
                    val lowerRef = db.collection("users").document(docId.lowercase())
                    lowerRef.delete().await()
                }

                userSnapshotListener?.remove()
                userSnapshotListener = null
                sessionManager.clearSession()
                _currentUser.value = null
                resetUsernameCheckState()

                onSuccess()
            } catch (e: Exception) {
                Log.e("FitnessVM", "Failed to delete account: ${e.message}", e)
                onError(e.localizedMessage ?: "Failed to delete account")
            }
        }
    }

    // --- Onboarding & Profile Setup ---

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
        val docId = profile.username.trim().lowercase()

        sessionManager.saveUsername(docId)
        _currentUser.value = profile
        attachUserListener(docId)

        db.collection("users").document(docId)
            .set(profile)
            .addOnCompleteListener {
                onComplete()
            }
    }

    // --- Profile & Nutrition Edits ---

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

        val docId = current.username.trim().lowercase()
        db.collection("users").document(docId)
            .set(updated, SetOptions.merge())
    }

    fun updateNutritionGoals(calories: Int, protein: Int) {
        val user = _currentUser.value ?: return
        val updated = user.copy(
            dailyCalorieGoal = calories,
            dailyProteinGoal = protein
        )
        _currentUser.value = updated

        val docId = user.username.trim().lowercase()
        db.collection("users").document(docId)
            .set(
                mapOf(
                    "dailyCalorieGoal" to calories,
                    "dailyProteinGoal" to protein
                ),
                SetOptions.merge()
            )
    }

    fun updateDailyCalorieGoal(newGoal: Int) {
        val user = _currentUser.value ?: return
        val updatedUser = user.copy(dailyCalorieGoal = newGoal)
        _currentUser.value = updatedUser

        val docId = user.username.trim().lowercase()
        db.collection("users").document(docId)
            .set(mapOf("dailyCalorieGoal" to newGoal), SetOptions.merge())
    }

    // --- Workout Logging & Management ---

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

        val docId = user.username.trim().lowercase()
        db.collection("users").document(docId)
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

    fun deleteLoggedExercise(exerciseId: String) {
        val user = _currentUser.value ?: return
        val updatedWorkouts = user.loggedWorkouts.filter { it.id != exerciseId }
        val updatedUser = user.copy(loggedWorkouts = updatedWorkouts)
        _currentUser.value = updatedUser

        val docId = user.username.trim().lowercase()
        db.collection("users").document(docId)
            .set(mapOf("loggedWorkouts" to updatedWorkouts), SetOptions.merge())
    }

    fun updateWorkoutPreferences(frequency: String, split: String) {
        val user = _currentUser.value ?: return
        val updatedUser = user.copy(
            workoutFrequency = frequency,
            workoutSplit = split
        )
        _currentUser.value = updatedUser

        val docId = user.username.trim().lowercase()
        db.collection("users").document(docId)
            .set(
                mapOf(
                    "workoutFrequency" to frequency,
                    "workoutSplit" to split
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

        val docId = user.username.trim().lowercase()
        db.collection("users").document(docId)
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

        val docId = user.username.trim().lowercase()
        db.collection("users").document(docId)
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

            val docId = user.username.trim().lowercase()
            db.collection("users").document(docId)
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

    // --- Meals & Nutrition Tracking ---

    fun loadFoodsCollection() {
        db.collection("foods").get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val servingBasis = doc.getString("servingBasis") ?: "100 g"
                    val calories = (doc.get("calories") as? Number)?.toDouble() ?: 0.0
                    val protein = (doc.get("protein") as? Number)?.toDouble() ?: 0.0
                    val carbohydrates = (doc.get("carbohydrates") as? Number)?.toDouble() ?: 0.0
                    val fat = (doc.get("fat") as? Number)?.toDouble() ?: 0.0
                    FoodItem(id, name, servingBasis, calories, protein, carbohydrates, fat)
                }
                _foodsList.value = items.sortedBy { it.name }
            }
    }

    fun lookupFoodByBarcode(barcode: String, onResult: (FoodItem?) -> Unit) {
        val localMatch = _foodsList.value.firstOrNull { it.id.equals(barcode, ignoreCase = true) }
        if (localMatch != null) {
            onResult(localMatch)
            return
        }

        viewModelScope.launch {
            val remoteItem = FoodLookupRepository.fetchProductByBarcode(barcode)
            onResult(remoteItem)
        }
    }

    fun setPendingScannedFood(food: FoodItem) {
        _pendingScannedFood.value = food
    }

    fun clearPendingScannedFood() {
        _pendingScannedFood.value = null
    }

    fun addMeal(
        name: String,
        calories: Int,
        protein: Double = 0.0,
        carbs: Double = 0.0,
        fat: Double = 0.0,
        items: List<MealItemEntry> = emptyList(),
        onComplete: (() -> Unit)? = null
    ) {
        val user = _currentUser.value ?: return

        val newMeal = Meal(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Meal" },
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            timestamp = System.currentTimeMillis(),
            items = items
        )

        val updatedMeals = user.meals + newMeal

        val mealNotif = NotificationItem(
            id = UUID.randomUUID().toString(),
            message = "You logged a meal: ${newMeal.name} (${newMeal.calories} kcal)",
            timestamp = System.currentTimeMillis()
        )
        val updatedNotifs = listOf(mealNotif) + user.notifications

        val updatedUser = user.copy(
            meals = updatedMeals,
            notifications = updatedNotifs
        )
        _currentUser.value = updatedUser

        val docId = user.username.trim().lowercase()
        db.collection("users").document(docId)
            .set(
                mapOf(
                    "meals" to updatedMeals,
                    "notifications" to updatedNotifs
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                onComplete?.invoke()
            }
    }

    fun saveCompleteMeal(
        mealName: String,
        totalCalories: Int,
        totalProtein: Double,
        totalCarbs: Double,
        totalFat: Double,
        items: List<MealItemEntry>,
        onComplete: () -> Unit
    ) {
        val user = _currentUser.value ?: return
        val newMeal = Meal(
            id = UUID.randomUUID().toString(),
            name = mealName.ifBlank { "Meal" },
            calories = totalCalories,
            protein = totalProtein,
            carbs = totalCarbs,
            fat = totalFat,
            timestamp = System.currentTimeMillis(),
            items = items
        )
        val updatedMeals = user.meals + newMeal
        val updatedUser = user.copy(meals = updatedMeals)
        _currentUser.value = updatedUser

        val docId = user.username.trim().lowercase()
        db.collection("users").document(docId)
            .set(mapOf("meals" to updatedMeals), SetOptions.merge())
            .addOnSuccessListener { onComplete() }
    }

    fun deleteMeal(mealId: String) {
        val user = _currentUser.value ?: return
        val updatedMeals = user.meals.filterNot { it.id == mealId }
        val updatedUser = user.copy(meals = updatedMeals)
        _currentUser.value = updatedUser

        val docId = user.username.trim().lowercase()
        db.collection("users").document(docId)
            .set(mapOf("meals" to updatedMeals), SetOptions.merge())
    }

    fun getStartOfTodayMillis(): Long {
        return java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    override fun onCleared() {
        super.onCleared()
        userSnapshotListener?.remove()
    }
}