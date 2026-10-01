package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.Routes
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient

private val BgGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1B1C1E),
        Color(0xFF131416),
        Color(0xFF2C2E31),
        Color(0xFF111214)
    )
)
private val CardBackground = Color(0xFF000000)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)

@Composable
fun CustomScheduleScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit,
    onNavigateAddExercises: (String) -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")


    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()

            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBack() }
                .padding(vertical = 12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = null,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Back", color = PrimaryGreen, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Custom workout\nschedule",
            color = PrimaryGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Start
        )

        Spacer(modifier = Modifier.height(28.dp))

        days.forEach { dayName ->
            val dayWorkout = user?.customSchedule?.find { it.dayName.equals(dayName, ignoreCase = true) }
            var muscleGroup by remember(dayWorkout?.muscleGroup) {
                mutableStateOf(dayWorkout?.muscleGroup ?: "")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardBackground)
                    .padding(20.dp)
            ) {
                Text(
                    text = dayName,
                    color = PrimaryGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // White Muscle Group Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (muscleGroup.isEmpty()) {
                            Text("Muscle group", color = Color.Gray, fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = muscleGroup,
                            onValueChange = {
                                muscleGroup = it
                                viewModel.updateCustomDayMuscleGroup(dayName, it)
                            },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Green "Add exercises ->" Pill
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(PrimaryGreen)
                            .clickable { onNavigateAddExercises(dayName) }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add exercises",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Add",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}