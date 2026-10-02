package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.components.NextPillButton
import com.anay.fitnesstracker.ui.theme.*
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.components.icons


@Composable
fun ConfirmationScreen(
    viewModel: FitnessViewModel,
    onEnterDashboard: () -> Unit
) {

    val tempUser by viewModel.tempOnboarding.collectAsState()

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
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Confirm Your\nPreferences",
                color = PrimaryGreen,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardBackground)
                    .padding(20.dp)
            ) {
                Text("Your Username", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(tempUser.username, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Save this username. You’ll use it to log in.", color = TextMuted, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardBackground)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GoalItem(title = "Daily Calories-Intake Goal", value = "${tempUser.dailyCalorieGoal} Kcals")
                GoalItem(title = "Daily Protein Goal", value = "${tempUser.dailyProteinGoal} Grams")
                GoalItem(title = "Daily Steps Goal", value = "${tempUser.dailyStepGoal} Steps")
                GoalItem(title = "Weekly Workout Split", value = "${tempUser.workoutSplit} (${tempUser.workoutFrequency})")
            }

            Spacer(modifier = Modifier.height(24.dp))
            icons()
        }

        NextPillButton(
            onClick = {
                viewModel.confirmAndSaveProfile {
                    onEnterDashboard()
                }
            },
            modifier = Modifier.padding(bottom = 60.dp)
        )
    }
}

@Composable
private fun GoalItem(title: String, value: String) {
    Column {
        Text(text = title, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = TextWhite, fontSize = 15.sp)
    }
}