package com.anay.fitnesstracker.Screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.Routes
import com.anay.fitnesstracker.data.WorkoutScheduleHelper
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.GlassCard
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import com.google.firebase.firestore.FirebaseFirestore
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.LocalDate

private val PrimaryGreen = Color(0xFF3DDC84)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9E9E)

private const val WORKOUT_PREFS = "workout_screen_persisted_lists"

data class WorkoutCardData(
    val title: String,
    val muscleGroups: String = "",
    val exerciseCountText: String,
    val route: String,
    val exercises: List<String> = emptyList(),
    val dayName: String = ""
)

@Composable
fun WorkoutScreen(
    onNavigate: (String) -> Unit,
    fitnessViewModel: FitnessViewModel
) {
    val hazeState = rememberHazeState()
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(WORKOUT_PREFS, Context.MODE_PRIVATE)
    }

    val user by fitnessViewModel.currentUser.collectAsState()
    val userKey = user?.username?.trim()?.lowercase().orEmpty()
    val todayDateStr = remember { LocalDate.now().toString() }

    // Revision counter to trigger immediate recomposition when local preferences change
    var localListRevision by remember { mutableIntStateOf(0) }

    // 1. Current Day Identification (Monday .. Sunday)
    val todayDayName = remember {
        LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    // 2. Resolve Today's Workout (Checking Custom Schedule First)
    val todayCustomDay = remember(user?.customSchedule, todayDayName) {
        user?.customSchedule?.find {
            it.dayName.equals(todayDayName, ignoreCase = true) && it.muscleGroup.isNotBlank()
        }
    }

    val todayWorkoutPreset = remember(user) {
        WorkoutScheduleHelper.getTodayWorkout(user)
    }

    val todayTitle = if (todayCustomDay != null) {
        todayCustomDay.muscleGroup
    } else {
        todayWorkoutPreset.title
    }

    val todayMuscles = if (todayCustomDay != null) {
        ""
    } else {
        todayWorkoutPreset.muscleGroups
    }

    // Resolve Today's Exercises with persistent deletion support
    val todayExercises = remember(user, todayCustomDay, todayTitle, todayDateStr, localListRevision) {
        val deletedTodaySet = prefs.getStringSet(
            "${userKey}_deleted_today_${todayDateStr}_$todayTitle",
            emptySet()
        ).orEmpty()

        val baseList = when {
            todayCustomDay != null && todayCustomDay.exercises.isNotEmpty() -> {
                todayCustomDay.exercises
            }
            !user?.todayCustomWorkoutList.isNullOrEmpty() &&
                    WorkoutTimerManager.isCustomWorkoutListModifiedToday(context, user?.username, todayTitle) -> {
                user!!.todayCustomWorkoutList
            }
            else -> {
                val presetModified = prefs.getStringSet("${userKey}_preset_$todayTitle", null)
                if (presetModified != null) {
                    WorkoutScheduleHelper.getTodayExercises(user).filter {
                        !presetModified.contains(it.trim().lowercase())
                    }
                } else {
                    WorkoutScheduleHelper.getTodayExercises(user)
                }
            }
        }

        baseList.filter { ex ->
            !deletedTodaySet.contains(ex.trim().lowercase())
        }
    }

    val todayExerciseCountText = "${todayExercises.size} Exercises"

    // 3. Resolve Other Workouts (Custom Schedule vs Presets)
    val hasCustomSchedule = user?.customSchedule?.any { it.muscleGroup.isNotBlank() } == true

    val otherWorkouts: List<WorkoutCardData> = remember(
        user,
        hasCustomSchedule,
        todayDayName,
        todayTitle,
        localListRevision
    ) {
        if (hasCustomSchedule) {
            user?.customSchedule
                ?.filter {
                    it.muscleGroup.isNotBlank() && !it.dayName.equals(todayDayName, ignoreCase = true)
                }
                ?.map { day ->
                    val deletedForDay = prefs.getStringSet(
                        "${userKey}_custom_day_${day.dayName.lowercase()}",
                        emptySet()
                    ).orEmpty()

                    val filteredExercises = day.exercises.filter { ex ->
                        !deletedForDay.contains(ex.trim().lowercase())
                    }

                    WorkoutCardData(
                        title = day.muscleGroup,
                        muscleGroups = "",
                        exerciseCountText = "${filteredExercises.size} Exercises",
                        route = "custom_workout_detail/${day.dayName}",
                        exercises = filteredExercises,
                        dayName = day.dayName
                    )
                } ?: emptyList()
        } else {
            val defaultWorkouts = when (user?.workoutFrequency) {
                "3 Days a Week" -> listOf(
                    WorkoutCardData(
                        "Full Body",
                        "Full Body",
                        "5 Exercises",
                        Routes.WORKOUT_FULL_BODY,
                        listOf("Squats", "Bench Press", "Bent-over Rows", "Overhead Press", "Barbell Curls")
                    )
                )
                "4 Days a Week" -> listOf(
                    WorkoutCardData(
                        "Upper Body",
                        "Chest, Back, Shoulders",
                        "6 Exercises",
                        Routes.WORKOUT_UPPER_BODY,
                        listOf("Incline Bench Press", "Pull-Ups", "Dumbbell Shoulder Press", "Cable Rows", "Dips", "Bicep Curls")
                    ),
                    WorkoutCardData(
                        "Lower Body",
                        "Quads, Hamstrings, Calves",
                        "5 Exercises",
                        Routes.WORKOUT_LOWER_BODY,
                        listOf("Squats", "Bulgarian Split Squats", "Calf Raises", "Leg Press", "Hanging Leg Raises")
                    )
                )
                "5 Days a Week" -> listOf(
                    WorkoutCardData(
                        "Upper Body",
                        "Chest, Back, Shoulders",
                        "6 Exercises",
                        Routes.WORKOUT_UPPER_BODY,
                        listOf("Incline Bench Press", "Pull-Ups", "Dumbbell Shoulder Press", "Cable Rows", "Dips", "Bicep Curls")
                    ),
                    WorkoutCardData(
                        "Lower Body",
                        "Quads, Hamstrings, Calves",
                        "5 Exercises",
                        Routes.WORKOUT_LOWER_BODY,
                        listOf("Squats", "Bulgarian Split Squats", "Calf Raises", "Leg Press", "Hanging Leg Raises")
                    ),
                    WorkoutCardData(
                        "Push Day",
                        "Chest, Shoulder, Triceps",
                        "5 Exercises",
                        Routes.WORKOUT_PUSH_DAY,
                        listOf("Flat Bench Press", "Incline Dumbbell Press", "Overhead Press", "Lateral Raises", "Tricep Pushdowns")
                    ),
                    WorkoutCardData(
                        "Pull Day",
                        "Back, Biceps",
                        "5 Exercises",
                        Routes.WORKOUT_PULL_DAY,
                        listOf("Deadlifts", "Lat Pulldown", "Barbell Row", "Face Pulls", "Bicep Curls")
                    ),
                    WorkoutCardData(
                        "Leg Day",
                        "Legs, Abs",
                        "5 Exercises",
                        Routes.WORKOUT_LEG_DAY,
                        listOf("Barbell Squats", "Romanian Deadlift", "Leg Press", "Leg Extensions", "Calf Raises")
                    )
                )
                else -> listOf(
                    WorkoutCardData(
                        "Push Day",
                        "Chest, Shoulder, Triceps",
                        "5 Exercises",
                        Routes.WORKOUT_PUSH_DAY,
                        listOf("Flat Bench Press", "Incline Dumbbell Press", "Overhead Press", "Lateral Raises", "Tricep Pushdowns")
                    ),
                    WorkoutCardData(
                        "Pull Day",
                        "Back, Biceps",
                        "5 Exercises",
                        Routes.WORKOUT_PULL_DAY,
                        listOf("Deadlifts", "Lat Pulldown", "Barbell Row", "Face Pulls", "Bicep Curls")
                    ),
                    WorkoutCardData(
                        "Leg Day",
                        "Legs, Abs",
                        "5 Exercises",
                        Routes.WORKOUT_LEG_DAY,
                        listOf("Barbell Squats", "Romanian Deadlift", "Leg Press", "Leg Extensions", "Calf Raises")
                    )
                )
            }

            defaultWorkouts
                .filter { it.title != todayTitle }
                .map { card ->
                    val deletedPresetSet = prefs.getStringSet(
                        "${userKey}_preset_${card.title}",
                        emptySet()
                    ).orEmpty()

                    val filtered = card.exercises.filter { ex ->
                        !deletedPresetSet.contains(ex.trim().lowercase())
                    }

                    card.copy(
                        exercises = filtered,
                        exerciseCountText = "${filtered.size} Exercises"
                    )
                }
        }
    }

    // Handler for deleting an exercise from Today's Workout
    val handleDeleteTodayExercise: (String) -> Unit = { exerciseName ->
        val normalized = exerciseName.trim().lowercase()
        val key = "${userKey}_deleted_today_${todayDateStr}_$todayTitle"
        val existing = prefs.getStringSet(key, emptySet()).orEmpty()
        prefs.edit().putStringSet(key, existing + normalized).apply()

        val updatedTodayList = todayExercises.filterNot {
            it.trim().equals(exerciseName.trim(), ignoreCase = true)
        }

        // Mark modified for Dashboard sync
        WorkoutTimerManager.markCustomWorkoutListModifiedToday(
            context = context,
            username = user?.username,
            workoutTitle = todayTitle
        )

        // Update Firestore so both Custom Schedule and todayCustomWorkoutList persist
        if (userKey.isNotBlank()) {
            val firestoreUpdates = mutableMapOf<String, Any>(
                "todayCustomWorkoutList" to updatedTodayList
            )
            if (todayCustomDay != null && user?.customSchedule != null) {
                val updatedSchedule = user!!.customSchedule.map { day ->
                    if (day.dayName.equals(todayDayName, ignoreCase = true)) {
                        day.copy(exercises = updatedTodayList)
                    } else {
                        day
                    }
                }
                firestoreUpdates["customSchedule"] = updatedSchedule
            }
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userKey)
                .update(firestoreUpdates)
        }

        fitnessViewModel.removePlannedExerciseFromToday(exerciseName)
        localListRevision++
    }

    // Handler for deleting an exercise from any Other Workout card
    val handleDeleteOtherWorkoutExercise: (WorkoutCardData, String) -> Unit = { workoutCard, exerciseName ->
        val normalized = exerciseName.trim().lowercase()

        if (hasCustomSchedule && workoutCard.dayName.isNotBlank()) {
            // 1. Save locally for instant persistence across navigation
            val dayKey = "${userKey}_custom_day_${workoutCard.dayName.lowercase()}"
            val existing = prefs.getStringSet(dayKey, emptySet()).orEmpty()
            prefs.edit().putStringSet(dayKey, existing + normalized).apply()

            // 2. Persist updated customSchedule to Firestore
            if (userKey.isNotBlank() && user?.customSchedule != null) {
                val updatedSchedule = user!!.customSchedule.map { day ->
                    if (day.dayName.equals(workoutCard.dayName, ignoreCase = true)) {
                        day.copy(
                            exercises = day.exercises.filterNot {
                                it.trim().equals(exerciseName.trim(), ignoreCase = true)
                            }
                        )
                    } else {
                        day
                    }
                }
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userKey)
                    .update("customSchedule", updatedSchedule)
            }
        } else {
            // Persist deletion for preset workout (e.g. Push Day, Pull Day, Leg Day)
            val presetKey = "${userKey}_preset_${workoutCard.title}"
            val existing = prefs.getStringSet(presetKey, emptySet()).orEmpty()
            prefs.edit().putStringSet(presetKey, existing + normalized).apply()
        }

        localListRevision++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()
            .hazeSource(state = hazeState)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Screen Header
            Text(
                text = "Workouts",
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(28.dp))

            ActionPillButton(
                hazeState = hazeState,
                title = "Log Workout",
                onClick = { onNavigate(Routes.LOG_WORKOUT) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Full-Width Create Custom Schedule Pill Button
            ActionPillButton(
                hazeState = hazeState,
                title = "Create custom schedule",
                onClick = { onNavigate(Routes.CUSTOM_SCHEDULE) }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Today's Workout Header
            Text(
                text = "Today’s Workout",
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryGreen,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Today's Expandable Card
            WorkoutDayExpandableCard(
                hazeState = hazeState,
                title = todayTitle,
                muscleGroups = todayMuscles,
                exerciseCountText = todayExerciseCountText,
                exercises = todayExercises,
                onDeleteExercise = handleDeleteTodayExercise
            )

            if (otherWorkouts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(30.dp))

                // Other Workouts Section
                Text(
                    text = "Other Workouts",
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryGreen,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                otherWorkouts.forEach { workout ->
                    key(workout.dayName.ifBlank { workout.title }) {
                        WorkoutDayExpandableCard(
                            hazeState = hazeState,
                            title = workout.title,
                            muscleGroups = workout.muscleGroups,
                            exerciseCountText = workout.exerciseCountText,
                            exercises = workout.exercises,
                            onDeleteExercise = { exerciseName ->
                                handleDeleteOtherWorkoutExercise(workout, exerciseName)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Fixed bottom navigation bar spacer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                color = Color(0xFFFFFFFF).copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
fun WorkoutDayExpandableCard(
    hazeState: HazeState,
    title: String,
    muscleGroups: String = "",
    exerciseCountText: String,
    exercises: List<String>,
    onDeleteExercise: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        blurRadius = 16.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (muscleGroups.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = muscleGroups,
                            color = TextWhite,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${exercises.size} Exercises",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                // Arrow Pill to toggle expansion
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 1f))
                        .clickable { isExpanded = !isExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand workout",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (exercises.isEmpty()) {
                        Text(
                            text = "No exercises in this workout.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    } else {
                        exercises.forEachIndexed { index, exerciseName ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}. $exerciseName",
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Exercise",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            onDeleteExercise(exerciseName)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    hazeState: HazeState,
    title: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        hazeState = hazeState,
        blurRadius = 16.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                color = PrimaryGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = title,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}