package com.anay.fitnesstracker.Screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.data.UserProfile
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.OneplusSlate
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val TitleGreen = Color(0xFF0673cb)
private val ButtonFillGreen = Color(0x1a0673cb)
private val ButtonBorderGreen = Color(0xFFFFFFFF).copy(alpha=0.18f)
private val ButtonCircleBlack = Color(0xFF07110B)
private val ButtonArrowGreen = Color(0x800673cb)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMutedGray = Color(0xFF8E9094)

private enum class OnboardingStep {
    INTRO,
    NAME,
    CONTACT_NO,
    AGE,
    GENDER,
    HEIGHT,
    WEIGHT,
    ACTIVITY_LEVEL,
    FITNESS_GOAL,
    TARGET_WEIGHT,
    WORKOUT_FREQUENCY
}

@Composable
fun OnboardingScreen(
    viewModel: FitnessViewModel,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val tempUser by viewModel.tempOnboarding.collectAsState()

    var currentStep by remember { mutableStateOf(OnboardingStep.INTRO) }
    var isNavigatingForward by remember { mutableStateOf(true) }

    // Step States
    var name by remember { mutableStateOf(tempUser.name) }
    var contactNo by remember { mutableStateOf("") }
    var age by remember {
        val initAge = if (tempUser.birthYear in 1920..2020) 2026 - tempUser.birthYear else 20
        mutableIntStateOf(initAge.coerceIn(10, 90))
    }
    var gender by remember {
        mutableStateOf(tempUser.gender.ifBlank { "Male" })
    }

    // Height state (stored in cm, with ft / cm scrollable unit selector + decimal/inch support)
    var heightUnit by remember { mutableStateOf("cm") }
    var heightCm by remember {
        mutableDoubleStateOf(if (tempUser.heightCm > 50.0) tempUser.heightCm else 175.0)
    }

    // Weight state (stored in kg, with lbs / kg scrollable unit selector + decimal support)
    var weightUnit by remember { mutableStateOf("kg") }
    var weightKg by remember {
        mutableDoubleStateOf(if (tempUser.weightKg > 20.0) tempUser.weightKg else 75.0)
    }

    var activityLevel by remember {
        mutableStateOf("Highly Active")
    }
    var fitnessGoal by remember {
        mutableStateOf(tempUser.fitnessGoal.ifBlank { "Maintain Weight" })
    }

    // Target weight state (stored in kg, with lbs / kg scrollable unit selector + decimal support)
    var targetWeightUnit by remember { mutableStateOf("kg") }
    var targetWeightKg by remember {
        mutableDoubleStateOf(if (tempUser.weightKg > 20.0) tempUser.weightKg else 75.0)
    }

    var workoutFrequency by remember {
        mutableStateOf(tempUser.workoutFrequency.ifBlank { "5 Days a week" })
    }

    // Builds the active step sequence (skips TARGET_WEIGHT when "Maintain Weight" is selected)
    fun getActiveSteps(): List<OnboardingStep> {
        val steps = mutableListOf(
            OnboardingStep.INTRO,
            OnboardingStep.NAME,
            OnboardingStep.CONTACT_NO,
            OnboardingStep.AGE,
            OnboardingStep.GENDER,
            OnboardingStep.HEIGHT,
            OnboardingStep.WEIGHT,
            OnboardingStep.ACTIVITY_LEVEL,
            OnboardingStep.FITNESS_GOAL
        )
        if (!fitnessGoal.trim().equals("Maintain Weight", ignoreCase = true)) {
            steps.add(OnboardingStep.TARGET_WEIGHT)
        }
        steps.add(OnboardingStep.WORKOUT_FREQUENCY)
        return steps
    }

    fun goNext() {
        focusManager.clearFocus()
        val steps = getActiveSteps()
        val currentIndex = steps.indexOf(currentStep)
        if (currentIndex < steps.lastIndex) {
            isNavigatingForward = true
            val nextStep = steps[currentIndex + 1]

            if (nextStep == OnboardingStep.TARGET_WEIGHT && targetWeightKg <= 20.0) {
                targetWeightKg = weightKg
            }
            currentStep = nextStep
        } else {
            val finalTargetWeight = if (fitnessGoal.trim().equals("Maintain Weight", ignoreCase = true)) {
                weightKg
            } else {
                targetWeightKg
            }

            // Automatically determine workout split from selected workout frequency
            val derivedWorkoutSplit = when (workoutFrequency.take(1)) {
                "3" -> "Full Body"
                "4" -> "Upper Lower"
                else -> "Push Pull Legs"
            }

            finalizeOnboardingData(
                context = context,
                viewModel = viewModel,
                name = name.trim(),
                contactNo = contactNo.trim(),
                age = age,
                gender = gender,
                heightCm = heightCm,
                weightKg = weightKg,
                activityLevel = activityLevel,
                fitnessGoal = fitnessGoal,
                targetWeightKg = finalTargetWeight,
                workoutFrequency = workoutFrequency,
                workoutSplit = derivedWorkoutSplit
            )
            onNext()
        }
    }

    fun goPrevious() {
        focusManager.clearFocus()
        val steps = getActiveSteps()
        val currentIndex = steps.indexOf(currentStep)
        if (currentIndex > 0) {
            isNavigatingForward = false
            currentStep = steps[currentIndex - 1]
        }
    }

    // Intercepts back press on all steps after INTRO to go to the previous question;
    // on INTRO, delegates to NavHost to pop back to the previous screen.
    BackHandler(enabled = currentStep != OnboardingStep.INTRO) {
        goPrevious()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (isNavigatingForward) {
                    (slideInHorizontally(tween(320)) { width -> width / 2 } + fadeIn(tween(320)))
                        .togetherWith(
                            slideOutHorizontally(tween(320)) { width -> -width / 2 } + fadeOut(tween(220))
                        )
                } else {
                    (slideInHorizontally(tween(320)) { width -> -width / 2 } + fadeIn(tween(320)))
                        .togetherWith(
                            slideOutHorizontally(tween(320)) { width -> width / 2 } + fadeOut(tween(220))
                        )
                }
            },
            label = "onboardingStepTransition",
            modifier = Modifier.fillMaxSize()
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (step) {
                    // 1. INTRO
                    OnboardingStep.INTRO -> {
                        OnboardingTitle(text = "Ready to start\nyour fitness\njourney?")
                        Spacer(modifier = Modifier.height(64.dp))
                        OnboardingActionPillButton(
                            text = "Start",
                            onClick = { goNext() }
                        )
                    }

                    // 2. NAME
                    OnboardingStep.NAME -> {
                        val focusRequester = remember { FocusRequester() }

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        OnboardingTitle(text = "What’s Your\nName?")
                        Spacer(modifier = Modifier.height(48.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = name,
                                onValueChange = { newValue: String -> name = newValue },
                                singleLine = true,
                                cursorBrush = SolidColor(TextWhite),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (name.isNotBlank()) {
                                            goNext()
                                        }
                                    }
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextWhite,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        OnboardingActionPillButton(
                            text = "Next",
                            onClick = {
                                if (name.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "Please enter your name",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    goNext()
                                }
                            }
                        )
                    }

                    // 3. CONTACT NO.
                    OnboardingStep.CONTACT_NO -> {
                        val focusRequester = remember { FocusRequester() }

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        OnboardingTitle(text = "What’s Your\nContact No.?")
                        Spacer(modifier = Modifier.height(48.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (contactNo.isEmpty()) {
                                Text(
                                    text = "10-digit mobile number",
                                    color = TextMutedGray.copy(alpha = 0.45f),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            BasicTextField(
                                value = contactNo,
                                onValueChange = { input: String ->
                                    contactNo = input.filter { ch -> ch.isDigit() }.take(10)
                                },
                                singleLine = true,
                                cursorBrush = SolidColor(TextWhite),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (contactNo.length >= 4) {
                                            goNext()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Enter at least 4 digits",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextWhite,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        OnboardingActionPillButton(
                            text = "Next",
                            onClick = {
                                if (contactNo.length < 4) {
                                    Toast.makeText(
                                        context,
                                        "Please enter a valid contact number",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    goNext()
                                }
                            }
                        )
                    }

                    // 4. AGE
                    OnboardingStep.AGE -> {
                        val ageRange = remember { (10..90).toList() }
                        val ageLabels = remember { ageRange.map { "$it Years" } }
                        val selectedIdx = (age - 10).coerceIn(0, ageRange.lastIndex)

                        OnboardingTitle(text = "What’s Your\nAge?")
                        Spacer(modifier = Modifier.height(36.dp))

                        SingleWheelPickerWithTyping(
                            items = ageLabels,
                            selectedIndex = selectedIdx,
                            onSelectedIndexChange = { idx -> age = ageRange[idx] },
                            keyboardType = KeyboardType.Number,
                            initialTypedValue = { age.toString() },
                            onTypedValueConfirmed = { typed ->
                                val parsed = typed.filter { it.isDigit() }.toIntOrNull()
                                if (parsed != null) {
                                    age = parsed.coerceIn(10, 90)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(42.dp))

                        OnboardingActionPillButton(
                            text = "Next",
                            onClick = { goNext() }
                        )
                    }

                    // 5. GENDER
                    OnboardingStep.GENDER -> {
                        val genders = remember { listOf("Female", "Male", "Other") }
                        val selectedIdx = genders.indexOfFirst {
                            it.equals(gender, ignoreCase = true)
                        }.let { if (it == -1) 1 else it }

                        OnboardingTitle(text = "What’s Your\nGender?")
                        Spacer(modifier = Modifier.height(36.dp))

                        SingleWheelPickerWithTyping(
                            items = genders,
                            selectedIndex = selectedIdx,
                            onSelectedIndexChange = { idx -> gender = genders[idx] },
                            keyboardType = KeyboardType.Text,
                            initialTypedValue = { gender },
                            onTypedValueConfirmed = { typed ->
                                val match = genders.firstOrNull {
                                    it.startsWith(typed.trim(), ignoreCase = true)
                                }
                                if (match != null) gender = match
                            }
                        )

                        Spacer(modifier = Modifier.height(42.dp))

                        OnboardingActionPillButton(
                            text = "Next",
                            onClick = { goNext() }
                        )
                    }

                    // 6. HEIGHT (Scrollable Whole + Decimal/Inches + ft/cm Unit Wheel)
                    OnboardingStep.HEIGHT -> {
                        val units = remember { listOf("ft", "cm") }
                        val unitIdx = if (heightUnit == "ft") 0 else 1

                        val cmWholeRange = remember { (120..230).toList() }
                        val cmDecimalLabels = remember { (0..9).map { ".$it" } }

                        val ftWholeRange = remember { (4..7).toList() }
                        val ftInchLabels = remember { (0..11).map { "${it}\"" } }

                        val wholeItems = if (heightUnit == "cm") {
                            cmWholeRange.map { it.toString() }
                        } else {
                            ftWholeRange.map { "${it}'" }
                        }

                        val subItems = if (heightUnit == "cm") cmDecimalLabels else ftInchLabels

                        val (selectedWholeIdx, selectedSubIdx) = if (heightUnit == "cm") {
                            val totalTenths = (heightCm * 10.0).roundToInt().coerceIn(1200, 2309)
                            val wIdx = ((totalTenths / 10) - 120).coerceIn(0, cmWholeRange.lastIndex)
                            val dIdx = (totalTenths % 10).coerceIn(0, 9)
                            wIdx to dIdx
                        } else {
                            val totalInches = (heightCm / 2.54).roundToInt().coerceIn(48, 95)
                            val ftIdx = ((totalInches / 12) - 4).coerceIn(0, ftWholeRange.lastIndex)
                            val inIdx = (totalInches % 12).coerceIn(0, 11)
                            ftIdx to inIdx
                        }

                        OnboardingTitle(text = "What’s Your\nHeight?")
                        Spacer(modifier = Modifier.height(36.dp))

                        DecimalUnitWheelPickerWithTyping(
                            wholeItems = wholeItems,
                            selectedWholeIndex = selectedWholeIdx,
                            onWholeIndexChange = { newWholeIdx ->
                                heightCm = if (heightUnit == "cm") {
                                    cmWholeRange[newWholeIdx] + (selectedSubIdx / 10.0)
                                } else {
                                    val ft = ftWholeRange[newWholeIdx]
                                    (ft * 12 + selectedSubIdx) * 2.54
                                }
                            },
                            decimalItems = subItems,
                            selectedDecimalIndex = selectedSubIdx,
                            onDecimalIndexChange = { newSubIdx ->
                                heightCm = if (heightUnit == "cm") {
                                    cmWholeRange[selectedWholeIdx] + (newSubIdx / 10.0)
                                } else {
                                    val ft = ftWholeRange[selectedWholeIdx]
                                    (ft * 12 + newSubIdx) * 2.54
                                }
                            },
                            unitItems = units,
                            selectedUnitIndex = unitIdx,
                            onUnitIndexChange = { newUnitIdx ->
                                heightUnit = units[newUnitIdx]
                            },
                            initialTypedValue = {
                                if (heightUnit == "cm") {
                                    String.format(Locale.US, "%.1f", heightCm)
                                } else {
                                    val ft = ftWholeRange[selectedWholeIdx]
                                    "$ft.$selectedSubIdx"
                                }
                            },
                            onTypedValueConfirmed = { typed ->
                                val clean = typed.trim()
                                if (heightUnit == "cm") {
                                    val v = clean.toDoubleOrNull()
                                    if (v != null) heightCm = v.coerceIn(120.0, 230.9)
                                } else {
                                    val parts = clean.split("'", ".", " ", "\"").filter { it.isNotBlank() }
                                    val ft = parts.getOrNull(0)?.toIntOrNull()
                                    val inch = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    if (ft != null) {
                                        val totalInches = (ft * 12 + inch.coerceIn(0, 11)).coerceIn(48, 95)
                                        heightCm = totalInches * 2.54
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(42.dp))

                        OnboardingActionPillButton(
                            text = "Next",
                            onClick = { goNext() }
                        )
                    }

                    // 7. WEIGHT (Scrollable Whole + .0-.9 Decimal + lbs/kg Unit Wheel)
                    OnboardingStep.WEIGHT -> {
                        val units = remember { listOf("lbs", "kg") }
                        val unitIdx = if (weightUnit == "lbs") 0 else 1

                        val kgRange = remember { (30..200).toList() }
                        val lbsRange = remember { (66..440).toList() }
                        val decimalLabels = remember { (0..9).map { ".$it" } }

                        val wholeItems = if (weightUnit == "kg") {
                            kgRange.map { it.toString() }
                        } else {
                            lbsRange.map { it.toString() }
                        }

                        val (selectedWholeIdx, selectedDecIdx) = if (weightUnit == "kg") {
                            val tenths = (weightKg * 10.0).roundToInt().coerceIn(300, 2009)
                            val wIdx = ((tenths / 10) - 30).coerceIn(0, kgRange.lastIndex)
                            val dIdx = (tenths % 10).coerceIn(0, 9)
                            wIdx to dIdx
                        } else {
                            val lbsVal = weightKg * 2.20462262
                            val tenths = (lbsVal * 10.0).roundToInt().coerceIn(660, 4409)
                            val wIdx = ((tenths / 10) - 66).coerceIn(0, lbsRange.lastIndex)
                            val dIdx = (tenths % 10).coerceIn(0, 9)
                            wIdx to dIdx
                        }

                        OnboardingTitle(text = "What’s Your\nWeight?")
                        Spacer(modifier = Modifier.height(36.dp))

                        DecimalUnitWheelPickerWithTyping(
                            wholeItems = wholeItems,
                            selectedWholeIndex = selectedWholeIdx,
                            onWholeIndexChange = { newWholeIdx ->
                                weightKg = if (weightUnit == "kg") {
                                    kgRange[newWholeIdx] + (selectedDecIdx / 10.0)
                                } else {
                                    (lbsRange[newWholeIdx] + (selectedDecIdx / 10.0)) / 2.20462262
                                }
                            },
                            decimalItems = decimalLabels,
                            selectedDecimalIndex = selectedDecIdx,
                            onDecimalIndexChange = { newDecIdx ->
                                weightKg = if (weightUnit == "kg") {
                                    kgRange[selectedWholeIdx] + (newDecIdx / 10.0)
                                } else {
                                    (lbsRange[selectedWholeIdx] + (newDecIdx / 10.0)) / 2.20462262
                                }
                            },
                            unitItems = units,
                            selectedUnitIndex = unitIdx,
                            onUnitIndexChange = { newUnitIdx ->
                                weightUnit = units[newUnitIdx]
                            },
                            initialTypedValue = {
                                val displayVal = if (weightUnit == "kg") weightKg else weightKg * 2.20462262
                                String.format(Locale.US, "%.1f", displayVal)
                            },
                            onTypedValueConfirmed = { typed ->
                                val parsed = typed.toDoubleOrNull()
                                if (parsed != null) {
                                    weightKg = if (weightUnit == "kg") {
                                        parsed.coerceIn(30.0, 200.9)
                                    } else {
                                        parsed.coerceIn(66.0, 440.9) / 2.20462262
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(42.dp))

                        OnboardingActionPillButton(
                            text = "Next",
                            onClick = { goNext() }
                        )
                    }

                    // 8. ACTIVITY LEVEL
                    OnboardingStep.ACTIVITY_LEVEL -> {
                        val levels = remember {
                            listOf(
                                "Lightly Active",
                                "Highly Active",
                                "Moderately Active",
                                "Sedentary"
                            )
                        }
                        val selectedIdx = levels.indexOfFirst {
                            it.equals(activityLevel, ignoreCase = true)
                        }.let { if (it == -1) 1 else it }

                        OnboardingTitle(text = "What’s Your\nActivity Level ?")
                        Spacer(modifier = Modifier.height(36.dp))

                        SingleWheelPickerWithTyping(
                            items = levels,
                            selectedIndex = selectedIdx,
                            onSelectedIndexChange = { idx -> activityLevel = levels[idx] },
                            keyboardType = KeyboardType.Text,
                            initialTypedValue = { activityLevel },
                            onTypedValueConfirmed = { typed ->
                                val match = levels.firstOrNull {
                                    it.contains(typed.trim(), ignoreCase = true)
                                }
                                if (match != null) activityLevel = match
                            }
                        )

                        Spacer(modifier = Modifier.height(42.dp))

                        OnboardingActionPillButton(
                            text = "Next",
                            onClick = { goNext() }
                        )
                    }

                    // 9. FITNESS GOAL
                    OnboardingStep.FITNESS_GOAL -> {
                        val goals = remember {
                            listOf(
                                "Gain Weight",
                                "Maintain Weight",
                                "Lose Weight",
                            )
                        }
                        val selectedIdx = goals.indexOfFirst {
                            it.equals(fitnessGoal, ignoreCase = true)
                        }.let { if (it == -1) 1 else it }

                        OnboardingTitle(text = "What’s Your\nFitness Goal?")
                        Spacer(modifier = Modifier.height(36.dp))

                        SingleWheelPickerWithTyping(
                            items = goals,
                            selectedIndex = selectedIdx,
                            onSelectedIndexChange = { idx -> fitnessGoal = goals[idx] },
                            keyboardType = KeyboardType.Text,
                            initialTypedValue = { fitnessGoal },
                            onTypedValueConfirmed = { typed ->
                                val match = goals.firstOrNull {
                                    it.contains(typed.trim(), ignoreCase = true)
                                }
                                if (match != null) fitnessGoal = match
                            }
                        )

                        Spacer(modifier = Modifier.height(42.dp))

                        OnboardingActionPillButton(
                            text = "Next",
                            onClick = { goNext() }
                        )
                    }

                    // 10. TARGET WEIGHT (Scrollable Whole + .0-.9 Decimal + lbs/kg Unit Wheel)
                    OnboardingStep.TARGET_WEIGHT -> {
                        val units = remember { listOf("lbs", "kg") }
                        val unitIdx = if (targetWeightUnit == "lbs") 0 else 1

                        val kgRange = remember { (30..200).toList() }
                        val lbsRange = remember { (66..440).toList() }
                        val decimalLabels = remember { (0..9).map { ".$it" } }

                        val wholeItems = if (targetWeightUnit == "kg") {
                            kgRange.map { it.toString() }
                        } else {
                            lbsRange.map { it.toString() }
                        }

                        val (selectedWholeIdx, selectedDecIdx) = if (targetWeightUnit == "kg") {
                            val tenths = (targetWeightKg * 10.0).roundToInt().coerceIn(300, 2009)
                            val wIdx = ((tenths / 10) - 30).coerceIn(0, kgRange.lastIndex)
                            val dIdx = (tenths % 10).coerceIn(0, 9)
                            wIdx to dIdx
                        } else {
                            val lbsVal = targetWeightKg * 2.20462262
                            val tenths = (lbsVal * 10.0).roundToInt().coerceIn(660, 4409)
                            val wIdx = ((tenths / 10) - 66).coerceIn(0, lbsRange.lastIndex)
                            val dIdx = (tenths % 10).coerceIn(0, 9)
                            wIdx to dIdx
                        }

                        OnboardingTitle(text = "What’s your\nTarget Weight?")
                        Spacer(modifier = Modifier.height(36.dp))

                        DecimalUnitWheelPickerWithTyping(
                            wholeItems = wholeItems,
                            selectedWholeIndex = selectedWholeIdx,
                            onWholeIndexChange = { newWholeIdx ->
                                targetWeightKg = if (targetWeightUnit == "kg") {
                                    kgRange[newWholeIdx] + (selectedDecIdx / 10.0)
                                } else {
                                    (lbsRange[newWholeIdx] + (selectedDecIdx / 10.0)) / 2.20462262
                                }
                            },
                            decimalItems = decimalLabels,
                            selectedDecimalIndex = selectedDecIdx,
                            onDecimalIndexChange = { newDecIdx ->
                                targetWeightKg = if (targetWeightUnit == "kg") {
                                    kgRange[selectedWholeIdx] + (newDecIdx / 10.0)
                                } else {
                                    (lbsRange[selectedWholeIdx] + (newDecIdx / 10.0)) / 2.20462262
                                }
                            },
                            unitItems = units,
                            selectedUnitIndex = unitIdx,
                            onUnitIndexChange = { newUnitIdx ->
                                targetWeightUnit = units[newUnitIdx]
                            },
                            initialTypedValue = {
                                val displayVal = if (targetWeightUnit == "kg") targetWeightKg else targetWeightKg * 2.20462262
                                String.format(Locale.US, "%.1f", displayVal)
                            },
                            onTypedValueConfirmed = { typed ->
                                val parsed = typed.toDoubleOrNull()
                                if (parsed != null) {
                                    targetWeightKg = if (targetWeightUnit == "kg") {
                                        parsed.coerceIn(30.0, 200.9)
                                    } else {
                                        parsed.coerceIn(66.0, 440.9) / 2.20462262
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(42.dp))

                        OnboardingActionPillButton(
                            text = "Next",
                            onClick = { goNext() }
                        )
                    }

                    // 11. WORKOUT FREQUENCY (Final step -> Automatically sets Workout Split)
                    OnboardingStep.WORKOUT_FREQUENCY -> {
                        val frequencies = remember {
                            listOf(
                                "4 Days a Week",
                                "5 Days a week",
                                "3 Days a Week",
                                "6 Days a Week"
                            )
                        }
                        val selectedIdx = frequencies.indexOfFirst {
                            it.equals(workoutFrequency, ignoreCase = true)
                        }.let { if (it == -1) 1 else it }

                        OnboardingTitle(text = "How Frequently\ndo you want to\nworkout?")
                        Spacer(modifier = Modifier.height(36.dp))

                        SingleWheelPickerWithTyping(
                            items = frequencies,
                            selectedIndex = selectedIdx,
                            onSelectedIndexChange = { idx -> workoutFrequency = frequencies[idx] },
                            keyboardType = KeyboardType.Number,
                            initialTypedValue = { workoutFrequency.take(1) },
                            onTypedValueConfirmed = { typed ->
                                val digit = typed.filter { it.isDigit() }.toIntOrNull()
                                if (digit != null) {
                                    val matched =
                                        frequencies.firstOrNull { it.startsWith("$digit") }
                                    if (matched != null) workoutFrequency = matched
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(42.dp))

                        OnboardingActionPillButton(
                            text = "Next",
                            onClick = { goNext() }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// REUSABLE UI COMPONENTS
// =========================================================================

@Composable
private fun OnboardingTitle(text: String) {
    Text(
        text = text,
        color = TitleGreen,
        fontSize = 34.sp,
        fontFamily = OneplusSlate,
        fontWeight = FontWeight.Bold,
        lineHeight = 42.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OnboardingActionPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(220.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(ButtonFillGreen)
            .border(1.dp, ButtonBorderGreen, RoundedCornerShape(27.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(34.dp)
                .clip(CircleShape)
                .background(ButtonCircleBlack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = text,
                tint = ButtonArrowGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 3-row visible wheel picker with tap-on-center to type directly.
 */
@Composable
private fun SingleWheelPickerWithTyping(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    keyboardType: KeyboardType,
    initialTypedValue: () -> String,
    onTypedValueConfirmed: (String) -> Unit
) {
    var isTyping by remember { mutableStateOf(false) }
    var typedText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isTyping) {
        focusManager.clearFocus()
        isTyping = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isTyping) {
            LaunchedEffect(Unit) {
                typedText = initialTypedValue()
                focusRequester.requestFocus()
            }

            BasicTextField(
                value = typedText,
                onValueChange = { newValue: String -> typedText = newValue },
                singleLine = true,
                cursorBrush = SolidColor(TitleGreen),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onTypedValueConfirmed(typedText)
                        focusManager.clearFocus()
                        isTyping = false
                    }
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .focusRequester(focusRequester)
            )
        } else {
            WheelColumn(
                items = items,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = onSelectedIndexChange,
                onCenterItemTap = { isTyping = true },
                itemHeight = 44.dp,
                selectedFontSize = 26f,
                unselectedFontSize = 15.5f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 3-column scrollable wheel picker for Whole Value + Decimal/Sub-unit (.0-.9 or inches) + Unit (lbs/kg or ft/cm)
 * with tap-on-center to type decimal values directly.
 */
@Composable
private fun DecimalUnitWheelPickerWithTyping(
    wholeItems: List<String>,
    selectedWholeIndex: Int,
    onWholeIndexChange: (Int) -> Unit,
    decimalItems: List<String>,
    selectedDecimalIndex: Int,
    onDecimalIndexChange: (Int) -> Unit,
    unitItems: List<String>,
    selectedUnitIndex: Int,
    onUnitIndexChange: (Int) -> Unit,
    initialTypedValue: () -> String,
    onTypedValueConfirmed: (String) -> Unit
) {
    var isTyping by remember { mutableStateOf(false) }
    var typedText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isTyping) {
        focusManager.clearFocus()
        isTyping = false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTyping) {
            LaunchedEffect(Unit) {
                typedText = initialTypedValue()
                focusRequester.requestFocus()
            }

            Box(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = typedText,
                    onValueChange = { newValue: String -> typedText = newValue },
                    singleLine = true,
                    cursorBrush = SolidColor(TitleGreen),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onTypedValueConfirmed(typedText)
                            focusManager.clearFocus()
                            isTyping = false
                        }
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = TextWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        } else {
            // Left Wheel: Whole Number
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                WheelColumn(
                    items = wholeItems,
                    selectedIndex = selectedWholeIndex,
                    onSelectedIndexChange = onWholeIndexChange,
                    onCenterItemTap = { isTyping = true },
                    itemHeight = 40.dp,
                    selectedFontSize = 27f,
                    unselectedFontSize = 15.5f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Middle Wheel: Decimal (.0 .. .9) or Inches (0" .. 11")
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                WheelColumn(
                    items = decimalItems,
                    selectedIndex = selectedDecimalIndex,
                    onSelectedIndexChange = onDecimalIndexChange,
                    onCenterItemTap = { isTyping = true },
                    itemHeight = 40.dp,
                    selectedFontSize = 23f,
                    unselectedFontSize = 14.5f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Wheel: Unit (lbs/kg or ft/cm)
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            WheelColumn(
                items = unitItems,
                selectedIndex = selectedUnitIndex,
                onSelectedIndexChange = { idx ->
                    if (isTyping) {
                        onTypedValueConfirmed(typedText)
                        isTyping = false
                    }
                    onUnitIndexChange(idx)
                },
                onCenterItemTap = {
                    val nextUnit = (selectedUnitIndex + 1) % unitItems.size
                    onUnitIndexChange(nextUnit)
                },
                itemHeight = 28.dp,
                selectedFontSize = 17f,
                unselectedFontSize = 13.5f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 3-row snapping wheel column using top & bottom spacer rows so all lists
 * center and snap accurately in the middle slot.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onCenterItemTap: () -> Unit,
    itemHeight: Dp,
    selectedFontSize: Float,
    unselectedFontSize: Float,
    modifier: Modifier = Modifier
) {
    val safeSelected = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeSelected)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    LaunchedEffect(safeSelected, items.size) {
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != safeSelected) {
            listState.animateScrollToItem(safeSelected)
        }
    }

    val centerIndex by remember(items.size) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val realVisible = layoutInfo.visibleItemsInfo.filter { it.index in 1..items.size }
            if (realVisible.isEmpty()) {
                safeSelected
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closestLazy = realVisible.minByOrNull { item ->
                    abs((item.offset + item.size / 2) - viewportCenter)
                }?.index ?: (safeSelected + 1)
                (closestLazy - 1).coerceIn(0, (items.size - 1).coerceAtLeast(0))
            }
        }
    }

    LaunchedEffect(centerIndex, listState.isScrollInProgress) {
        if (centerIndex in items.indices && centerIndex != selectedIndex) {
            onSelectedIndexChange(centerIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(itemHeight * 3)
    ) {
        // Top Spacer Row
        item(key = "top_spacer") {
            Spacer(modifier = Modifier.height(itemHeight))
        }

        // Actual Selectable Items
        items(count = items.size, key = { idx -> "item_${idx}_${items[idx]}" }) { index ->
            val isSelected = index == centerIndex
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 0.92f,
                animationSpec = tween(durationMillis = 150),
                label = "wheelItemScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isSelected) {
                            onCenterItemTap()
                        } else {
                            scope.launch {
                                listState.animateScrollToItem(index)
                            }
                            onSelectedIndexChange(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    color = if (isSelected) TextWhite else TextMutedGray,
                    fontSize = (if (isSelected) selectedFontSize else unselectedFontSize).sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        // Bottom Spacer Row
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(itemHeight))
        }
    }
}

// =========================================================================
// FINALIZE ONBOARDING & POPULATE VIEWMODEL FOR CONFIRMATION SCREEN
// =========================================================================

@Suppress("UNCHECKED_CAST")
private fun finalizeOnboardingData(
    context: Context,
    viewModel: FitnessViewModel,
    name: String,
    contactNo: String,
    age: Int,
    gender: String,
    heightCm: Double,
    weightKg: Double,
    activityLevel: String,
    fitnessGoal: String,
    targetWeightKg: Double,
    workoutFrequency: String,
    workoutSplit: String
) {
    val cleanName = name.ifBlank { "User" }
    val birthYear = (2026 - age).coerceIn(1920, 2016)
    val roundedHeightCm = (heightCm * 10.0).roundToInt() / 10.0
    val roundedWeightKg = (weightKg * 10.0).roundToInt() / 10.0
    val roundedTargetWeightKg = (targetWeightKg * 10.0).roundToInt() / 10.0

    // 1. Update initial details through existing ViewModel method
    viewModel.updateInitialDetails(
        name = cleanName,
        gender = gender,
        dobYear = birthYear,
        contact = contactNo,
        heightCm = roundedHeightCm,
        weightKg = roundedWeightKg
    )

    // 2. Standard Mifflin-St Jeor & Macro Calculations
    val heightMeters = (roundedHeightCm / 100.0).coerceAtLeast(0.5)
    val bmi = ((roundedWeightKg / (heightMeters * heightMeters)) * 10.0).roundToInt() / 10.0

    val baseBmr = (10.0 * roundedWeightKg) + (6.25 * roundedHeightCm) - (5.0 * age)
    val bmr = when (gender.trim().lowercase()) {
        "male" -> baseBmr + 5.0
        "female" -> baseBmr - 161.0
        else -> baseBmr - 78.0
    }

    val activityMultiplier = when {
        activityLevel.contains("Sedentary", ignoreCase = true) -> 1.2
        activityLevel.contains("Lightly", ignoreCase = true) -> 1.375
        activityLevel.contains("Moderately", ignoreCase = true) -> 1.55
        else -> 1.725 // Highly Active
    }

    val maintenanceCal = (bmr * activityMultiplier).roundToInt()

    val targetCal = when {
        fitnessGoal.contains("Lose", ignoreCase = true) -> (maintenanceCal - 500).coerceAtLeast(1200)
        fitnessGoal.contains("Gain", ignoreCase = true) -> maintenanceCal + 400
        fitnessGoal.contains("Recomposition", ignoreCase = true) -> (maintenanceCal - 200).coerceAtLeast(1400)
        else -> maintenanceCal
    }

    val proteinGoal = when {
        fitnessGoal.contains("Gain", ignoreCase = true) ||
                fitnessGoal.contains("Recomposition", ignoreCase = true) -> (roundedWeightKg * 2.0).roundToInt()
        fitnessGoal.contains("Lose", ignoreCase = true) -> (roundedWeightKg * 1.8).roundToInt()
        else -> (roundedWeightKg * 1.6).roundToInt()
    }

    val rawWaterLiters = (roundedWeightKg * 0.035) + if (activityMultiplier >= 1.55) 0.5 else 0.0
    val waterGoal = ((rawWaterLiters * 10.0).roundToInt() / 10.0).coerceAtLeast(2.5)

    // 3. Generate Username: INITIALS + BIRTH_YEAR + LAST 4 DIGITS OF CONTACT NO
    val cleanWords = cleanName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    val initials = when {
        cleanWords.size >= 2 -> "${cleanWords.first().first()}${cleanWords.last().first()}".uppercase()
        cleanWords.size == 1 && cleanWords.first().length >= 2 -> cleanWords.first().take(2).uppercase()
        cleanWords.size == 1 -> "${cleanWords.first().first()}X".uppercase()
        else -> "US"
    }
    val last4Phone = contactNo.filter { it.isDigit() }.takeLast(4).padStart(4, '0')
    val generatedUsername = "$initials$birthYear$last4Phone"

    // 4. Save Carbs, Fats, Target Weight & Contact in SharedPreferences
    val defaultCarbs = (targetCal * 0.50 / 4.0).roundToInt()
    val defaultFats = (targetCal * 0.25 / 9.0).roundToInt()
    val userKey = generatedUsername.lowercase()
    context.getSharedPreferences("fitness_profile_extra_prefs", Context.MODE_PRIVATE)
        .edit()
        .putInt("${userKey}_carbs", defaultCarbs)
        .putInt("${userKey}_fats", defaultFats)
        .putFloat("${userKey}_target_weight", roundedTargetWeightKg.toFloat())
        .putString("${userKey}_contact", contactNo)
        .apply()

    val normalizedFrequency = workoutFrequency.replace("week", "Week")

    val currentTemp = viewModel.tempOnboarding.value
    val completedTempProfile = currentTemp.copy(
        username = if (currentTemp.username.isNotBlank()) currentTemp.username else generatedUsername,
        name = cleanName,
        birthYear = birthYear,
        gender = gender,
        heightCm = roundedHeightCm,
        weightKg = roundedWeightKg,
        lastWeekEndWeightKg = roundedWeightKg,
        bmi = bmi,
        fitnessGoal = fitnessGoal,
        workoutFrequency = normalizedFrequency,
        workoutSplit = workoutSplit,
        dailyCalorieGoal = targetCal,
        dailyProteinGoal = proteinGoal,
        dailyWaterGoalLiters = waterGoal,
        dailyStepGoal = if (currentTemp.dailyStepGoal > 0) currentTemp.dailyStepGoal else 10000
    )

    // 5. Update tempOnboarding StateFlow inside FitnessViewModel
    try {
        for (field in viewModel.javaClass.declaredFields) {
            field.isAccessible = true
            val obj = field.get(viewModel)
            if (obj is MutableStateFlow<*>) {
                val value = obj.value
                if (value is UserProfile) {
                    val fieldName = field.name.lowercase()
                    if (fieldName.contains("temp") || fieldName.contains("onboarding")) {
                        (obj as MutableStateFlow<UserProfile>).value = completedTempProfile
                        break
                    }
                }
            }
        }
    } catch (_: Exception) {
    }
}