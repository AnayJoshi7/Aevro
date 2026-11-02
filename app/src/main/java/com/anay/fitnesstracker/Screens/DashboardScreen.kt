package com.anay.fitnesstracker.Screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.Routes
import com.anay.fitnesstracker.data.StepSensorManager
import com.anay.fitnesstracker.data.WorkoutScheduleHelper
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.GlassCard
import com.anay.fitnesstracker.ui.theme.PrimaryGreen
import com.anay.fitnesstracker.ui.theme.TextMuted
import com.anay.fitnesstracker.ui.theme.TextWhite
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import com.anay.fitnesstracker.ui.theme.borderGradient
import com.anay.fitnesstracker.ui.theme.glassEffect
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onNavigateMealLog: () -> Unit,
    fitnessViewModel: FitnessViewModel
) {
    val hazeState = rememberHazeState()
    val context = LocalContext.current

    val user by fitnessViewModel.currentUser.collectAsState()
    val calorieTarget = user?.dailyCalorieGoal ?: 2400
    val todayWorkout = WorkoutScheduleHelper.getTodayWorkout(user)

    // Dialog & Expansion states
    var showWaterDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var todayWorkoutExpanded by remember { mutableStateOf(false) }
    var completedWorkoutExpanded by remember { mutableStateOf(false) }

    // ---------- Activity recognition permission ----------

    val permissionPrefs = remember(context) {
        context.getSharedPreferences(
            "app_permission_prefs",
            Context.MODE_PRIVATE
        )
    }

    var activityPermissionGranted by remember(context) {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        activityPermissionGranted = granted

        permissionPrefs.edit()
            .putBoolean("activity_permission_prompted", true)
            .apply()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasPromptedBefore = permissionPrefs.getBoolean(
                "activity_permission_prompted",
                false
            )

            if (!activityPermissionGranted && !hasPromptedBefore) {
                permissionLauncher.launch(
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            }
        }
    }

    // ---------- Step tracking ----------

    val stepSensorManager = remember(context) {
        StepSensorManager(context)
    }

    val currentSteps by stepSensorManager.todaySteps.collectAsState()

    DisposableEffect(stepSensorManager, activityPermissionGranted) {
        if (activityPermissionGranted) {
            stepSensorManager.startListening()
        }

        onDispose {
            stepSensorManager.stopListening()
        }
    }

    val stepGoal = user?.dailyStepGoal ?: 10_000

    val stepsFraction by remember(stepGoal) {
        derivedStateOf { (currentSteps.toFloat() / stepGoal).coerceIn(0f, 1f) }
    }
    val stepsPercent by remember {
        derivedStateOf { (stepsFraction * 100).toInt() }
    }

    // ---------- Today's totals & Workout resolution ----------

    val startOfTodayMillis = remember {
        LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    val plannedExercises = remember(user?.todayCustomWorkoutList, todayWorkout) {
        if (!user?.todayCustomWorkoutList.isNullOrEmpty()) {
            user!!.todayCustomWorkoutList
        } else {
            WorkoutScheduleHelper.getTodayExercises(user)
        }
    }

    val completedExercisesList = remember(user?.loggedWorkouts, startOfTodayMillis) {
        user?.loggedWorkouts
            ?.filter { it.timestamp >= startOfTodayMillis }
            ?.map { it.exerciseName.trim().lowercase() }
            ?.distinct() ?: emptyList()
    }

    val remainingExercises = remember(plannedExercises, completedExercisesList) {
        plannedExercises.filter { plan ->
            !completedExercisesList.contains(plan.trim().lowercase())
        }
    }

    val completedExercisesToday = completedExercisesList.size
    val targetExercisesToday = plannedExercises.size.coerceAtLeast(1)

    val workoutProgressFraction = (completedExercisesToday.toFloat() / targetExercisesToday).coerceIn(0f, 1f)
    val workoutProgressPercent = (workoutProgressFraction * 100).toInt()

    val totalCaloriesConsumedToday = remember(
        user?.meals,
        startOfTodayMillis
    ) {
        user?.meals
            ?.filter { it.timestamp >= startOfTodayMillis }
            ?.sumOf { it.calories } ?: 0
    }

    val caloriesFraction = if (calorieTarget > 0) {
        (
                totalCaloriesConsumedToday.toFloat() /
                        calorieTarget
                ).coerceIn(0f, 1f)
    } else {
        0f
    }

    val caloriesPercent = (caloriesFraction * 100).toInt()

    // ---------- Water calculations ----------
    val todayWaterMl = remember(user?.waterLogs, startOfTodayMillis) {
        user?.waterLogs?.filter { it.timestamp >= startOfTodayMillis }?.sumOf { it.amountMl } ?: 0
    }
    val todayWaterLiters = todayWaterMl / 1000.0
    val waterGoalLiters = user?.dailyWaterGoalLiters ?: 4.0
    val waterFraction = (todayWaterLiters / waterGoalLiters).toFloat().coerceIn(0f, 1f)
    val waterPercent = (waterFraction * 100).toInt()

    // ---------- Weight calculations ----------
    val currentWeight = user?.weightKg ?: 70.0
    val lastWeekWeight = user?.lastWeekEndWeightKg ?: currentWeight
    val weightDiff = currentWeight - lastWeekWeight
    val weightDiffText = when {
        weightDiff > 0 -> "+${String.format(Locale.US, "%.1f", weightDiff)} kg than last week"
        weightDiff < 0 -> "-${String.format(Locale.US, "%.1f", -weightDiff)} kg than last week"
        else -> "Same as last week"
    }

    // ---------- Screen ----------

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .appBackgroundGradient()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .hazeSource(state = hazeState)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Greetings, ${
                        user?.name?.ifBlank { "Anay" } ?: "Anay"
                    } 👋",
                    color = PrimaryGreen,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                SectionTitle("Today’s Workout")

                WorkoutCardTop(
                    hazeState = hazeState,
                    title = todayWorkout.title,
                    exerciseCount = "${plannedExercises.size} Exercises",
                    exercises = plannedExercises,
                    isExpanded = todayWorkoutExpanded,
                    onToggleExpand = { todayWorkoutExpanded = !todayWorkoutExpanded },
                    onDeleteExercise = { fitnessViewModel.removePlannedExerciseFromToday(it) },
                    onStartClick = {
                        val targetRoute = todayWorkout.route ?: Routes.LOG_WORKOUT
                        onNavigate(targetRoute)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. TODAY'S PROGRESS SECTION
                SectionTitle("Today’s Progress")

                TodayProgressCard(
                    hazeState = hazeState,
                    completed = completedExercisesToday,
                    target = targetExercisesToday,
                    fraction = workoutProgressFraction,
                    percent = workoutProgressPercent,
                    remainingExercises = remainingExercises,
                    isExpanded = completedWorkoutExpanded,
                    onToggleExpand = { completedWorkoutExpanded = !completedWorkoutExpanded },
                    onDeleteRemainingExercise = { fitnessViewModel.removePlannedExerciseFromToday(it) }
                )

                CaloriesCard(
                    hazeState = hazeState,
                    consumed = totalCaloriesConsumedToday,
                    target = calorieTarget,
                    fraction = caloriesFraction,
                    percent = caloriesPercent,
                    onClick = onNavigateMealLog
                )

                StepsCard(
                    hazeState = hazeState,
                    steps = currentSteps,
                    target = stepGoal,
                    fraction = stepsFraction,
                    percent = stepsPercent
                )

                // 3. WATER INTAKE CARD
                WaterIntakeCard(
                    hazeState = hazeState,
                    liters = todayWaterLiters,
                    goalLiters = waterGoalLiters,
                    fraction = waterFraction,
                    percent = waterPercent,
                    onLogClick = { showWaterDialog = true }
                )

                // 4. WEIGHT CARD
                WeightMetricCard(
                    hazeState = hazeState,
                    weightKg = currentWeight,
                    diffText = weightDiffText,
                    onLogClick = { showWeightDialog = true }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = Color.White.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }

    // Water Quick-Log Dialog
    if (showWaterDialog) {
        WaterLogDialog(
            hazeState = hazeState,
            onDismiss = { showWaterDialog = false },
            onAddWater = { ml ->
                fitnessViewModel.logWater(ml)
                showWaterDialog = false
            }
        )
    }

    // Weight Quick-Log Dialog
    if (showWeightDialog) {
        WeightLogDialog(
            hazeState = hazeState,
            currentWeight = currentWeight,
            onDismiss = { showWeightDialog = false },
            onSaveWeight = { wt ->
                fitnessViewModel.logWeight(wt)
                showWeightDialog = false
            }
        )
    }
}

// ---------- Shared heading ----------

@Composable
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
        color = TextWhite,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold
    )
}

// ---------- Shared progress indicator ----------

@Composable
private fun GlassProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val safeFraction = if (fraction.isFinite()) {
        fraction.coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.10f))
    ) {
        if (safeFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(safeFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(PrimaryGreen)
            )
        }
    }
}

// ---------- Shared percentage pill ----------

@Composable
private fun ProgressPill(
    percent: Int
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(PrimaryGreen.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${percent.coerceIn(0, 100)}%",
            color = PrimaryGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------- Action Start / Log Pill ----------

@Composable
private fun ActionPill(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(PrimaryGreen.copy(alpha = 0.20f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = PrimaryGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------- Reusable metric card ----------

@Composable
private fun DashboardMetricCard(
    hazeState: HazeState,
    title: String,
    value: String,
    supportingText: String,
    fraction: Float,
    percent: Int,
    modifier: Modifier = Modifier,
    footer: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        hazeState = hazeState,
        blurRadius = 16.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                ProgressPill(percent)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = value,
                        color = TextWhite,
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = supportingText,
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }

                trailing?.invoke()
            }

            GlassProgressBar(
                fraction = fraction,
                modifier = Modifier.fillMaxWidth()
            )

            footer?.let {
                Text(
                    text = it,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ---------- Top Today's Workout Card ----------

@Composable
private fun WorkoutCardTop(
    hazeState: HazeState,
    title: String,
    exerciseCount: String,
    exercises: List<String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteExercise: (String) -> Unit,
    onStartClick: () -> Unit
) {
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onToggleExpand)
                    ) {
                        Text(
                            text = exerciseCount,
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                ActionPill(
                    text = "Start",
                    onClick = onStartClick
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exercises.forEachIndexed { index, ex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${index + 1}. $ex",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDeleteExercise(ex) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Workout completion ----------

@Composable
private fun TodayProgressCard(
    hazeState: HazeState,
    completed: Int,
    target: Int,
    fraction: Float,
    percent: Int,
    remainingExercises: List<String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteRemainingExercise: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        blurRadius = 16.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Workouts Completed",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                ProgressPill(percent)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = completed.toString(),
                        color = TextWhite,
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onToggleExpand)
                    ) {
                        Text(
                            text = "${(target - completed).coerceAtLeast(0)} remaining",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand remaining",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Image(
                    painter = painterResource(R.drawable.ic_barbell),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    remainingExercises.forEachIndexed { index, rem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${index + 1}. $rem",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDeleteRemainingExercise(rem) }
                            )
                        }
                    }
                }
            }

            GlassProgressBar(
                fraction = fraction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------- Calories ----------

@Composable
private fun CaloriesCard(
    hazeState: HazeState,
    consumed: Int,
    target: Int,
    fraction: Float,
    percent: Int,
    onClick: () -> Unit
) {
    DashboardMetricCard(
        modifier = Modifier.clickable(onClick = onClick),
        hazeState = hazeState,
        title = "Calories",
        value = consumed.toString(),
        supportingText = "of $target kcal",
        fraction = fraction,
        percent = percent,
        footer = if (consumed > target) {
            "${consumed - target} kcal over goal · Tap to log meals"
        } else {
            "${target - consumed} kcal remaining · Tap to log meals"
        },
        trailing = {
            ArrowBadge()
        }
    )
}

// ---------- Steps ----------

@Composable
private fun StepsCard(
    hazeState: HazeState,
    steps: Int,
    target: Int,
    fraction: Float,
    percent: Int
) {
    DashboardMetricCard(
        hazeState = hazeState,
        title = "Steps",
        value = steps.toString(),
        supportingText = "of $target steps",
        fraction = fraction,
        percent = percent,
        footer = if (steps >= target) {
            "Daily step goal reached"
        } else {
            "${target - steps} remaining"
        },
        trailing = {
            Image(
                painter = painterResource(R.drawable.ic_sneakers),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )
        }
    )
}

// ---------- Water Intake Card ----------

@Composable
private fun WaterIntakeCard(
    hazeState: HazeState,
    liters: Double,
    goalLiters: Double,
    fraction: Float,
    percent: Int,
    onLogClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        blurRadius = 16.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Water Intake",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                ProgressPill(percent)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", liters),
                        color = TextWhite,
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val remaining = (goalLiters - liters).coerceAtLeast(0.0)
                    Text(
                        text = "${String.format(Locale.US, "%.1f", remaining)} L remaining",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }

                ActionPill(
                    text = "Log",
                    onClick = onLogClick
                )
            }

            GlassProgressBar(
                fraction = fraction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------- Weight Card ----------

@Composable
private fun WeightMetricCard(
    hazeState: HazeState,
    weightKg: Double,
    diffText: String,
    onLogClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        blurRadius = 16.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Weight",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", weightKg)} Kg",
                        color = TextWhite,
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = diffText,
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }

                ActionPill(
                    text = "Log",
                    onClick = onLogClick
                )
            }
        }
    }
}

// ---------- Shared decorative arrow ----------

@Composable
private fun ArrowBadge() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ---------- Dialogs with Matching Glass/Border Modifiers ----------

@Composable
private fun WaterLogDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onAddWater: (Int) -> Unit
) {
    var customInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF000000))
                .border(1.dp, Color(0xFF3DDC84).copy(0.18f), RoundedCornerShape(14.dp))
//                .glassEffect(hazeState = hazeState, blurRadius = 24.dp, shadowEnabled = true)
//                .borderGradient()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Log Water Intake",
                color = PrimaryGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(200, 250, 500).forEach { amount ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryGreen.copy(alpha = 0.15f))
                            .clickable { onAddWater(amount) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$amount ml",
                            color = PrimaryGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (customInput.isEmpty()) {
                    Text(
                        text = "Or enter custom ml",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = customInput,
                    onValueChange = { customInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = TextWhite, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(PrimaryGreen)
                        .clickable {
                            val addVal = customInput.toIntOrNull() ?: 0
                            if (addVal > 0) onAddWater(addVal)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WeightLogDialog(
    hazeState: HazeState,
    currentWeight: Double,
    onDismiss: () -> Unit,
    onSaveWeight: (Double) -> Unit
) {
    var inputWeight by remember { mutableStateOf(String.format(Locale.US, "%.1f", currentWeight)) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF000000))
                .border(1.dp, Color(0xFF3DDC84).copy(0.18f), RoundedCornerShape(14.dp))
//                .glassEffect(hazeState = hazeState, blurRadius = 24.dp, shadowEnabled = true)
//                .borderGradient()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Log Weight",
                color = PrimaryGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.25, 0.5, 1.0).forEach { delta ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryGreen.copy(alpha = 0.15f))
                            .clickable {
                                val cur = inputWeight.toDoubleOrNull() ?: currentWeight
                                inputWeight = String.format(Locale.US, "%.2f", cur + delta)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$delta kg",
                            color = PrimaryGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputWeight.isEmpty()) {
                    Text(
                        text = "Enter weight in kg",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = inputWeight,
                    onValueChange = { inputWeight = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = TextWhite, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(PrimaryGreen)
                        .clickable {
                            val wt = inputWeight.toDoubleOrNull()
                            if (wt != null && wt > 0) onSaveWeight(wt)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}