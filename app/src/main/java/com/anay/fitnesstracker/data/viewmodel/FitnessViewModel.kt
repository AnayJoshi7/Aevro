package com.anay.fitnesstracker.data.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anay.fitnesstracker.data.*
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.anay.fitnesstracker.components.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
import com.anay.fitnesstracker.data.repository.FoodLookupRepository
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.asStateFlow
import com.anay.fitnesstracker.data.FoodItem
import com.google.firebase.firestore.auth.User

sealed class UsernameCheckState {
    object Idle : UsernameCheckState()
    object Checking : UsernameCheckState()
    object Available : UsernameCheckState()
    data class Unavailable(val message: String) : UsernameCheckState()
}

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
    private val _pendingScannedFood = MutableStateFlow<FoodItem?>(null)
    val pendingScannedFood: StateFlow<FoodItem?> = _pendingScannedFood.asStateFlow()
    private var debounceJob: Job? = null
    private val _usernameCheckState = MutableStateFlow<UsernameCheckState>(UsernameCheckState.Idle)
    val usernameCheckState: StateFlow<UsernameCheckState> = _usernameCheckState.asStateFlow()

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

        // If it's unchanged, disallow or mark idle
        if (clean == current) {
            _usernameCheckState.value = UsernameCheckState.Unavailable("This is already your current username")
            return
        }

        _usernameCheckState.value = UsernameCheckState.Checking

        debounceJob = viewModelScope.launch {
            delay(300) // 300ms debounce
            try {
                val firestore = FirebaseFirestore.getInstance()
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("username", clean)
                    .get()
                    .await()

                // Also check for case-insensitive matches if needed
                val matchingDoc = querySnapshot.documents.firstOrNull()
                val myId = currentUser.value?.username

                if (matchingDoc != null && matchingDoc.id != myId) {
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

        // 1. Keep the exact current document ID so Firestore can locate and delete it
        val oldDocId = currentUserData.username.trim()

        // 2. Preserve exact display casing for the UI
        val displayUsername = newUsername.trim().removePrefix("@")

        // 3. Normalized lowercase document ID for reliable lookups
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
            val firestore = FirebaseFirestore.getInstance()
            try {
                // Check if another user already owns this lowercase document ID
                val targetDoc = firestore.collection("users").document(newDocId).get().await()
                if (targetDoc.exists() && newDocId != oldDocId.lowercase()) {
                    onError("Username is already taken")
                    return@launch
                }

                // Fetch the existing document using its EXACT original ID
                var sourceDoc = firestore.collection("users").document(oldDocId).get().await()
                var actualExistingDocId = oldDocId

                // Fallback: if not found with exact casing, try lowercase
                if (!sourceDoc.exists()) {
                    val fallbackDoc = firestore.collection("users").document(oldDocId.lowercase()).get().await()
                    if (fallbackDoc.exists()) {
                        sourceDoc = fallbackDoc
                        actualExistingDocId = oldDocId.lowercase()
                    } else {
                        onError("Original user record not found")
                        return@launch
                    }
                }

                val data = sourceDoc.data?.toMutableMap() ?: mutableMapOf()

                // Store the display username with preferred casing
                data["username"] = displayUsername

                val batch = firestore.batch()
                val newDocRef = firestore.collection("users").document(newDocId)
                batch.set(newDocRef, data)

                // Delete the old document using its exact discovered ID
                if (actualExistingDocId != newDocId) {
                    val oldDocRef = firestore.collection("users").document(actualExistingDocId)
                    batch.delete(oldDocRef)
                }

                batch.commit().await()

                // Update local state
                _currentUser.value = currentUserData.copy(username = displayUsername)
                _usernameCheckState.value = FitnessViewModel.UsernameCheckState.Idle

                onSuccess()
            } catch (e: Exception) {
                Log.e("FitnessVM", "Error updating username: ${e.message}", e)
                onError(e.localizedMessage ?: "Failed to update username")
            }
        }
    }

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
            val firestore = FirebaseFirestore.getInstance()
            try {
                // 1. Try deleting with exact casing
                val userRef = firestore.collection("users").document(docId)
                val doc = userRef.get().await()

                if (doc.exists()) {
                    userRef.delete().await()
                } else {
                    // Fallback to lowercase document ID if casing differed
                    val lowerRef = firestore.collection("users").document(docId.lowercase())
                    lowerRef.delete().await()
                }

                // 2. Clear state
                _currentUser.value = null
                resetUsernameCheckState()

                onSuccess()
            } catch (e: Exception) {
                Log.e("FitnessVM", "Failed to delete account: ${e.message}", e)
                onError(e.localizedMessage ?: "Failed to delete account")
            }
        }
    }
    sealed class UsernameCheckState {
        object Idle : UsernameCheckState()
        object Checking : UsernameCheckState()
        object Available : UsernameCheckState()
        data class Unavailable(val message: String) : UsernameCheckState()
    }
    fun setPendingScannedFood(food: FoodItem) {
        _pendingScannedFood.value = food
    }

    fun clearPendingScannedFood() {
        _pendingScannedFood.value = null
    }


    init {
        checkSavedSession()
    }
    // In FitnessViewModel.kt
    private val _foodsList = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodsList: StateFlow<List<FoodItem>> = _foodsList.asStateFlow()

    init {
        loadFoodsCollection()
    }

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
    fun deleteMeal(mealId: String) {
        val user = _currentUser.value ?: return
        val updatedMeals = user.meals.filterNot { it.id == mealId }
        val updatedUser = user.copy(meals = updatedMeals)
        _currentUser.value = updatedUser

        db.collection("users").document(user.username)
            .set(mapOf("meals" to updatedMeals), SetOptions.merge())
    }

    fun updateDailyCalorieGoal(newGoal: Int) {
        val user = _currentUser.value ?: return
        val updatedUser = user.copy(dailyCalorieGoal = newGoal)
        _currentUser.value = updatedUser

        db.collection("users").document(user.username)
            .set(mapOf("dailyCalorieGoal" to newGoal), SetOptions.merge())
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

        db.collection("users").document(user.username)
            .set(mapOf("meals" to updatedMeals), SetOptions.merge())
            .addOnSuccessListener { onComplete() }
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

    fun login(username: String, onComplete: (Boolean) -> Unit) {
        val rawInput = username.trim().removePrefix("@")
        val lowerInput = rawInput.lowercase()

        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                var doc = firestore.collection("users").document(rawInput).get().await()

                if (!doc.exists()) {
                    doc = firestore.collection("users").document(lowerInput).get().await()
                }

                if (!doc.exists()) {
                    val querySnapshot = firestore.collection("users")
                        .whereEqualTo("username", rawInput)
                        .limit(1)
                        .get()
                        .await()
                    doc = querySnapshot.documents.firstOrNull() ?: doc
                }

                if (doc.exists()) {
                    val loadedUser = doc.toObject<UserProfile>()
                    _currentUser.value = loadedUser
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
    fun lookupFoodByBarcode(barcode: String, onResult: (FoodItem?) -> Unit) {
        // 1. Check if the barcode matches any item in local Firestore collection
        val localMatch = _foodsList.value.firstOrNull { it.id.equals(barcode, ignoreCase = true) }
        if (localMatch != null) {
            onResult(localMatch)
            return
        }

        // 2. Query Open Food Facts public database
        viewModelScope.launch {
            val remoteItem = FoodLookupRepository.fetchProductByBarcode(barcode)
            onResult(remoteItem)
        }
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

        // Persist to Firestore
        db.collection("users").document(user.username)
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

}