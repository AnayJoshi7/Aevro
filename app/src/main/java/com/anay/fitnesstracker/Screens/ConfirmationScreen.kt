package com.anay.fitnesstracker.Screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.components.NextPillButton
import com.anay.fitnesstracker.data.SessionManager
import com.anay.fitnesstracker.data.UserProfile
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val CardDarkBg = Color(0xFF0F1E16)
private val CardBorderColor = Color(0xFF27D07F).copy(alpha = 0.22f)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF8E9094)
private val InputBg = Color(0xFF162B20)
private val InputBorder = Color(0xFF27D07F).copy(alpha = 0.40f)
private val ErrorRed = Color(0xFFFF5252)

@Composable
fun ConfirmationScreen(
    viewModel: FitnessViewModel,
    onEnterDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tempUser by viewModel.tempOnboarding.collectAsState()

    val extraPrefs = remember(context) {
        context.getSharedPreferences("fitness_profile_extra_prefs", Context.MODE_PRIVATE)
    }

    // ---------- Username Edit State ----------
    var isEditingUsername by remember { mutableStateOf(false) }
    var currentUsername by remember(tempUser.username) {
        mutableStateOf(tempUser.username.trim().uppercase())
    }
    var usernameInput by remember(currentUsername) {
        mutableStateOf(currentUsername)
    }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var isCheckingUsername by remember { mutableStateOf(false) }

    // ---------- Goals Edit State ----------
    var isEditingGoals by remember { mutableStateOf(false) }

    val defaultCal = if (tempUser.dailyCalorieGoal > 0) tempUser.dailyCalorieGoal else 2400
    val defaultCarbs = (defaultCal * 0.50 / 4.0).roundToInt()
    val defaultFats = (defaultCal * 0.25 / 9.0).roundToInt()

    var caloriesInput by remember(tempUser.dailyCalorieGoal) {
        mutableStateOf(defaultCal.toString())
    }
    var proteinInput by remember(tempUser.dailyProteinGoal) {
        mutableStateOf((if (tempUser.dailyProteinGoal > 0) tempUser.dailyProteinGoal else 120).toString())
    }
    var carbsInput by remember(defaultCarbs) {
        val saved = extraPrefs.getInt("${currentUsername.lowercase()}_carbs", -1)
        mutableStateOf((if (saved > 0) saved else defaultCarbs).toString())
    }
    var fatsInput by remember(defaultFats) {
        val saved = extraPrefs.getInt("${currentUsername.lowercase()}_fats", -1)
        mutableStateOf((if (saved > 0) saved else defaultFats).toString())
    }
    var waterInput by remember(tempUser.dailyWaterGoalLiters) {
        val waterVal = if (tempUser.dailyWaterGoalLiters > 0.0) tempUser.dailyWaterGoalLiters else 4.0
        mutableStateOf(formatCleanDecimal(waterVal))
    }
    var stepsInput by remember(tempUser.dailyStepGoal) {
        mutableStateOf((if (tempUser.dailyStepGoal > 0) tempUser.dailyStepGoal else 10000).toString())
    }
    var fitnessGoalInput by remember(tempUser.fitnessGoal) {
        mutableStateOf(tempUser.fitnessGoal.ifBlank { "Body Recomposition" })
    }
    var workoutSplitInput by remember(tempUser.workoutSplit) {
        mutableStateOf(tempUser.workoutSplit.ifBlank { "Push Pull Legs" })
    }
    var workoutDaysInput by remember(tempUser.workoutFrequency) {
        val daysDigit = tempUser.workoutFrequency.take(1).toIntOrNull() ?: 6
        mutableStateOf(daysDigit.toString())
    }

    val formattedSteps = remember(stepsInput) {
        val stepsVal = stepsInput.replace(",", "").toIntOrNull() ?: tempUser.dailyStepGoal
        NumberFormat.getNumberInstance(Locale.US).format(stepsVal)
    }

    // Syncs all current UI values back into ViewModel's tempOnboarding & SharedPreferences
    fun applyGoalsToProfile(targetUsername: String = currentUsername): UserProfile {
        val finalCal = caloriesInput.toIntOrNull()?.coerceAtLeast(800) ?: tempUser.dailyCalorieGoal
        val finalProtein = proteinInput.toIntOrNull()?.coerceAtLeast(20) ?: tempUser.dailyProteinGoal
        val finalCarbs = carbsInput.toIntOrNull()?.coerceAtLeast(20) ?: defaultCarbs
        val finalFats = fatsInput.toIntOrNull()?.coerceAtLeast(15) ?: defaultFats
        val finalWater = waterInput.toDoubleOrNull()?.coerceAtLeast(0.5) ?: tempUser.dailyWaterGoalLiters
        val finalSteps = stepsInput.replace(",", "").toIntOrNull()?.coerceAtLeast(500) ?: tempUser.dailyStepGoal
        val finalGoal = fitnessGoalInput.trim().ifBlank { "Body Recomposition" }
        val finalSplit = workoutSplitInput.trim().ifBlank { "Push Pull Legs" }
        val finalDays = (workoutDaysInput.toIntOrNull() ?: 6).coerceIn(1, 7)
        val finalFrequency = "$finalDays Days a Week"

        caloriesInput = finalCal.toString()
        proteinInput = finalProtein.toString()
        carbsInput = finalCarbs.toString()
        fatsInput = finalFats.toString()
        waterInput = formatCleanDecimal(finalWater)
        stepsInput = finalSteps.toString()
        fitnessGoalInput = finalGoal
        workoutSplitInput = finalSplit
        workoutDaysInput = finalDays.toString()

        val userKey = targetUsername.trim().lowercase()
        extraPrefs.edit()
            .putInt("${userKey}_carbs", finalCarbs)
            .putInt("${userKey}_fats", finalFats)
            .apply()

        val updatedProfile = tempUser.copy(
            username = targetUsername.trim().uppercase(),
            dailyCalorieGoal = finalCal,
            dailyProteinGoal = finalProtein,
            dailyWaterGoalLiters = finalWater,
            dailyStepGoal = finalSteps,
            fitnessGoal = finalGoal,
            workoutSplit = finalSplit,
            workoutFrequency = finalFrequency
        )
        updateViewModelTempProfile(viewModel, updatedProfile)
        return updatedProfile
    }

    // Validates and verifies username uniqueness in Firestore (case-insensitive)
    fun validateAndSaveUsername(onVerifiedSuccess: ((String) -> Unit)? = null) {
        val normalized = usernameInput.replace("\\s+".toRegex(), "").uppercase()
        usernameInput = normalized

        if (normalized.isBlank()) {
            usernameError = "Username cannot be empty"
            return
        }
        if (normalized.length < 4) {
            usernameError = "Username must be at least 4 characters"
            return
        }
        if (!normalized.all { it.isLetterOrDigit() || it == '_' }) {
            usernameError = "Only letters, numbers, and underscores allowed"
            return
        }

        // If unchanged from current username, accept immediately
        if (normalized.equals(currentUsername, ignoreCase = true)) {
            currentUsername = normalized
            usernameError = null
            isEditingUsername = false
            applyGoalsToProfile(normalized)
            onVerifiedSuccess?.invoke(normalized)
            return
        }

        isCheckingUsername = true
        usernameError = null

        scope.launch {
            val isTaken = checkUsernameTakenInFirestore(
                candidate = normalized,
                currentUsername = currentUsername
            )
            isCheckingUsername = false

            if (isTaken) {
                usernameError = "Username '$normalized' is already taken"
            } else {
                val previousUsername = currentUsername
                currentUsername = normalized
                usernameError = null
                isEditingUsername = false

                // Migrate any existing Firestore draft document if already created during onboarding
                migrateExistingDraftIfPresent(
                    context = context,
                    oldUsername = previousUsername,
                    newProfile = applyGoalsToProfile(normalized)
                )

                Toast.makeText(context, "Username updated to $normalized", Toast.LENGTH_SHORT).show()
                onVerifiedSuccess?.invoke(normalized)
            }
        }
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

            // 1. Username Card (with inline Edit & Uniqueness validation)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(CardDarkBg)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(26.dp))
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Username",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!isEditingUsername) {
                        EditIconPill(
                            label = "Edit",
                            onClick = {
                                usernameInput = currentUsername
                                usernameError = null
                                isEditingUsername = true
                            }
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCheckingUsername) {
                                CircularProgressIndicator(
                                    color = PrimaryGreen,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                SmallIconActionButton(
                                    icon = Icons.Default.Close,
                                    tint = TextWhite,
                                    bgColor = Color.White.copy(alpha = 0.12f),
                                    onClick = {
                                        usernameInput = currentUsername
                                        usernameError = null
                                        isEditingUsername = false
                                    }
                                )
                                SmallIconActionButton(
                                    icon = Icons.Default.Check,
                                    tint = Color.Black,
                                    bgColor = PrimaryGreen,
                                    onClick = { validateAndSaveUsername() }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedContent(
                    targetState = isEditingUsername,
                    label = "usernameEditTransition"
                ) { editing ->
                    if (!editing) {
                        Text(
                            text = currentUsername,
                            color = TextWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(InputBg)
                                    .border(
                                        1.dp,
                                        if (usernameError != null) ErrorRed else InputBorder,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = usernameInput,
                                    onValueChange = { input ->
                                        // Automatically strip spaces and normalize to uppercase
                                        usernameInput = input.replace(" ", "").uppercase()
                                        usernameError = null
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        keyboardType = KeyboardType.Ascii
                                    ),
                                    cursorBrush = SolidColor(PrimaryGreen),
                                    textStyle = TextStyle(
                                        color = TextWhite,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (usernameError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = usernameError!!,
                                    color = ErrorRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Save this username. You'll use it to log in.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Targets / Fitness Goals Card (with all goals & inline editing)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(CardDarkBg)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(26.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Top Row: First Goal + Edit/Save Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (!isEditingGoals) {
                            LargeGoalItem(
                                title = "Daily Calories-Intake Goal",
                                value = "$caloriesInput Kcals"
                            )
                        } else {
                            EditableLargeGoalItem(
                                title = "Daily Calories-Intake Goal",
                                value = caloriesInput,
                                unit = "Kcals",
                                keyboardType = KeyboardType.Number,
                                onValueChange = { caloriesInput = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    if (!isEditingGoals) {
                        EditIconPill(
                            label = "Edit",
                            onClick = { isEditingGoals = true }
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallIconActionButton(
                                icon = Icons.Default.Close,
                                tint = TextWhite,
                                bgColor = Color.White.copy(alpha = 0.12f),
                                onClick = {
                                    // Reset inputs back to current saved state
                                    caloriesInput = tempUser.dailyCalorieGoal.toString()
                                    proteinInput = tempUser.dailyProteinGoal.toString()
                                    waterInput = formatCleanDecimal(tempUser.dailyWaterGoalLiters)
                                    stepsInput = tempUser.dailyStepGoal.toString()
                                    fitnessGoalInput = tempUser.fitnessGoal.ifBlank { "Body Recomposition" }
                                    workoutSplitInput = tempUser.workoutSplit.ifBlank { "Push Pull Legs" }
                                    workoutDaysInput = (tempUser.workoutFrequency.take(1).toIntOrNull() ?: 6).toString()
                                    isEditingGoals = false
                                }
                            )
                            SmallIconActionButton(
                                icon = Icons.Default.Check,
                                tint = Color.Black,
                                bgColor = PrimaryGreen,
                                onClick = {
                                    applyGoalsToProfile()
                                    isEditingGoals = false
                                    Toast.makeText(context, "Fitness goals updated!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                if (!isEditingGoals) {
                    LargeGoalItem(
                        title = "Daily Protein Goal",
                        value = "$proteinInput Grams"
                    )

                    LargeGoalItem(
                        title = "Daily Carbs Goal",
                        value = "$carbsInput Grams"
                    )

                    LargeGoalItem(
                        title = "Daily Fats Goal",
                        value = "$fatsInput Grams"
                    )

                    LargeGoalItem(
                        title = "Daily Water Goal",
                        value = "$waterInput Liters"
                    )

                    LargeGoalItem(
                        title = "Daily Steps Goal",
                        value = "$formattedSteps Steps"
                    )

                    LargeGoalItem(
                        title = "Fitness Goal",
                        value = fitnessGoalInput
                    )

                    LargeGoalItem(
                        title = "Weekly Workout Split",
                        value = "$workoutSplitInput - $workoutDaysInput days"
                    )
                } else {
                    EditableLargeGoalItem(
                        title = "Daily Protein Goal",
                        value = proteinInput,
                        unit = "Grams",
                        keyboardType = KeyboardType.Number,
                        onValueChange = { proteinInput = it }
                    )

                    EditableLargeGoalItem(
                        title = "Daily Carbs Goal",
                        value = carbsInput,
                        unit = "Grams",
                        keyboardType = KeyboardType.Number,
                        onValueChange = { carbsInput = it }
                    )

                    EditableLargeGoalItem(
                        title = "Daily Fats Goal",
                        value = fatsInput,
                        unit = "Grams",
                        keyboardType = KeyboardType.Number,
                        onValueChange = { fatsInput = it }
                    )

                    EditableLargeGoalItem(
                        title = "Daily Water Goal",
                        value = waterInput,
                        unit = "Liters",
                        keyboardType = KeyboardType.Decimal,
                        onValueChange = { waterInput = it }
                    )

                    EditableLargeGoalItem(
                        title = "Daily Steps Goal",
                        value = stepsInput,
                        unit = "Steps",
                        keyboardType = KeyboardType.Number,
                        onValueChange = { stepsInput = it.replace(",", "") }
                    )

                    EditableLargeGoalItem(
                        title = "Fitness Goal",
                        value = fitnessGoalInput,
                        unit = null,
                        keyboardType = KeyboardType.Text,
                        onValueChange = { fitnessGoalInput = it }
                    )

                    // Weekly Workout Split + Days Editor
                    Column {
                        Text(
                            text = "Weekly Workout Split",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(InputBg)
                                    .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = workoutSplitInput,
                                    onValueChange = { workoutSplitInput = it },
                                    singleLine = true,
                                    cursorBrush = SolidColor(PrimaryGreen),
                                    textStyle = TextStyle(
                                        color = TextWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(InputBg)
                                    .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = workoutDaysInput,
                                    onValueChange = { workoutDaysInput = it.filter { ch -> ch.isDigit() }.take(1) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cursorBrush = SolidColor(PrimaryGreen),
                                    textStyle = TextStyle(
                                        color = TextWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "days",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Bottom Next Button
        NextPillButton(
            onClick = {
                val proceedToDashboard: (String) -> Unit = { verifiedUsername ->
                    val finalProfile = applyGoalsToProfile(verifiedUsername)
                    isEditingGoals = false

                    viewModel.confirmAndSaveProfile {
                        // Ensure Firestore & SessionManager reflect any inline edits made on this screen
                        val lowerKey = finalProfile.username.trim().lowercase()
                        SessionManager(context).saveUsername(finalProfile.username.trim().uppercase())
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(lowerKey)
                            .set(finalProfile)

                        onEnterDashboard()
                    }
                }

                if (isEditingUsername) {
                    validateAndSaveUsername { verifiedUsername ->
                        proceedToDashboard(verifiedUsername)
                    }
                } else {
                    proceedToDashboard(currentUsername)
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

@Composable
private fun EditableLargeGoalItem(
    title: String,
    value: String,
    unit: String?,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InputBg)
                .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(PrimaryGreen),
                textStyle = TextStyle(
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.weight(1f)
            )
            if (!unit.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = unit,
                    color = TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EditIconPill(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PrimaryGreen.copy(alpha = 0.14f))
            .border(1.dp, PrimaryGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = label,
            tint = PrimaryGreen,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = PrimaryGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SmallIconActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Checks Firestore (case-insensitively across lowercase/uppercase document IDs and username field)
 * to ensure the candidate username is unique.
 */
private suspend fun checkUsernameTakenInFirestore(
    candidate: String,
    currentUsername: String
): Boolean {
    val cleanUpper = candidate.trim().uppercase()
    val cleanLower = candidate.trim().lowercase()

    if (cleanUpper.equals(currentUsername.trim(), ignoreCase = true)) {
        return false
    }

    val db = FirebaseFirestore.getInstance()

    return try {
        val lowerDoc = db.collection("users").document(cleanLower).get().await()
        if (lowerDoc.exists()) return true

        val upperDoc = db.collection("users").document(cleanUpper).get().await()
        if (upperDoc.exists()) return true

        val queryUpper = db.collection("users")
            .whereEqualTo("username", cleanUpper)
            .limit(1)
            .get()
            .await()
        if (!queryUpper.isEmpty) return true

        val queryLower = db.collection("users")
            .whereEqualTo("username", cleanLower)
            .limit(1)
            .get()
            .await()
        !queryLower.isEmpty
    } catch (_: Exception) {
        // Fallback to local cache check if device is currently offline
        try {
            val cachedLower = db.collection("users").document(cleanLower).get(Source.CACHE).await()
            val cachedUpper = db.collection("users").document(cleanUpper).get(Source.CACHE).await()
            cachedLower.exists() || cachedUpper.exists()
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Updates the backing MutableStateFlow inside FitnessViewModel so confirmAndSaveProfile()
 * picks up all edited values immediately.
 */
@Suppress("UNCHECKED_CAST")
private fun updateViewModelTempProfile(viewModel: FitnessViewModel, updated: UserProfile) {
    try {
        for (field in viewModel.javaClass.declaredFields) {
            field.isAccessible = true
            val obj = field.get(viewModel)
            if (obj is MutableStateFlow<*>) {
                val currentVal = obj.value
                if (currentVal is UserProfile) {
                    val fieldName = field.name.lowercase()
                    if (fieldName.contains("temp") || fieldName.contains("onboarding")) {
                        (obj as MutableStateFlow<UserProfile>).value = updated
                        return
                    }
                }
            }
        }
    } catch (_: Exception) {
    }
}

/**
 * If an onboarding draft document was already created under the old username,
 * migrates it to the new username and removes the old document.
 */
private suspend fun migrateExistingDraftIfPresent(
    context: Context,
    oldUsername: String,
    newProfile: UserProfile
) {
    val oldLower = oldUsername.trim().lowercase()
    val newLower = newProfile.username.trim().lowercase()
    if (oldLower.isBlank() || oldLower == newLower) return

    val db = FirebaseFirestore.getInstance()
    try {
        val oldDoc = db.collection("users").document(oldLower).get().await()
        if (oldDoc.exists()) {
            db.collection("users").document(newLower).set(newProfile).await()
            db.collection("users").document(oldLower).delete()
            SessionManager(context).saveUsername(newProfile.username.trim().uppercase())
        }
    } catch (_: Exception) {
    }
}

private fun formatCleanDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}