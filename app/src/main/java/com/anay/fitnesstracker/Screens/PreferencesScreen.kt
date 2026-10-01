package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.components.NextPillButton
import com.anay.fitnesstracker.ui.theme.*
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.components.icons


@Composable
fun PreferencesScreen(
    viewModel: FitnessViewModel,
    onNavigateWorkoutSplit: () -> Unit,
    onNext: () -> Unit
) {
    val tempUser by viewModel.tempOnboarding.collectAsState()
    val frequencies = listOf("3 Days a Week", "4 Days a Week", "5 Days a Week", "6 Days a Week")
    val splits = listOf("Push Pull Legs", "Upper Body, Lower Body", "Full Body", "Upper, Lower + Push Pull Legs")

    Spacer(modifier = Modifier.height(28.dp))


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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Select the frequency of\nyour workouts",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                frequencies.forEach { freq ->
                    val isSelected = freq == tempUser.workoutFrequency
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF3DDC84).copy(alpha = 0.80f) else Color.White)
                            .clickable { viewModel.updateFrequency(freq) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = freq,
                            color = if (isSelected) TextWhite else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Explore the workout splits",
                color = PrimaryGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
                    .padding(20.dp)
            ) {
                splits.forEach { split ->
                    val isSelected = split == tempUser.workoutSplit
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF3DDC84).copy(alpha = 0.80f) else Color.White)
                            .clickable {
                                viewModel.selectExplicitSplit(split)
                                onNavigateWorkoutSplit()
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = split,
                            color = if (isSelected) TextWhite else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF000000))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You can also create a custom split later",
                    color = Color(0xFF3DDC84),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        NextPillButton(
            onClick = onNext,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}