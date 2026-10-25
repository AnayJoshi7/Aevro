package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.Routes
import com.anay.fitnesstracker.components.AppBottomNavigationBar
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import com.anay.fitnesstracker.ui.theme.glassEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters

private val CardBackground = Color(0x1A3DDC84)
private val PrimaryGreen = Color(0xFF3DDC84)
private val TextWhite = Color.White
private val BarDefaultBg = Color(0xFF333539)

@Composable
fun ProgressScreen(
    onNavigate: (String) -> Unit,
    fitnessViewModel: FitnessViewModel
) {
    val hazeState = rememberHazeState()

    val user by fitnessViewModel.currentUser.collectAsState()
    val loggedWorkouts = user?.loggedWorkouts ?: emptyList()
    val meals = user?.meals ?: emptyList()

    // Exact start of current week: Monday at midnight.
    val startOfWeekMillis = remember {
        LocalDate.now()
            .with(
                TemporalAdjusters.previousOrSame(
                    DayOfWeek.MONDAY
                )
            )
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    val thisWeekWorkouts = remember(
        loggedWorkouts,
        startOfWeekMillis
    ) {
        loggedWorkouts.filter {
            it.timestamp >= startOfWeekMillis
        }
    }

    // Monday through Saturday, preserving your existing logic.
    val dayCounts = remember(thisWeekWorkouts) {
        val map = mutableMapOf<Int, Int>()

        for (i in 1..6) {
            map[i] = 0
        }

        thisWeekWorkouts.forEach { exercise ->
            val dayOfWeek = Instant
                .ofEpochMilli(exercise.timestamp)
                .atZone(ZoneId.systemDefault())
                .get(ChronoField.DAY_OF_WEEK)

            if (dayOfWeek in 1..6) {
                map[dayOfWeek] = (map[dayOfWeek] ?: 0) + 1
            }
        }

        map
    }

    val maxCount = (dayCounts.values.maxOrNull() ?: 0)
        .coerceAtLeast(1)

    val dailyCalGoal = user?.dailyCalorieGoal ?: 2000
    val targetWeeklyCalories = dailyCalGoal * 7

    val totalCaloriesConsumedWeek = remember(
        meals,
        startOfWeekMillis
    ) {
        meals
            .filter { it.timestamp >= startOfWeekMillis }
            .sumOf { it.calories }
    }

    val calorieProgressFraction =
        if (targetWeeklyCalories > 0) {
            (
                    totalCaloriesConsumedWeek.toFloat() /
                            targetWeeklyCalories.toFloat()
                    ).coerceIn(0f, 1f)
        } else {
            0f
        }

    val calorieProgressPercent =
        (calorieProgressFraction * 100).toInt()

    val totalWorkoutsThisWeek = thisWeekWorkouts.size

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeSource(state = hazeState)
                .appBackgroundGradient()
        )

        // Foreground content is a sibling of the background.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.Start
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
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Progress",
                    color = PrimaryGreen,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ---------- Weekly Report card ----------

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(
                            hazeState = hazeState,
                            blurRadius = 24.dp,
                            shadowEnabled = true
                        )
                        .padding(22.dp)
                ) {
                    Text(
                        text = "Weekly Report",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val dayLabels =
                            listOf("M", "T", "W", "T", "F", "S")

                        for (dayIndex in 1..6) {
                            val count = dayCounts[dayIndex] ?: 0

                            val barHeightDp = 18.dp + (
                                    82.dp * (
                                            count.toFloat() /
                                                    maxCount.toFloat()
                                            )
                                    )

                            val barShape = RoundedCornerShape(
                                topStart = 8.dp,
                                topEnd = 8.dp
                            )

                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(barHeightDp)
                                        .clip(barShape)
                                        .background(
                                            if (count > 0) {
                                                Color(0x803DDC84)
                                            } else {
                                                BarDefaultBg
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = PrimaryGreen.copy(
                                                alpha = 0.5f
                                            ),
                                            shape = barShape
                                        )
                                )

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                Text(
                                    text = dayLabels[dayIndex - 1],
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ---------- This Week card ----------

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(
                            hazeState = hazeState,

                            blurRadius = 24.dp,
                            shadowEnabled = true
                        )
                        .padding(22.dp)
                ) {
                    Text(
                        text = "This Week",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Workouts",
                            color = TextWhite,
                            fontSize = 15.sp
                        )

                        Text(
                            text = "$totalWorkoutsThisWeek Logged",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Calories",
                        color = TextWhite,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$totalCaloriesConsumedWeek / " +
                                "$targetWeeklyCalories Kcal",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        // Normal progress bar: no backdrop effect.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(
                                    RoundedCornerShape(24.dp)
                                )
                                .background(
                                    Color(0x805E6065)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(
                                        alpha = 0.18f
                                    ),
                                    shape = RoundedCornerShape(
                                        24.dp
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(
                                        calorieProgressFraction
                                    )
                                    .fillMaxHeight()
                                    .clip(
                                        RoundedCornerShape(24.dp)
                                    )
                                    .background(
                                        Color(0x803DDC84)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = PrimaryGreen.copy(
                                            alpha = 0.18f
                                        ),
                                        shape = RoundedCornerShape(
                                            24.dp
                                        )
                                    )
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(14.dp)
                        )

                        Text(
                            text = "$calorieProgressPercent%",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ---------- Fixed bottom navigation ----------

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    color = Color(0xFF8E9094),
                    thickness = 1.dp,
                    modifier = Modifier.padding(
                        horizontal = 4.dp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                AppBottomNavigationBar(
                    currentRoute = Routes.PROGRESS,
                    onNavigate = onNavigate
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
