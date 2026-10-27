package com.anay.fitnesstracker.Screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.data.*
import com.anay.fitnesstracker.data.WorkoutScheduleHelper
import com.anay.fitnesstracker.data.WorkoutSet
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

private val CardDarkBg = Color(0x1A3DDC84)
private val CardBorderColor = Color(0xFF3DDC84).copy(alpha = 0.20f)
private val DropdownCardBg = Color(0xFF161719)
private val PillGrayBg = Color(0xFFAEB2B0)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)

enum class WorkoutLogMode {
    WEIGHTS, CARDIO
}

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

    val todayDate = remember { LocalDate.now() }
    val systemZone = remember { ZoneId.systemDefault() }

    val todayLogged = remember(allLogged, todayDate) {
        allLogged.filter { workout ->
            val millis = if (workout.timestamp < 100_000_000_000L) {
                workout.timestamp * 1000L
            } else {
                workout.timestamp
            }
            val workoutDate = Instant.ofEpochMilli(millis).atZone(systemZone).toLocalDate()
            workoutDate == todayDate
        }
    }

    var logMode by remember { mutableStateOf(WorkoutLogMode.WEIGHTS) }
    var selectedExercise by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isKg by remember { mutableStateOf(true) }

    var cardioDurationMins by remember { mutableStateOf("") }

    // Dialog state for editing a saved exercise
    var editingWorkout by remember { mutableStateOf<LoggedExercise?>(null) }

    val allAvailableExercises = remember(user?.customExercises, logMode) {
        if (logMode == WorkoutLogMode.CARDIO) {
            listOf("Treadmill", "Cycling", "Running", "Elliptical", "Rowing", "Jump Rope", "Stair Climber")
        } else {
            viewModel.getAllAvailableExercises()
        }
    }
    val filteredExercises = remember(selectedExercise, allAvailableExercises) {
        if (selectedExercise.isBlank()) allAvailableExercises
        else allAvailableExercises.filter { it.contains(selectedExercise, ignoreCase = true) }
    }

    var currentSets by remember {
        mutableStateOf(
            listOf(
                WorkoutSet(setNumber = 1, weightKg = "", reps = ""),
                WorkoutSet(setNumber = 2, weightKg = "", reps = ""),
                WorkoutSet(setNumber = 3, weightKg = "", reps = "")
            )
        )
    }

    // Horizontal Swipe Gesture Handler
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeModifier = Modifier.pointerInput(logMode) {
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
            onDragEnd = {
                val swipeThreshold = 60f
                if (totalDrag < -swipeThreshold && logMode == WorkoutLogMode.WEIGHTS) {
                    logMode = WorkoutLogMode.CARDIO
                    selectedExercise = ""
                } else if (totalDrag > swipeThreshold && logMode == WorkoutLogMode.CARDIO) {
                    logMode = WorkoutLogMode.WEIGHTS
                    selectedExercise = ""
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()
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

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Log Your Workout",
            color = PrimaryGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Weights / Cardio Segmented Switcher with Sliding Capsule & Swipe Support
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .then(swipeModifier)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CardBorderColor, RoundedCornerShape(20.dp))
                .background(CardDarkBg)
                .padding(6.dp)
        ) {
            val totalWidth = maxWidth
            val halfWidth = totalWidth / 2
            val indicatorOffset by animateDpAsState(
                targetValue = if (logMode == WorkoutLogMode.WEIGHTS) 0.dp else halfWidth,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "segmentedIndicator"
            )

            // Animated Gliding Pill
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(halfWidth)
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryGreen)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Weights Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (logMode != WorkoutLogMode.WEIGHTS) {
                                logMode = WorkoutLogMode.WEIGHTS
                                selectedExercise = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Weights",
                        color = if (logMode == WorkoutLogMode.WEIGHTS) Color.Black else TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Cardio Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (logMode != WorkoutLogMode.CARDIO) {
                                logMode = WorkoutLogMode.CARDIO
                                selectedExercise = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cardio",
                        color = if (logMode == WorkoutLogMode.CARDIO) Color.Black else TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Exercise Form Card (Swipable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(swipeModifier)
                .clip(RoundedCornerShape(24.dp))
                .background(CardDarkBg)
                .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Name of the exercise",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Typable Search Input Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PillGrayBg)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedExercise.isEmpty()) {
                            Text(
                                text = if (logMode == WorkoutLogMode.WEIGHTS) "Select or type exercise" else "e.g. Treadmill, Running",
                                color = Color(0xFF4A4E4C),
                                fontSize = 14.sp
                            )
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
                                fontWeight = FontWeight.SemiBold
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
                                tint = Color(0xFF4A4E4C),
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

            // Docked Dropdown Card
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
                        .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(16.dp))
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

            Spacer(modifier = Modifier.height(18.dp))

            // Animated Mode Switcher (Horizontal Slide on Swipe)
            AnimatedContent(
                targetState = logMode,
                transitionSpec = {
                    if (targetState == WorkoutLogMode.CARDIO) {
                        (slideInHorizontally(tween(250)) { it } + fadeIn()).togetherWith(
                            slideOutHorizontally(tween(250)) { -it } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally(tween(250)) { -it } + fadeIn()).togetherWith(
                            slideOutHorizontally(tween(250)) { it } + fadeOut()
                        )
                    }
                },
                label = "modeSwipeTransition"
            ) { targetMode ->
                if (targetMode == WorkoutLogMode.WEIGHTS) {
                    // ================== WEIGHTS MODE ==================
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0F1E16))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(18.dp))
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                        ) {
                            // Unified Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SET",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = if (isKg) "WEIGHT (KG)" else "WEIGHT (LB)",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.35f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "REPS",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Compact Kg / Lb Pill Toggle Container
                                Box(
                                    modifier = Modifier.width(62.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1E382B))
                                            .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                                            .padding(2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isKg) PrimaryGreen else Color.Transparent)
                                                    .clickable { isKg = true }
                                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Kg",
                                                    color = if (isKg) Color.Black else TextWhite,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (!isKg) PrimaryGreen else Color.Transparent)
                                                    .clickable { isKg = false }
                                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Lb",
                                                    color = if (!isKg) Color.Black else TextWhite,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            currentSets.forEachIndexed { index, setItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${setItem.setNumber}",
                                        color = TextWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Weight input pill
                                    Box(
                                        modifier = Modifier
                                            .weight(1.35f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(PillGrayBg),
                                        contentAlignment = Alignment.Center
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
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Reps input pill
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(PillGrayBg),
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
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Delete Set Icon Container
                                    Box(
                                        modifier = Modifier.width(62.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (currentSets.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    val mutable = currentSets.toMutableList()
                                                    mutable.removeAt(index)
                                                    currentSets = mutable.mapIndexed { i, s -> s.copy(setNumber = i + 1) }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete Set",
                                                    tint = TextWhite.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // + Add Set & Copy Last Set Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(23.dp))
                                    .background(PrimaryGreen)
                                    .clickable {
                                        currentSets = currentSets + WorkoutSet(
                                            setNumber = currentSets.size + 1,
                                            weightKg = "",
                                            reps = ""
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Add Set",
                                        color = Color.Black,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(23.dp))
                                    .background(PrimaryGreen)
                                    .clickable {
                                        val lastSet = currentSets.lastOrNull()
                                        currentSets = currentSets + WorkoutSet(
                                            setNumber = currentSets.size + 1,
                                            weightKg = lastSet?.weightKg ?: "",
                                            reps = lastSet?.reps ?: ""
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Copy Last Set",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // ================== CARDIO MODE ==================
                    Column {
                        Text(
                            text = "Duration (MINS)",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(PillGrayBg)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (cardioDurationMins.isEmpty()) {
                                Text("e.g. 30, 45", color = Color(0xFF4A4E4C), fontSize = 14.sp)
                            }
                            BasicTextField(
                                value = cardioDurationMins,
                                onValueChange = { cardioDurationMins = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.Black,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Save Exercise Pill Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(PrimaryGreen)
                    .clickable {
                        if (selectedExercise.isBlank()) {
                            Toast.makeText(context, "Select or enter an exercise first", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }

                        if (logMode == WorkoutLogMode.WEIGHTS) {
                            val validSets = currentSets.filter { it.weightKg.isNotBlank() && it.reps.isNotBlank() }
                            if (validSets.isEmpty()) {
                                Toast.makeText(context, "Please enter weight and reps", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }

                            val finalSets = validSets.map { set ->
                                if (!isKg) {
                                    val lbVal = set.weightKg.toDoubleOrNull() ?: 0.0
                                    val exactKg = lbVal * 0.45359237
                                    set.copy(weightKg = String.format(Locale.US, "%.4f", exactKg))
                                } else {
                                    set
                                }
                            }

                            viewModel.logExercise(selectedExercise.trim(), currentWorkoutCategory, finalSets)
                            currentSets = listOf(
                                WorkoutSet(1, "", ""),
                                WorkoutSet(2, "", ""),
                                WorkoutSet(3, "", "")
                            )
                        } else {
                            if (cardioDurationMins.isBlank()) {
                                Toast.makeText(context, "Please enter duration in minutes", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }
                            val cardioSet = listOf(WorkoutSet(setNumber = 1, weightKg = "0", reps = cardioDurationMins.trim()))
                            viewModel.logExercise(selectedExercise.trim(), "Cardio", cardioSet)
                            cardioDurationMins = ""
                        }

                        selectedExercise = ""
                        Toast.makeText(context, "Exercise saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save Exercise",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Logged Exercises Header
        Text(
            text = "Logged Exercises",
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (todayLogged.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardDarkBg)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(22.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No exercises logged yet today.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            todayLogged.forEach { exercise ->
                val isCardioExercise = exercise.category.equals("Cardio", ignoreCase = true)

                if (isCardioExercise) {
                    val duration = exercise.sets.firstOrNull()?.reps ?: "0"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardDarkBg)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 22.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${exercise.exerciseName} : $duration Mins",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { editingWorkout = exercise },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Cardio",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = { viewModel.deleteLoggedExercise(exercise.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Cardio",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(CardDarkBg)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(22.dp))
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise.exerciseName,
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Edit Button
                            IconButton(
                                onClick = { editingWorkout = exercise },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Exercise",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Delete Button
                            IconButton(
                                onClick = { viewModel.deleteLoggedExercise(exercise.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Exercise",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        exercise.sets.forEach { setItem ->
                            val displayWeight = if (!isKg) {
                                val kgVal = setItem.weightKg.toDoubleOrNull() ?: 0.0
                                val lbVal = kgVal * 2.20462262
                                if (abs(lbVal - round(lbVal)) < 0.05) {
                                    round(lbVal).toInt().toString()
                                } else {
                                    String.format(Locale.US, "%.1f", lbVal)
                                }
                            } else {
                                val kgVal = setItem.weightKg.toDoubleOrNull() ?: 0.0
                                if (abs(kgVal - round(kgVal)) < 0.05) {
                                    round(kgVal).toInt().toString()
                                } else {
                                    String.format(Locale.US, "%.1f", kgVal)
                                }
                            }
                            val unitLabel = if (isKg) "kg" else "lb"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Set : ${setItem.setNumber}",
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(0.9f)
                                )
                                Text(
                                    text = "Weight : $displayWeight $unitLabel",
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = "Reps : ${setItem.reps}",
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ================== EDIT SAVED EXERCISE DIALOG ==================
    editingWorkout?.let { workoutToEdit ->
        EditExerciseDialog(
            workout = workoutToEdit,
            isKg = isKg,
            onDismiss = { editingWorkout = null },
            onSave = { updatedWorkout ->
                viewModel.deleteLoggedExercise(workoutToEdit.id)
                viewModel.logExercise(
                    exerciseName = updatedWorkout.exerciseName,
                    category = updatedWorkout.category,
                    sets = updatedWorkout.sets
                )
                editingWorkout = null
                Toast.makeText(context, "Exercise updated successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun EditExerciseDialog(
    workout: LoggedExercise,
    isKg: Boolean,
    onDismiss: () -> Unit,
    onSave: (LoggedExercise) -> Unit
) {
    val isCardio = workout.category.equals("Cardio", ignoreCase = true)
    var editName by remember { mutableStateOf(workout.exerciseName) }
    var editDuration by remember { mutableStateOf(workout.sets.firstOrNull()?.reps ?: "") }

    var editableSets by remember {
        mutableStateOf(
            workout.sets.map { set ->
                val weight = if (!isKg) {
                    val kgVal = set.weightKg.toDoubleOrNull() ?: 0.0
                    val lbVal = kgVal * 2.20462262
                    if (abs(lbVal - round(lbVal)) < 0.05) round(lbVal).toInt().toString()
                    else String.format(Locale.US, "%.1f", lbVal)
                } else {
                    val kgVal = set.weightKg.toDoubleOrNull() ?: 0.0
                    if (abs(kgVal - round(kgVal)) < 0.05) round(kgVal).toInt().toString()
                    else String.format(Locale.US, "%.1f", kgVal)
                }
                set.copy(weightKg = weight)
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF161719))
                .border(1.dp, CardBorderColor, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Exercise",
                    color = PrimaryGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Exercise Name Field
            Text("Exercise Name", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PillGrayBg)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isCardio) {
                Text("Duration (Mins)", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PillGrayBg)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = editDuration,
                        onValueChange = { editDuration = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(
                    text = if (isKg) "Sets & Weight (kg)" else "Sets & Weight (lb)",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    editableSets.forEachIndexed { idx, setItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${setItem.setNumber}",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(28.dp),
                                textAlign = TextAlign.Center
                            )

                            // Weight input
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PillGrayBg),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = setItem.weightKg,
                                    onValueChange = { newW ->
                                        editableSets = editableSets.toMutableList().also {
                                            it[idx] = it[idx].copy(weightKg = newW)
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Reps input
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PillGrayBg),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = setItem.reps,
                                    onValueChange = { newR ->
                                        editableSets = editableSets.toMutableList().also {
                                            it[idx] = it[idx].copy(reps = newR)
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Delete Set
                            IconButton(
                                onClick = {
                                    if (editableSets.size > 1) {
                                        val m = editableSets.toMutableList()
                                        m.removeAt(idx)
                                        editableSets = m.mapIndexed { i, s -> s.copy(setNumber = i + 1) }
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // + Add Set Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F1E16))
                        .border(1.dp, CardBorderColor, RoundedCornerShape(18.dp))
                        .clickable {
                            val last = editableSets.lastOrNull()
                            editableSets = editableSets + WorkoutSet(
                                setNumber = editableSets.size + 1,
                                weightKg = last?.weightKg ?: "",
                                reps = last?.reps ?: ""
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Add Set", color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF242528))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(PrimaryGreen)
                        .clickable {
                            if (editName.isBlank()) return@clickable

                            if (isCardio) {
                                val updated = workout.copy(
                                    exerciseName = editName.trim(),
                                    sets = listOf(WorkoutSet(1, "0", editDuration.trim()))
                                )
                                onSave(updated)
                            } else {
                                val validSets = editableSets.filter { it.weightKg.isNotBlank() && it.reps.isNotBlank() }
                                if (validSets.isEmpty()) return@clickable

                                val normalizedSets = validSets.map { set ->
                                    if (!isKg) {
                                        val lbVal = set.weightKg.toDoubleOrNull() ?: 0.0
                                        val exactKg = lbVal * 0.45359237
                                        set.copy(weightKg = String.format(Locale.US, "%.4f", exactKg))
                                    } else {
                                        set
                                    }
                                }

                                val updated = workout.copy(
                                    exerciseName = editName.trim(),
                                    sets = normalizedSets
                                )
                                onSave(updated)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save Changes", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}