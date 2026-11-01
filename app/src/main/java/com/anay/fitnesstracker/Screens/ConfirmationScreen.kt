package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.components.NextPillButton
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import java.text.NumberFormat
import java.util.Locale

private val CardDarkBg = Color(0xFF0F1E16)
private val CardBorderColor = Color(0xFF27D07F).copy(alpha = 0.22f)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF8E9094)

@Composable
fun ConfirmationScreen(
    viewModel: FitnessViewModel,
    onEnterDashboard: () -> Unit
) {
    val tempUser by viewModel.tempOnboarding.collectAsState()

    val formattedSteps = remember(tempUser.dailyStepGoal) {
        NumberFormat.getNumberInstance(Locale.US).format(tempUser.dailyStepGoal)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Main Screen Title
            Text(
                text = "Confirm Your\nPreferences",
                color = PrimaryGreen,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 1. Username Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(CardDarkBg)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(26.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Your Username",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tempUser.username,
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Save this username. You'll use it to log in.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Targets Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(CardDarkBg)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(26.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                LargeGoalItem(
                    title = "Daily Calories-Intake Goal",
                    value = "${tempUser.dailyCalorieGoal} Kcals"
                )

                LargeGoalItem(
                    title = "Daily Protein Goal",
                    value = "${tempUser.dailyProteinGoal} Grams"
                )

                LargeGoalItem(
                    title = "Daily Steps Goal",
                    value = "$formattedSteps Steps"
                )

                LargeGoalItem(
                    title = "Weekly Workout Split",
                    value = "${tempUser.workoutSplit} - ${tempUser.workoutFrequency.take(1)} days"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Bottom Next Button
        NextPillButton(
            onClick = {
                viewModel.confirmAndSaveProfile {
                    onEnterDashboard()
                }
            },
            modifier = Modifier.padding(bottom = 36.dp)
        )
    }
}

@Composable
private fun LargeGoalItem(title: String, value: String) {
    Column {
        Text(
            text = title,
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    }
}