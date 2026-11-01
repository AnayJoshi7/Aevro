package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.components.NextPillButton
import com.anay.fitnesstracker.data.FitnessCalculator
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient

private val CardDarkBg = Color(0xFF0F1E16)
private val CardBorderColor = Color(0xFF27D07F).copy(alpha = 0.22f)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val GrayPillBg = Color(0xFFC7CDC9)
private val SelectedGreenPillBg = Color(0xFF27D07F)

@Composable
fun OnboardingContinuedScreen(
    viewModel: FitnessViewModel,
    onNext: () -> Unit
) {
    val tempUser by viewModel.tempOnboarding.collectAsState()

    val activityOptions = listOf("Highly active", "Moderately active", "Lightly active")
    var selectedActivity by remember { mutableStateOf("Moderately active") }

    var targetWeightText by remember { mutableStateOf(if (tempUser.weightKg > 0) tempUser.weightKg.toString() else "70.0") }
    var targetStepsText by remember { mutableStateOf(if (tempUser.dailyStepGoal > 0) tempUser.dailyStepGoal.toString() else "10000") }
    var targetWaterText by remember { mutableStateOf(if (tempUser.dailyWaterGoalLiters > 0) tempUser.dailyWaterGoalLiters.toString() else "3.5") }

    // Dynamically calculate Maintenance Calories based on current selected activity
    val dynamicMaintenanceCalories = remember(tempUser, selectedActivity) {
        FitnessCalculator.calculateMaintenanceCalories(
            gender = tempUser.gender,
            birthYear = tempUser.birthYear,
            heightCm = tempUser.heightCm,
            weightKg = tempUser.weightKg,
            activityLevel = selectedActivity
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Maintenance Calories & BMI Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardDarkBg)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Your Maintenance\nCalories",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "$dynamicMaintenanceCalories",
                    color = TextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Your BMI",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${tempUser.bmi} - ${tempUser.bmiCategory}",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // 2. Select Your Activity Level Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardDarkBg)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select your activity level",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                activityOptions.forEach { option ->
                    val isSelected = option.equals(selectedActivity, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SelectedGreenPillBg else GrayPillBg)
                            .clickable { selectedActivity = option }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = option,
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // 3. Set Your Fitness Goals Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardDarkBg)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Set your fitness goals",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Target Weight
                GoalInputPill(
                    label = "Target Weight (kg)",
                    value = targetWeightText,
                    onValueChange = { targetWeightText = it },
                    keyboardType = KeyboardType.Decimal
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Target Steps
                GoalInputPill(
                    label = "Daily target steps",
                    value = targetStepsText,
                    onValueChange = { targetStepsText = it },
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Target Water Intake
                GoalInputPill(
                    label = "Daily water-intake (L)",
                    value = targetWaterText,
                    onValueChange = { targetWaterText = it },
                    keyboardType = KeyboardType.Decimal
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Bottom Navigation Next Pill
        NextPillButton(
            onClick = {
                val targetWt = targetWeightText.toDoubleOrNull() ?: tempUser.weightKg
                val targetSteps = targetStepsText.toIntOrNull() ?: 10000
                val targetWater = targetWaterText.toDoubleOrNull() ?: 3.5

                viewModel.updateOnboardingGoals(
                    activityLevel = selectedActivity,
                    targetWeight = targetWt,
                    targetSteps = targetSteps,
                    targetWaterL = targetWater
                )
                onNext()
            },
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Composable
private fun GoalInputPill(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GrayPillBg)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                ),
                modifier = Modifier.width(90.dp)
            )
        }
    }
}