package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anay.fitnesstracker.Routes
import com.anay.fitnesstracker.data.WorkoutScheduleHelper
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.data.viewmodel.QuoteViewModel


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
    fitnessViewModel: FitnessViewModel,
    quoteViewModel: QuoteViewModel = viewModel()
) {
    val user by fitnessViewModel.currentUser.collectAsState()
    val totalCaloriesConsumed = user?.meals?.sumOf { it.calories } ?: 0
    val calorieTarget = user?.dailyCalorieGoal ?: 2200

    val quote = quoteViewModel.quote.collectAsState().value
    val isLoading = quoteViewModel.isLoading.collectAsState().value
    val error = quoteViewModel.error.collectAsState().value
    val todayWorkout = WorkoutScheduleHelper.getTodayWorkout(user)


    val targetExercisesToday = remember(todayWorkout.title) {
        when (todayWorkout.title) {
            "Push Day" -> 8
            "Pull Day" -> 9
            "Leg Day" -> 8
            "Upper Body" -> 9
            "Lower Body" -> 7
            "Full Body" -> 9
            else -> 0
        }
    }

    // 2. Count distinct logged exercises for today (from midnight onwards)
    val startOfTodayMillis = remember {
        java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    val totalCaloriesConsumedToday = remember(user?.meals, startOfTodayMillis) {
        user?.meals
            ?.filter { it.timestamp >= startOfTodayMillis }
            ?.sumOf { it.calories } ?: 0
    }

    val completedExercisesToday = remember(user?.loggedWorkouts) {
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
            .background(backgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Greeting
            Text(
                text = "Greetings, ${user?.name?.ifBlank { "Anay" } ?: "Anay"} 👋",
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Dynamic Today's Progress Card
            TodayProgressCard(
                completed = completedExercisesToday,
                target = targetExercisesToday,
                fraction = progressFraction,
                percent = progressPercent
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Statistics Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateMealLog() }
                ) {
                    StatCard(
                        title = "Calories",
                        value = "$totalCaloriesConsumed",
                        unit = "of $calorieTarget Kcal"
                    )
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    StatCard(
                        title = "Steps",
                        value = "8,341",
                        unit = "of ${user?.dailyStepGoal ?: 10000} steps"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Daily Motivation
            Text(
                text = "Quote Of The Day",
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryGreen,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            MotivationCard(
                quote = quote?.quote,
                author = quote?.author,
                isLoading = isLoading,
                error = error,
                onRetry = {
                    quoteViewModel.loadQuote()
                }
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

            Spacer(modifier = Modifier.height(18.dp))

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

        // Fixed bottom container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                color = Color(0xFF8E9094),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            BottomNavigationBar(onNavigate = onNavigate)

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
            .padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Today’s Progress",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (target > 0) "$completed/$target" else "$completed",
            color = TextWhite,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (target > 0) "Workouts Completed" else "Rest Day Activity",
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Dynamic Horizontal Progress Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 12.dp)
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
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MotivationCard(
    quote: String?,
    author: String?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF000000)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Unable to load quote of the day",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Tap Retry to try again",
                            color = TextMuted,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Retry",
                            color = PrimaryGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                onRetry()
                            }
                        )
                    }
                }

                quote != null -> {
                    Text(
                        text = "\"$quote\"",
                        color = TextWhite,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "— $author",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    unit: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.50f)
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = value,
            color = TextWhite,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        if (unit.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = unit,
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
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
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = muscles, color = TextWhite, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = exerciseCount, color = TextMuted, fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(TextWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Start Workout",
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(BottomNavBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = Icons.Default.Home,
            label = "Home",
            selected = true,
            onClick = { onNavigate(Routes.DASHBOARD) }
        )
        BottomNavItem(
            icon = Icons.Default.FitnessCenter,
            label = "Workouts",
            selected = false,
            onClick = { onNavigate(Routes.WORKOUTS) }
        )
        BottomNavItem(
            icon = Icons.Default.Leaderboard,
            label = "Progress",
            selected = false,
            onClick = { onNavigate(Routes.PROGRESS) }
        )
        BottomNavItem(
            icon = Icons.Default.Person,
            label = "Profile",
            selected = false,
            onClick = { onNavigate(Routes.PROFILE) }
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) SelectedTabBg else Color.Transparent)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = BottomNavIconBg,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = BottomNavIconBg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}