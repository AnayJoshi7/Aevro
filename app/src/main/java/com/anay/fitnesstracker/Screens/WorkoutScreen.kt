package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.Routes
import com.anay.fitnesstracker.data.WorkoutScheduleHelper
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.components.AppBottomNavigationBar


private val CardBackground = Color(0xFF070708)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9E9E)
private val BottomNavBg = Color(0xFF6B6E70)
private val BottomNavIconBg = Color(0xFF1E1E1E)
private val SelectedTabBg = Color(0xFF3DDC84).copy(alpha = 0.30f)

data class WorkoutCardData(
    val title: String,
    val muscleGroups: String = "",
    val exerciseCountText: String,
    val route: String
)

@Composable
fun WorkoutScreen(
    onNavigate: (String) -> Unit,
    fitnessViewModel: FitnessViewModel
) {
    val user by fitnessViewModel.currentUser.collectAsState()

    // 1. Current Day Identification (Monday .. Sunday)
    val todayDayName = remember {
        java.time.LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    // 2. Resolve Today's Workout (Checking Custom Schedule First)
    val todayCustomDay = user?.customSchedule?.find {
        it.dayName.equals(todayDayName, ignoreCase = true) && it.muscleGroup.isNotBlank()
    }

    val todayWorkoutPreset = WorkoutScheduleHelper.getTodayWorkout(user)

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

    val todayExerciseCountText = if (todayCustomDay != null) {
        "${todayCustomDay.exercises.size} Exercises"
    } else {
        todayWorkoutPreset.exerciseCountText
    }

    val todayRoute = if (todayCustomDay != null) {
        "custom_workout_detail/${todayCustomDay.dayName}"
    } else {
        todayWorkoutPreset.route ?: Routes.LOG_WORKOUT
    }

    // 3. Resolve Other Workouts (Custom Schedule vs Presets)
    val hasCustomSchedule = user?.customSchedule?.any { it.muscleGroup.isNotBlank() } == true

    val otherWorkouts: List<WorkoutCardData> = if (hasCustomSchedule) {
        user?.customSchedule
            ?.filter {
                it.muscleGroup.isNotBlank() && !it.dayName.equals(todayDayName, ignoreCase = true)
            }
            ?.map { day ->
                WorkoutCardData(
                    title = day.muscleGroup,
                    muscleGroups = "",
                    exerciseCountText = "${day.exercises.size} Exercises",
                    route = "custom_workout_detail/${day.dayName}"
                )
            } ?: emptyList()
    } else {
        val defaultWorkouts = when (user?.workoutFrequency) {
            "3 Days a Week" -> listOf(
                WorkoutCardData("Full Body", "Full Body", "9 Exercises", Routes.WORKOUT_FULL_BODY)
            )
            "4 Days a Week" -> listOf(
                WorkoutCardData("Upper Body", "Chest, Back, Shoulders", "9 Exercises", Routes.WORKOUT_UPPER_BODY),
                WorkoutCardData("Lower Body", "Quads, Hamstrings, Calves", "7 Exercises", Routes.WORKOUT_LOWER_BODY)
            )
            "5 Days a Week" -> listOf(
                WorkoutCardData("Upper Body", "Chest, Back, Shoulders", "9 Exercises", Routes.WORKOUT_UPPER_BODY),
                WorkoutCardData("Lower Body", "Quads, Hamstrings, Calves", "7 Exercises", Routes.WORKOUT_LOWER_BODY),
                WorkoutCardData("Push Day", "Chest, Shoulder, Triceps", "7 Exercises", Routes.WORKOUT_PUSH_DAY),
                WorkoutCardData("Pull Day", "Back, Biceps", "8 Exercises", Routes.WORKOUT_PULL_DAY),
                WorkoutCardData("Leg Day", "Legs, Abs", "9 Exercises", Routes.WORKOUT_LEG_DAY)
            )
            else -> listOf(
                WorkoutCardData("Push Day", "Chest, Shoulder, Triceps", "7 Exercises", Routes.WORKOUT_PUSH_DAY),
                WorkoutCardData("Pull Day", "Back, Biceps", "8 Exercises", Routes.WORKOUT_PULL_DAY),
                WorkoutCardData("Leg Day", "Legs, Abs", "9 Exercises", Routes.WORKOUT_LEG_DAY)
            )
        }
        defaultWorkouts.filter { it.title != todayTitle }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1B1C1E),
            Color(0xFF131416),
            Color(0xFF2C2E31),
            Color(0xFF111214)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Scrollable content area with sweet spacing
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
                title = "Log Workout",
                onClick = { onNavigate(Routes.LOG_WORKOUT) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Full-Width Create Custom Schedule Pill Button
            ActionPillButton(
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




            // Today's Clickable Card
            WorkoutDayClickableCard(
                title = todayTitle,
                muscleGroups = todayMuscles,
                exerciseCountText = todayExerciseCountText,
                onClick = { onNavigate(todayRoute) }
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

                // Other Clickable Cards
                otherWorkouts.forEach { workout ->
                    WorkoutDayClickableCard(
                        title = workout.title,
                        muscleGroups = workout.muscleGroups,
                        exerciseCountText = workout.exerciseCountText,
                        onClick = { onNavigate(workout.route) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }


        }

        // Fixed bottom navigation bar
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
                currentRoute = Routes.WORKOUTS,
                onNavigate = onNavigate
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun WorkoutDayClickableCard(
    title: String,
    muscleGroups: String = "",
    exerciseCountText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
                text = exerciseCountText,
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open $title",
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ActionPillButton(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
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

//@Composable
//private fun WorkoutsBottomNavigationBar(
//    onNavigate: (String) -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(64.dp)
//            .clip(RoundedCornerShape(22.dp))
//            .background(BottomNavBg)
//            .padding(horizontal = 8.dp, vertical = 6.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        WorkoutsBottomNavItem(
//            icon = Icons.Default.Home,
//            label = "Home",
//            selected = false,
//            onClick = { onNavigate(Routes.DASHBOARD) }
//        )
//        WorkoutsBottomNavItem(
//            icon = Icons.Default.FitnessCenter,
//            label = "Workouts",
//            selected = true,
//            onClick = { onNavigate(Routes.WORKOUTS) }
//        )
//        WorkoutsBottomNavItem(
//            icon = Icons.Default.Leaderboard,
//            label = "Progress",
//            selected = false,
//            onClick = { onNavigate(Routes.PROGRESS) }
//        )
//        WorkoutsBottomNavItem(
//            icon = Icons.Default.Person,
//            label = "Profile",
//            selected = false,
//            onClick = { onNavigate(Routes.PROFILE) }
//        )
//    }
//}
//
//@Composable
//private fun RowScope.WorkoutsBottomNavItem(
//    icon: ImageVector,
//    label: String,
//    selected: Boolean,
//    onClick: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .weight(1f)
//            .fillMaxHeight()
//            .clip(RoundedCornerShape(16.dp))
//            .background(if (selected) SelectedTabBg else Color.Transparent)
//            .clickable { onClick() },
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Icon(
//            imageVector = icon,
//            contentDescription = label,
//            tint = BottomNavIconBg,
//            modifier = Modifier.size(22.dp)
//        )
//
//        Spacer(modifier = Modifier.height(2.dp))
//
//        Text(
//            text = label,
//            color = BottomNavIconBg,
//            fontSize = 11.sp,
//            fontWeight = FontWeight.SemiBold,
//            maxLines = 1
//        )
//    }
//}