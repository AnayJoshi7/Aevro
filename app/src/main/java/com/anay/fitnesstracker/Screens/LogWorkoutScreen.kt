package com.anay.fitnesstracker.Screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.data.WorkoutScheduleHelper
import com.anay.fitnesstracker.data.WorkoutSet
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val BgGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1B1C1E),
        Color(0xFF131416),
        Color(0xFF2C2E31),
        Color(0xFF111214)
    )
)
private val CardBackground = Color(0xFF070708)
private val DropdownCardBg = Color(0xFF161719)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)

@Composable
fun LogWorkoutScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val allLogged = user?.loggedWorkouts ?: emptyList()

    val todayWorkout = remember(user?.workoutFrequency, user?.workoutSplit, user?.customSchedule) {
        WorkoutScheduleHelper.getTodayWorkout(user)
    }
    val currentWorkoutCategory = todayWorkout.title

    // Helper to accurately compare calendar dates (handles both seconds and milliseconds timestamps)
    val todayDate = remember { LocalDate.now() }
    val systemZone = remember { ZoneId.systemDefault() }

    val todayLogged = remember(allLogged, todayDate) {
        allLogged.filter { workout ->
            val millis = if (workout.timestamp < 100_000_000_000L) {
                workout.timestamp * 1000L // Convert seconds to milliseconds if stored in seconds
            } else {
                workout.timestamp
            }
            val workoutDate = Instant.ofEpochMilli(millis).atZone(systemZone).toLocalDate()
            workoutDate == todayDate
        }
    }

    val previousWorkouts = remember(allLogged, todayDate, currentWorkoutCategory) {
        allLogged.filter { workout ->
            val millis = if (workout.timestamp < 100_000_000_000L) {
                workout.timestamp * 1000L
            } else {
                workout.timestamp
            }
            val workoutDate = Instant.ofEpochMilli(millis).atZone(systemZone).toLocalDate()
            workoutDate < todayDate && (workout.category == currentWorkoutCategory || currentWorkoutCategory == "Rest Day")
        }
    }

    var selectedExercise by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isKg by remember { mutableStateOf(true) } // kg vs lb toggle

    val allAvailableExercises = remember(user?.customExercises) {
        viewModel.getAllAvailableExercises()
    }
    val filteredExercises = remember(selectedExercise, allAvailableExercises) {
        if (selectedExercise.isBlank()) allAvailableExercises
        else allAvailableExercises.filter { it.contains(selectedExercise, ignoreCase = true) }
    }

    var currentSets by remember {
        mutableStateOf(
            listOf(
                WorkoutSet(setNumber = 1, weightKg = "", reps = ""),
                WorkoutSet(setNumber = 2, weightKg = "", reps = "")
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back Button Header
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
            text = "Log Your Workout",
            color = PrimaryGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Add Exercise", color = PrimaryGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        // Main Exercise Form Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardBackground)
                .padding(20.dp)
        ) {
            Text(text = "Name of the exercise", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            // Typable Search Input Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedExercise.isEmpty()) {
                            Text("Select or type exercise", color = Color.Gray, fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = selectedExercise,
                            onValueChange = { query ->
                                selectedExercise = query
                                dropdownExpanded = query.isNotBlank()
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (selectedExercise.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                selectedExercise = ""
                                dropdownExpanded = false
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { dropdownExpanded = !dropdownExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand",
                            tint = Color.Black
                        )
                    }
                }
            }

            // Clean Docked Dropdown Card
            AnimatedVisibility(
                visible = dropdownExpanded && filteredExercises.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .heightIn(max = 190.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DropdownCardBg)
                        .border(1.dp, Color(0xFF2C2E33), RoundedCornerShape(16.dp))
                        .verticalScroll(rememberScrollState())
                ) {
                    filteredExercises.forEachIndexed { index, exercise ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedExercise = exercise
                                    dropdownExpanded = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise,
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (index < filteredExercises.lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFF242528),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Set Rows with centered alignments and Delete Set option
            currentSets.forEachIndexed { index, setItem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Set Number Pill
                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (index == 0) {
                            Text("Set", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${setItem.setNumber}",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Weight Pill (with kg / lb unit switcher)
                    Column(
                        modifier = Modifier.weight(1.3f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (index == 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Weight", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isKg) "(kg)" else "(lb)",
                                    color = PrimaryGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { isKg = !isKg }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BasicTextField(
                                    value = setItem.weightKg,
                                    onValueChange = { newVal ->
                                        currentSets = currentSets.toMutableList().also {
                                            it[index] = it[index].copy(weightKg = newVal)
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.Black,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            innerTextField()
                                        }
                                    }
                                )

                                Text(
                                    text = if (isKg) "kg" else "lb",
                                    color = Color.DarkGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .padding(start = 2.dp)
                                        .clickable { isKg = !isKg }
                                )
                            }
                        }
                    }

                    // Reps Pill
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (index == 0) {
                            Text("Reps", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = setItem.reps,
                                onValueChange = { newVal ->
                                    currentSets = currentSets.toMutableList().also {
                                        it[index] = it[index].copy(reps = newVal)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }

                    // Delete Set Button
                    if (currentSets.size > 1) {
                        IconButton(
                            onClick = {
                                val mutable = currentSets.toMutableList()
                                mutable.removeAt(index)
                                currentSets = mutable.mapIndexed { i, s -> s.copy(setNumber = i + 1) }
                            },
                            modifier = Modifier
                                .padding(top = if (index == 0) 24.dp else 0.dp)
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Set",
                                tint = Color(0xFFE53935)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(PrimaryGreen)
                        .clickable {
                            currentSets = currentSets + WorkoutSet(
                                setNumber = currentSets.size + 1,
                                weightKg = "",
                                reps = ""
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Set", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Set",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen)
                        .clickable {
                            if (selectedExercise.isBlank()) {
                                Toast.makeText(context, "Select or enter an exercise first", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }
                            val validSets = currentSets.filter { it.weightKg.isNotBlank() && it.reps.isNotBlank() }
                            if (validSets.isEmpty()) {
                                Toast.makeText(context, "Please enter weight and reps", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }

                            // If entered in lb, store clean kg value so internal data remains unified
                            val finalSets = validSets.map { set ->
                                if (!isKg) {
                                    val lbVal = set.weightKg.toDoubleOrNull() ?: 0.0
                                    val convertedKg = String.format(java.util.Locale.US, "%.1f", lbVal * 0.45359237)
                                    set.copy(weightKg = convertedKg)
                                } else {
                                    set
                                }
                            }

                            viewModel.logExercise(selectedExercise.trim(), currentWorkoutCategory, finalSets)
                            selectedExercise = ""
                            currentSets = listOf(
                                WorkoutSet(1, "", ""),
                                WorkoutSet(2, "", "")
                            )
                            Toast.makeText(context, "Workout logged successfully!", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Submit", tint = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Logged Exercises Section
        Text(text = "Today's Logged Exercises", color = PrimaryGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        if (todayLogged.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardBackground)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No exercises logged yet today.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            todayLogged.forEach { exercise ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardBackground)
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = exercise.exerciseName, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { viewModel.deleteLoggedExercise(exercise.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Exercise", tint = Color(0xFFE53935))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    exercise.sets.forEach { setItem ->
                        val displayWeight = if (!isKg) {
                            val kgVal = setItem.weightKg.toDoubleOrNull() ?: 0.0
                            String.format(java.util.Locale.US, "%.1f", kgVal * 2.20462)
                        } else {
                            setItem.weightKg
                        }
                        val unitLabel = if (isKg) "kg" else "lb"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Set : ${setItem.setNumber}", color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(0.9f))
                            Text("Weight : $displayWeight $unitLabel", color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1.4f))
                            Text("Reps : ${setItem.reps}", color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1.0f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Exercises of Previous Session Section
        if (previousWorkouts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Exercises of previous $currentWorkoutCategory",
                color = PrimaryGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            previousWorkouts.forEach { exercise ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardBackground)
                        .padding(18.dp)
                ) {
                    Text(text = exercise.exerciseName, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    exercise.sets.forEach { setItem ->
                        val displayWeight = if (!isKg) {
                            val kgVal = setItem.weightKg.toDoubleOrNull() ?: 0.0
                            String.format(java.util.Locale.US, "%.1f", kgVal * 2.20462)
                        } else {
                            setItem.weightKg
                        }
                        val unitLabel = if (isKg) "kg" else "lb"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Set : ${setItem.setNumber}", color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(0.9f))
                            Text("Weight : $displayWeight $unitLabel", color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1.4f))
                            Text("Reps : ${setItem.reps}", color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1.0f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}