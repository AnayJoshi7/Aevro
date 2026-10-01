package com.anay.fitnesstracker.Screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
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
private val DropdownCardBg = Color(0xFF161719)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)

@Composable
fun AddExercisesScreen(
    dayName: String,
    viewModel: FitnessViewModel,
    onBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val dayData = user?.customSchedule?.find { it.dayName.equals(dayName, ignoreCase = true) }
    val savedExercises = dayData?.exercises ?: emptyList()
    val allAvailable = remember(user?.customExercises) { viewModel.getAllAvailableExercises() }

    var inputDrafts by remember { mutableStateOf(listOf("")) }


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
        // Back Header
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

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Add Exercises",
            color = PrimaryGreen,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Dynamic Card Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardBackground)
                .padding(22.dp)
        ) {
            Text(
                text = dayName,
                color = PrimaryGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (dayData?.muscleGroup?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayData.muscleGroup,
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Saved Exercises Rows (Numbered with Delete icon)
            savedExercises.forEachIndexed { index, exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE4E4E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = exercise,
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = { viewModel.deleteExerciseFromDay(dayName, exercise) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (savedExercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2. Dynamic Input Draft Rows with Clean Inline Suggestions
            inputDrafts.forEachIndexed { draftIdx, draftText ->
                val displayNumber = savedExercises.size + draftIdx + 1
                var expandedDropdown by remember { mutableStateOf(false) }

                val suggestions = remember(draftText, allAvailable) {
                    if (draftText.isBlank()) allAvailable
                    else allAvailable.filter { it.contains(draftText, ignoreCase = true) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Number Circle
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE4E4E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$displayNumber", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Typeable Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE4E4E6))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (draftText.isEmpty()) {
                                        Text("Name of the exercise", color = Color.DarkGray, fontSize = 13.sp)
                                    }
                                    BasicTextField(
                                        value = draftText,
                                        onValueChange = { query ->
                                            val list = inputDrafts.toMutableList()
                                            list[draftIdx] = query
                                            inputDrafts = list
                                            expandedDropdown = query.isNotBlank()
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.Black,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (draftText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            val list = inputDrafts.toMutableList()
                                            list[draftIdx] = ""
                                            inputDrafts = list
                                            expandedDropdown = false
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color.DarkGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { expandedDropdown = !expandedDropdown },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Green Tick Checkmark (Saves exercise)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                                .clickable {
                                    if (draftText.isNotBlank()) {
                                        viewModel.addExerciseToDay(dayName, draftText.trim())
                                        val list = inputDrafts.toMutableList()
                                        list.removeAt(draftIdx)
                                        if (list.isEmpty()) list.add("")
                                        inputDrafts = list
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Green Plus Icon (Adds input row)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                                .clickable {
                                    inputDrafts = inputDrafts + ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Row",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Clean Docked Dropdown Card Directly Below Input
                    AnimatedVisibility(
                        visible = expandedDropdown && suggestions.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 6.dp, start = 38.dp, end = 76.dp)
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DropdownCardBg)
                                .border(1.dp, Color(0xFF2C2E33), RoundedCornerShape(14.dp))
                                .verticalScroll(rememberScrollState())
                        ) {
                            suggestions.forEachIndexed { sIdx, suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val list = inputDrafts.toMutableList()
                                            list[draftIdx] = suggestion
                                            inputDrafts = list
                                            expandedDropdown = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = suggestion,
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (sIdx < suggestions.lastIndex) {
                                    HorizontalDivider(
                                        color = Color(0xFF242528),
                                        thickness = 0.8.dp,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}