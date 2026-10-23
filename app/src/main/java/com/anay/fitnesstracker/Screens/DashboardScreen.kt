package com.anay.fitnesstracker.Screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.anay.fitnesstracker.ui.theme.PrimaryGreen
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle

private val CardBackground = Color(0xFF070708)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9E9E)

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onNavigateMealLog: () -> Unit,
    fitnessViewModel: FitnessViewModel
) {
    val context = LocalContext.current
    val user by fitnessViewModel.currentUser.collectAsState()
    val calorieTarget = user?.dailyCalorieGoal ?: 2400
    val todayWorkout = WorkoutScheduleHelper.getTodayWorkout(user)

    // --- One-Time Physical Activity Permission Request on First Login ---
    val permissionPrefs = remember { context.getSharedPreferences("app_permission_prefs", Context.MODE_PRIVATE) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionPrefs.edit().putBoolean("activity_permission_prompted", true).apply()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            val hasPromptedBefore = permissionPrefs.getBoolean("activity_permission_prompted", false)

            if (!hasPermission && !hasPromptedBefore) {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    // --- Step Tracker Lifecycle ---
    val stepSensorManager = remember { StepSensorManager(context) }
    val currentSteps by stepSensorManager.todaySteps.collectAsState()

    DisposableEffect(Unit) {
        stepSensorManager.startListening()
        onDispose {
            stepSensorManager.stopListening()
        }
    }

    val stepGoal = 10000
    val stepsFraction = (currentSteps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
    val stepsPercent = (stepsFraction * 100).toInt()

    val targetExercisesToday = remember(todayWorkout.title, todayWorkout.exerciseCountText) {
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
            val digits = todayWorkout.exerciseCountText.filter { it.isDigit() }
            digits.toIntOrNull() ?: 5
        }
    }

    val startOfTodayMillis = remember {
        LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    val totalCaloriesConsumedToday = remember(user?.meals, startOfTodayMillis) {
        user?.meals
            ?.filter { it.timestamp >= startOfTodayMillis }
            ?.sumOf { it.calories } ?: 0
    }
    val caloriesFraction = if (calorieTarget > 0) {
        (totalCaloriesConsumedToday.toFloat() / calorieTarget.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val caloriesPercent = (caloriesFraction * 100).toInt()

    val completedExercisesToday = remember(user?.loggedWorkouts, startOfTodayMillis) {
        user?.loggedWorkouts
            ?.filter { it.timestamp >= startOfTodayMillis }
            ?.map { it.exerciseName }
            ?.distinct()
            ?.size ?: 0
    }

    val workoutProgressFraction = if (targetExercisesToday > 0) {
        (completedExercisesToday.toFloat() / targetExercisesToday.toFloat()).coerceIn(0f, 1f)
    } else {
        if (completedExercisesToday > 0) 1f else 0f
    }
    val workoutProgressPercent = (workoutProgressFraction * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()
    ) {
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
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Greetings, ${user?.name?.ifBlank { "Anay" } ?: "Anay"} 👋",
                    color = PrimaryGreen,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,

                    )

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "Today’s Progress",
                    color = PrimaryGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                TodayProgressCard(
                    completed = completedExercisesToday,
                    target = todayWorkout.exerciseCountText.filter { it.isDigit() }.toIntOrNull() ?: 5,
                    fraction = workoutProgressFraction,
                    percent = workoutProgressPercent
                )

                Spacer(modifier = Modifier.height(18.dp))

                CaloriesCard(
                    consumed = totalCaloriesConsumedToday,
                    target = calorieTarget,
                    fraction = caloriesFraction,
                    percent = caloriesPercent,
                    onClick = onNavigateMealLog
                )

                Spacer(modifier = Modifier.height(18.dp))

                StepsCard(
                    steps = currentSteps,
                    target = stepGoal,
                    fraction = stepsFraction,
                    percent = stepsPercent
                )

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = "Today’s Workout",
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryGreen,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                WorkoutCard(
                    title = todayWorkout.title,
                    exerciseCount = todayWorkout.exerciseCountText,
                    onClick = {
                        todayWorkout.route?.let { onNavigate(it) }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    color = Color(0xFF8E9094).copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                AppBottomNavigationBar(
                    currentRoute = Routes.DASHBOARD,
                    onNavigate = onNavigate
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TodayProgressCard(
    completed: Int,
    target: Int,
    fraction: Float,
    percent: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()

            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFF3DDC84).copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .background(Color(0x1A3DDC84))
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Workouts Completed",
                    color = TextWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$completed / $target",
                    color = TextWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(13.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x805E6065))
                            .border(1.dp, Color(0xFFffffff).copy(alpha = 0.18f), RoundedCornerShape(24.dp))

                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x80E4E4E6))
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "$percent%",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_barbell),
                    contentDescription = "Workouts",
                    modifier = Modifier.size(52.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun CaloriesCard(
    consumed: Int,
    target: Int,
    fraction: Float,
    percent: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x1A3DDC84))
            .border(1.dp, Color(0xFF3DDC84).copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left content column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Calories",
                    color = TextWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$consumed / $target ",
                        color = TextWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp
                    )


                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(13.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x805E6065))
                            .border(1.dp, Color(0xFFffffff).copy(alpha = 0.18f), RoundedCornerShape(24.dp))

                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x80E4E4E6))
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "$percent%",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Click on the card to log your meals",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {


                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TextWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Log meals",
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepsCard(
    steps: Int,
    target: Int,
    fraction: Float,
    percent: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x1A3DDC84))
            .border(1.dp, Color(0xFF3DDC84).copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Steps",
                    color = TextWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$steps / $target ",
                        color = TextWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp
                    )

                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(13.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xff5E6065))
                            .border(1.dp, Color(0x80ffffff).copy(alpha = 0.18f), RoundedCornerShape(24.dp))

                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x80E4E4E6))
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "$percent%",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_sneakers),
                    contentDescription = "Steps Sneakers",
                    modifier = Modifier.size(52.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    title: String,
    exerciseCount: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x1A3DDC84))
            .border(1.dp, Color(0xFF3DDC84).copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextWhite,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exerciseCount,
                color = TextWhite,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Click on the card to start a workout",
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(TextWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Start Workout",
                tint = Color.Black,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}