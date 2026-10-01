package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient

import java.time.LocalDate
import java.time.ZoneId

private val CardBackground = Color(0xFF070708)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9E9E)
private val BottomNavBg = Color(0xFF6B6E70)
private val BottomNavIconBg = Color(0xFF1E1E1E)
private val SelectedTabBg = Color(0xFF3DDC84).copy(alpha = 0.30f)

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onNavigateMealLog: () -> Unit,
    fitnessViewModel: FitnessViewModel
) {
    val user by fitnessViewModel.currentUser.collectAsState()
    val calorieTarget = user?.dailyCalorieGoal ?: 2400
    val todayWorkout = WorkoutScheduleHelper.getTodayWorkout(user)

    // 1. Determine target exercises for today: check title or parse from exerciseCountText (e.g., "7 Exercises" -> 7)
    val targetExercisesToday = remember(todayWorkout.title, todayWorkout.exerciseCountText) {
        val mappedCount = when (todayWorkout.title) {
            "Push Day" -> 8
            "Pull Day" -> 9
            "Leg Day" -> 8
            "Upper Body" -> 9
            "Lower Body" -> 7
            "Full Body" -> 9
            else -> 0
        }
        if (mappedCount > 0) {
            mappedCount
        } else {
            // Fallback: extract first number found in exerciseCountText (e.g. "8 Exercises" -> 8, defaults to 5)
            val digits = todayWorkout.exerciseCountText.filter { it.isDigit() }
            digits.toIntOrNull() ?: 5
        }
    }

    // 2. Start of today (midnight) for resetting metrics daily
    val startOfTodayMillis = remember {
        LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    // Daily calories consumed (filtered for today only)
    val totalCaloriesConsumedToday = remember(user?.meals, startOfTodayMillis) {
        user?.meals
            ?.filter { it.timestamp >= startOfTodayMillis }
            ?.sumOf { it.calories } ?: 0
    }

    // Distinct logged exercises completed today
    val completedExercisesToday = remember(user?.loggedWorkouts, startOfTodayMillis) {
        user?.loggedWorkouts
            ?.filter { it.timestamp >= startOfTodayMillis }
            ?.map { it.exerciseName }
            ?.distinct()
            ?.size ?: 0
    }

    // 3. Compute dynamic progress ratio and percentage
    val progressFraction = if (targetExercisesToday > 0) {
        (completedExercisesToday.toFloat() / targetExercisesToday.toFloat()).coerceIn(0f, 1f)
    } else {
        if (completedExercisesToday > 0) 1f else 0f
    }
    val progressPercent = (progressFraction * 100).toInt()

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
            .appBackgroundGradient()

            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))


        // Main scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Greeting matching new design
            Text(
                text = "Greetings, ${user?.name?.ifBlank { "Anay" } ?: "Anay"} 👋",
                color = PrimaryGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Dynamic Today's Progress Card
            TodayProgressCard(
                completed = completedExercisesToday,
                target = targetExercisesToday,
                fraction = progressFraction,
                percent = progressPercent
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Calories Card (Matches design on right)
            CaloriesCard(
                consumed = totalCaloriesConsumedToday,
                target = calorieTarget,
                onClick = onNavigateMealLog
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Today's Workout Section Header
            Text(
                text = "Today’s Workout",
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryGreen,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Today's Workout Card
            WorkoutCard(
                title = todayWorkout.title,
                muscles = todayWorkout.muscleGroups,
                exerciseCount = todayWorkout.exerciseCountText,
                onClick = {
                    todayWorkout.route?.let { onNavigate(it) }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Fixed bottom navigation container
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

@Composable
private fun TodayProgressCard(
    completed: Int,
    target: Int,
    fraction: Float,
    percent: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(CardBackground)
            .padding(vertical = 24.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Today’s Progress",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "$completed/$target",
            color = TextWhite,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Workouts Completed",
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar and % representation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(width = 130.dp, height = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF5E6065))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFE4E4E6))
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "$percent%",
                color = TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }



    }
}

@Composable
private fun CaloriesCard(
    consumed: Int,
    target: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Text(
            text = "Calories",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$consumed",
                    color = TextWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " /$target",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Text(
                text = "Kcal",
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Click to log your meals",
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(TextWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Log Meals",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    title: String,
    muscles: String,
    exerciseCount: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = muscles,
                color = TextWhite,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = exerciseCount,
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(TextWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Start Workout",
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


//@Composable
//private fun BottomNavigationBar(
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
//        BottomNavItem(
//            icon = Icons.Default.Home,
//            label = "Home",
//            selected = true,
//            onClick = { onNavigate(Routes.DASHBOARD) }
//        )
//        BottomNavItem(
//            icon = Icons.Default.FitnessCenter,
//            label = "Workouts",
//            selected = false,
//            onClick = { onNavigate(Routes.WORKOUTS) }
//        )
//        BottomNavItem(
//            icon = Icons.Default.Leaderboard,
//            label = "Progress",
//            selected = false,
//            onClick = { onNavigate(Routes.PROGRESS) }
//        )
//        BottomNavItem(
//            icon = Icons.Default.Person,
//            label = "Profile",
//            selected = false,
//            onClick = { onNavigate(Routes.PROFILE) }
//        )
//    }
//}
//
//@Composable
//private fun RowScope.BottomNavItem(
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