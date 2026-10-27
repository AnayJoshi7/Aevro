package com.anay.fitnesstracker.Screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.Routes
import com.anay.fitnesstracker.components.AppBottomNavigationBar
import com.anay.fitnesstracker.data.StepSensorManager
import com.anay.fitnesstracker.data.WorkoutScheduleHelper
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.GlassCard
import com.anay.fitnesstracker.ui.theme.PrimaryGreen
import com.anay.fitnesstracker.ui.theme.TextMuted
import com.anay.fitnesstracker.ui.theme.TextWhite
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.LocalDate
import java.time.ZoneId

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

    val stepGoal = 10_000

    val stepsFraction =
        (currentSteps.toFloat() / stepGoal).coerceIn(0f, 1f)

    val stepsPercent = (stepsFraction * 100).toInt()

    // ---------- Workout target ----------

    val targetExercisesToday = remember(
        todayWorkout.title,
        todayWorkout.exerciseCountText
    ) {
        val mappedCount = when (todayWorkout.title) {
            "Push Day" -> 5
            "Pull Day" -> 6
            "Leg Day" -> 5
            "Upper Body" -> 6
            "Lower Body" -> 5
            "Full Body" -> 6
            else -> 0
        }

        if (mappedCount > 0) {
            mappedCount
        } else {
            todayWorkout.exerciseCountText
                .filter { it.isDigit() }
                .toIntOrNull() ?: 5
        }
    }

    // ---------- Today's totals ----------

    val startOfTodayMillis = remember {
        LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

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

    val completedExercisesToday = remember(
        user?.loggedWorkouts,
        startOfTodayMillis
    ) {
        user?.loggedWorkouts
            ?.filter { it.timestamp >= startOfTodayMillis }
            ?.map { it.exerciseName }
            ?.distinct()
            ?.size ?: 0
    }

    val workoutProgressFraction = if (targetExercisesToday > 0) {
        (
                completedExercisesToday.toFloat() /
                        targetExercisesToday
                ).coerceIn(0f, 1f)
    } else {
        if (completedExercisesToday > 0) 1f else 0f
    }

    val workoutProgressPercent =
        (workoutProgressFraction * 100).toInt()

    // ---------- Screen ----------

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeSource(state = hazeState)
                .appBackgroundGradient()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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

                SectionTitle("Today’s Progress")

                TodayProgressCard(
                    hazeState = hazeState,
                    completed = completedExercisesToday,
                    target = targetExercisesToday,
                    fraction = workoutProgressFraction,
                    percent = workoutProgressPercent
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

                Spacer(modifier = Modifier.height(4.dp))

                SectionTitle("Today’s Workout")

                WorkoutCard(
                    hazeState = hazeState,
                    title = todayWorkout.title,
                    exerciseCount = todayWorkout.exerciseCountText,
                    onClick = {
                        todayWorkout.route?.let(onNavigate)
                    }
                )
            }

            // Existing navigation component is retained.
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
//                AppBottomNavigationBar(
//                    currentRoute = Routes.DASHBOARD,
//                    onNavigate = onNavigate,
//                    hazeState = hazeState
//                )

            }
        }
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
        blurRadius = 24.dp,
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

// ---------- Workout completion ----------

@Composable
private fun TodayProgressCard(
    hazeState: HazeState,
    completed: Int,
    target: Int,
    fraction: Float,
    percent: Int
) {
    DashboardMetricCard(
        hazeState = hazeState,
        title = "Workouts Completed",
        value = completed.toString(),
        supportingText = "of $target exercises",
        fraction = fraction,
        percent = percent,
        footer = "${(target - completed).coerceAtLeast(0)} remaining",
        trailing = {
            Image(
                painter = painterResource(R.drawable.ic_barbell),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )
        }
    )
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

// ---------- Today's workout ----------

@Composable
private fun WorkoutCard(
    hazeState: HazeState,
    title: String,
    exerciseCount: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        hazeState = hazeState,
        blurRadius = 24.dp,
        contentPadding = PaddingValues(20.dp)
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
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = exerciseCount,
                    color = TextMuted,
                    fontSize = 14.sp
                )

                Text(
                    text = "Tap to start your workout",
                    color = PrimaryGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            ArrowBadge()
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
