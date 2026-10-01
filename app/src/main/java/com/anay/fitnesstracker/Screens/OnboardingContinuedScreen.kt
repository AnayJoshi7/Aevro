package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.Icon
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
import com.anay.fitnesstracker.ui.theme.*
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.components.SplashBottomIcons
import com.anay.fitnesstracker.components.SplashTopIcons
import com.anay.fitnesstracker.components.icons



@Composable
fun OnboardingContinuedScreen(
    viewModel: FitnessViewModel,
    onNext: () -> Unit
) {
    val tempUser by viewModel.tempOnboarding.collectAsState()
    val goals = listOf("Maintain Weight", "Lose Weight", "Gain Weight")
    var selectedGoal by remember { mutableStateOf(tempUser.fitnessGoal) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()

            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            SplashTopIcons()


            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Your Maintenance\nCalories",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "${tempUser.maintenanceCalories} kcal", color = TextWhite, fontSize = 20.sp)

                Spacer(modifier = Modifier.height(22.dp))

                Text(text = "Your BMI", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "${tempUser.bmi} - ${tempUser.bmiCategory}", color = TextWhite, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
                    .padding(24.dp)
            ) {
                Text(text = "Select your fitness goal", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(18.dp))

                goals.forEach { goal ->
                    val isSelected = goal == selectedGoal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF3DDC84).copy(alpha = 0.80f) else Color.White)
                            .clickable {
                                selectedGoal = goal
                                viewModel.updateGoal(goal)
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = goal,
                            color = if (isSelected) TextWhite else Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            icons()
            Spacer(modifier = Modifier.height(24.dp))
        }

        NextPillButton(
            onClick = {
                viewModel.updateGoal(selectedGoal)
                onNext()
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}