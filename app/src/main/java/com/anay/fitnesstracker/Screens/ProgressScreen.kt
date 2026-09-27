package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.screens.BottomNavigationBar
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoField

private val CardBackground = Color(0xFF070708)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val BarDefaultBg = Color(0xFF333539)

@Composable
fun ProgressScreen(
    onNavigate: (String) -> Unit,
    fitnessViewModel: FitnessViewModel
) {
    val user by fitnessViewModel.currentUser.collectAsState()
    val loggedWorkouts = user?.loggedWorkouts ?: emptyList()
    val meals = user?.meals ?: emptyList()

    // Aggregate counts for Monday (1) through Saturday (6)
    val dayCounts = remember(loggedWorkouts) {
        val map = mutableMapOf<Int, Int>()
        for (i in 1..6) map[i] = 0

        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)

        loggedWorkouts.filter { it.timestamp >= oneWeekAgo }.forEach { exercise ->
            val dayOfWeek = Instant.ofEpochMilli(exercise.timestamp)
                .atZone(ZoneId.systemDefault())
                .get(ChronoField.DAY_OF_WEEK)
            if (dayOfWeek in 1..6) {
                map[dayOfWeek] = (map[dayOfWeek] ?: 0) + 1
            }
        }
        map
    }


    val maxCount = (dayCounts.values.maxOrNull() ?: 0).coerceAtLeast(1)

    // Weekly calories calculations: Target = dailyCalorieGoal * 7
    val dailyCalGoal = user?.dailyCalorieGoal ?: 2000
    val targetWeeklyCalories = dailyCalGoal * 7
    val totalCaloriesConsumedWeek = meals.sumOf { it.calories }
    val calorieProgressFraction = (totalCaloriesConsumedWeek.toFloat() / targetWeeklyCalories.toFloat()).coerceIn(0f, 1f)
    val calorieProgressPercent = (calorieProgressFraction * 100).toInt()

    val totalWorkoutsThisWeek = loggedWorkouts.size

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
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Progress",
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Dynamic Weekly Report Card (Mon - Sat)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
                    .padding(22.dp)
            ) {
                Text(
                    text = "Weekly Report",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bar Graph: M, T, W, T, F, S (No Sunday)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val dayLabels = listOf("M", "T", "W", "T", "F", "S")
                    for (dayIndex in 1..6) {
                        val count = dayCounts[dayIndex] ?: 0
                        // Min height 18.dp, max 100.dp scaled dynamically
                        val barHeightDp = 18.dp + (82.dp * (count.toFloat() / maxCount.toFloat()))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(barHeightDp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(if (count > 0) PrimaryGreen else BarDefaultBg)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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

            // Dynamic "This Week" Progress Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
                    .padding(22.dp)
            ) {
                Text(
                    text = "This Week",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Workouts count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Workouts", color = TextWhite, fontSize = 15.sp)
                    Text("$totalWorkoutsThisWeek Logged", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calories consumed vs weekly target
                Text("Calories", color = TextWhite, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalCaloriesConsumedWeek / $targetWeeklyCalories Kcal",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Calorie Progress Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF5E6065))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(calorieProgressFraction)
                                .fillMaxHeight()
                                .background(PrimaryGreen)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = Color(0xFF8E9094), thickness = 1.dp, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(modifier = Modifier.height(14.dp))
            BottomNavigationBar(selectedTab = "Progress", onNavigate = onNavigate)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}