package com.anay.fitnesstracker.Screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.Routes
import com.anay.fitnesstracker.data.UserProfile
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import com.anay.fitnesstracker.ui.theme.glassEffect
import com.anay.fitnesstracker.util.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val AvatarColor = Color(0xFFD4D4D6)
private val UpdatePillBg = Color(0xFFB5B9B7)
private val LogoutRed = Color(0xFFE53935)
private val EditFieldBg = Color(0xFF14261D)
private val EditFieldBorder = Color(0xFF27D07F).copy(alpha = 0.35f)

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    fitnessViewModel: FitnessViewModel,
    onLogout: () -> Unit
) {
    val hazeState = rememberHazeState()
    val user by fitnessViewModel.currentUser.collectAsState()
    val avatarBitmap = remember(user?.avatarBase64) {
        ImageUtils.base64ToBitmap(user?.avatarBase64)
    }

    val calculatedAge = remember(user?.birthYear) {
        if (user != null && user!!.birthYear > 0) {
            (2026 - user!!.birthYear).coerceAtLeast(0)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(state = hazeState)
            .appBackgroundGradient()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Top Header with Profile Title & Logout Pill
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Profile",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF241515))
                        .border(
                            1.dp,
                            LogoutRed.copy(alpha = 0.18f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            fitnessViewModel.logout {
                                onLogout()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = LogoutRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Logout",
                        color = LogoutRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. User Header Pill Card (Name & Age on Left, Avatar on Right)
            UserHeaderPillCard(
                name = user?.name?.ifBlank { "User Profile" } ?: "User Profile",
                age = calculatedAge,
                avatarBitmap = avatarBitmap,
                hazeState = hazeState
            )

            Spacer(modifier = Modifier.height(26.dp))

            // 2. Fitness Profile Section Header
            Text(
                text = "Fitness Profile",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Editable Fitness Profile Card
            FitnessProfileCard(
                user = user,
                hazeState = hazeState
            )

            Spacer(modifier = Modifier.height(26.dp))

            // 3. Settings Card
            SettingsCard(
                onCustomRemindersClick = { onNavigate(Routes.CUSTOM_REMINDER) },
                onPersonalInfoClick = { onNavigate(Routes.PERSONAL_INFO) },
                onNotificationsClick = { onNavigate(Routes.NOTIFICATIONS) },
                onAboutClick = { onNavigate(Routes.ABOUT) },
                hazeState = hazeState
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                color = Color(0xFF8E9094),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
private fun UserHeaderPillCard(
    name: String,
    age: Int?,
    avatarBitmap: android.graphics.Bitmap?,
    hazeState: HazeState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(
                hazeState = hazeState,
                blurRadius = 16.dp,
                cornerRadius = 26.dp,
                shadowEnabled = true
            )
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = name,
                color = PrimaryGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = age?.let { "$it Years" } ?: "- Years",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .border(1.dp, PrimaryGreen.copy(alpha = 0.22f), CircleShape)
                .background(AvatarColor),
            contentAlignment = Alignment.Center
        ) {
            if (avatarBitmap != null) {
                Image(
                    bitmap = avatarBitmap.asImageBitmap(),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun FitnessProfileCard(
    user: UserProfile?,
    hazeState: HazeState
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("fitness_profile_extra_prefs", Context.MODE_PRIVATE)
    }
    val userKey = user?.username?.trim()?.lowercase().orEmpty()

    var isEditing by remember { mutableStateOf(false) }

    // Default macro fallbacks
    val defaultCal = user?.dailyCalorieGoal ?: 2400
    val defaultCarbs = (defaultCal * 0.50 / 4.0).roundToInt()
    val defaultFats = (defaultCal * 0.25 / 9.0).roundToInt()

    // State for all 10 editable fields
    var goalInput by remember(user?.fitnessGoal) {
        mutableStateOf(user?.fitnessGoal?.ifBlank { "Body Recomposition" } ?: "Body Recomposition")
    }
    var weightInput by remember(user?.weightKg) {
        mutableStateOf(formatCleanNumber(user?.weightKg ?: 75.0))
    }
    var heightInput by remember(user?.heightCm) {
        mutableStateOf(formatCleanNumber(user?.heightCm ?: 184.0))
    }
    var bmiInput by remember(user?.bmi, user?.weightKg, user?.heightCm) {
        val initialBmi = if ((user?.bmi ?: 0.0) > 0.0) {
            user!!.bmi
        } else {
            val hMeters = ((user?.heightCm ?: 184.0) / 100.0).coerceAtLeast(0.5)
            (user?.weightKg ?: 75.0) / (hMeters * hMeters)
        }
        mutableStateOf(initialBmi.roundToInt().toString())
    }
    var caloriesInput by remember(user?.dailyCalorieGoal) {
        mutableStateOf((user?.dailyCalorieGoal ?: 2400).toString())
    }
    var proteinInput by remember(user?.dailyProteinGoal) {
        mutableStateOf((user?.dailyProteinGoal ?: 120).toString())
    }
    var carbsInput by remember(userKey, defaultCarbs) {
        val savedCarbs = prefs.getInt("${userKey}_carbs", -1)
        mutableStateOf((if (savedCarbs > 0) savedCarbs else defaultCarbs).toString())
    }
    var fatsInput by remember(userKey, defaultFats) {
        val savedFats = prefs.getInt("${userKey}_fats", -1)
        mutableStateOf((if (savedFats > 0) savedFats else defaultFats).toString())
    }
    var waterInput by remember(user?.dailyWaterGoalLiters) {
        mutableStateOf(formatCleanNumber(user?.dailyWaterGoalLiters ?: 4.0))
    }
    var stepsInput by remember(user?.dailyStepGoal) {
        mutableStateOf((user?.dailyStepGoal ?: 10000).toString())
    }

    // Automatically recalculate BMI when Weight or Height is edited
    fun recalculateBmiFromInputs(newWeightStr: String, newHeightStr: String) {
        val w = newWeightStr.toDoubleOrNull()
        val hCm = newHeightStr.toDoubleOrNull()
        if (w != null && hCm != null && hCm > 30.0) {
            val hM = hCm / 100.0
            val calcBmi = w / (hM * hM)
            bmiInput = calcBmi.roundToInt().toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(
                hazeState = hazeState,
                blurRadius = 16.dp,
                cornerRadius = 26.dp,
                shadowEnabled = true
            )
            .padding(horizontal = 22.dp, vertical = 22.dp)
    ) {
        AnimatedContent(
            targetState = isEditing,
            label = "fitnessProfileEditTransition"
        ) { editing ->
            if (!editing) {
                val formattedSteps = remember(stepsInput) {
                    val stepsVal = stepsInput.replace(",", "").toIntOrNull() ?: 10000
                    NumberFormat.getNumberInstance(Locale.US).format(stepsVal)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FitnessMetricReadRow(label = "Goal", value = goalInput)
                    FitnessMetricReadRow(label = "Weight", value = "$weightInput Kg")
                    FitnessMetricReadRow(label = "Height", value = "$heightInput Cm")
                    FitnessMetricReadRow(label = "BMI", value = bmiInput)
                    FitnessMetricReadRow(label = "Daily calories target", value = "$caloriesInput kcals")
                    FitnessMetricReadRow(label = "Daily protein target", value = "$proteinInput g")
                    FitnessMetricReadRow(label = "Daily carbs target", value = "$carbsInput g")
                    FitnessMetricReadRow(label = "Daily fats target", value = "$fatsInput g")
                    FitnessMetricReadRow(label = "Daily water target", value = "$waterInput L")
                    FitnessMetricReadRow(label = "Daily steps target", value = formattedSteps)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FitnessMetricEditRow(
                        label = "Goal",
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        keyboardType = KeyboardType.Text
                    )
                    FitnessMetricEditRow(
                        label = "Weight",
                        value = weightInput,
                        unit = "Kg",
                        onValueChange = {
                            weightInput = it
                            recalculateBmiFromInputs(it, heightInput)
                        },
                        keyboardType = KeyboardType.Decimal
                    )
                    FitnessMetricEditRow(
                        label = "Height",
                        value = heightInput,
                        unit = "Cm",
                        onValueChange = {
                            heightInput = it
                            recalculateBmiFromInputs(weightInput, it)
                        },
                        keyboardType = KeyboardType.Decimal
                    )
                    FitnessMetricEditRow(
                        label = "BMI",
                        value = bmiInput,
                        onValueChange = { bmiInput = it },
                        keyboardType = KeyboardType.Decimal
                    )
                    FitnessMetricEditRow(
                        label = "Daily calories target",
                        value = caloriesInput,
                        unit = "kcals",
                        onValueChange = { caloriesInput = it },
                        keyboardType = KeyboardType.Number
                    )
                    FitnessMetricEditRow(
                        label = "Daily protein target",
                        value = proteinInput,
                        unit = "g",
                        onValueChange = { proteinInput = it },
                        keyboardType = KeyboardType.Number
                    )
                    FitnessMetricEditRow(
                        label = "Daily carbs target",
                        value = carbsInput,
                        unit = "g",
                        onValueChange = { carbsInput = it },
                        keyboardType = KeyboardType.Number
                    )
                    FitnessMetricEditRow(
                        label = "Daily fats target",
                        value = fatsInput,
                        unit = "g",
                        onValueChange = { fatsInput = it },
                        keyboardType = KeyboardType.Number
                    )
                    FitnessMetricEditRow(
                        label = "Daily water target",
                        value = waterInput,
                        unit = "L",
                        onValueChange = { waterInput = it },
                        keyboardType = KeyboardType.Decimal
                    )
                    FitnessMetricEditRow(
                        label = "Daily steps target",
                        value = stepsInput,
                        onValueChange = { stepsInput = it.replace(",", "") },
                        keyboardType = KeyboardType.Number
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bottom-Right Action Pill(s)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                // Cancel Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable { isEditing = false }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = TextWhite,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cancel",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
            }

            // Update / Save Pill Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isEditing) PrimaryGreen else UpdatePillBg)
                    .clickable {
                        if (!isEditing) {
                            isEditing = true
                        } else {
                            val newWeight = weightInput.toDoubleOrNull() ?: user?.weightKg ?: 75.0
                            val newHeight = heightInput.toDoubleOrNull() ?: user?.heightCm ?: 184.0
                            val newBmi = bmiInput.toDoubleOrNull() ?: user?.bmi ?: 21.0
                            val newCalories = caloriesInput.toIntOrNull() ?: user?.dailyCalorieGoal ?: 2400
                            val newProtein = proteinInput.toIntOrNull() ?: user?.dailyProteinGoal ?: 120
                            val newCarbs = carbsInput.toIntOrNull() ?: defaultCarbs
                            val newFats = fatsInput.toIntOrNull() ?: defaultFats
                            val newWater = waterInput.toDoubleOrNull() ?: user?.dailyWaterGoalLiters ?: 4.0
                            val newSteps = stepsInput.replace(",", "").toIntOrNull() ?: user?.dailyStepGoal ?: 10000
                            val cleanGoal = goalInput.trim().ifBlank { "Body Recomposition" }

                            // Persist Carbs & Fats in SharedPreferences
                            prefs.edit()
                                .putInt("${userKey}_carbs", newCarbs)
                                .putInt("${userKey}_fats", newFats)
                                .apply()

                            // Update Firestore (triggers instant local cache & ViewModel listener update)
                            if (userKey.isNotBlank()) {
                                val updates = mapOf(
                                    "fitnessGoal" to cleanGoal,
                                    "weightKg" to newWeight,
                                    "heightCm" to newHeight,
                                    "bmi" to newBmi,
                                    "dailyCalorieGoal" to newCalories,
                                    "dailyProteinGoal" to newProtein,
                                    "dailyWaterGoalLiters" to newWater,
                                    "dailyStepGoal" to newSteps
                                )
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(userKey)
                                    .update(updates)
                            }

                            isEditing = false
                            Toast.makeText(context, "Fitness profile updated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = if (isEditing) "Save" else "Update",
                    tint = Color.Black,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEditing) "Save" else "Update",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FitnessMetricReadRow(
    label: String,
    value: String
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = PrimaryGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("$label : ")
            }
            withStyle(
                style = SpanStyle(
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(value)
            }
        }
    )
}

@Composable
private fun FitnessMetricEditRow(
    label: String,
    value: String,
    unit: String? = null,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label : ",
            color = PrimaryGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.1f)
        )

        Row(
            modifier = Modifier
                .weight(0.9f)
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x80ffffff))
                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(PrimaryGreen),
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f)
            )
            if (!unit.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = Color(0xFF8E9094),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatCleanNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

@Composable
private fun SettingsCard(
    onCustomRemindersClick: () -> Unit,
    onPersonalInfoClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAboutClick: () -> Unit,
    hazeState: HazeState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(
                hazeState = hazeState,
                blurRadius = 16.dp,
                cornerRadius = 26.dp,
                shadowEnabled = true
            )
            .padding(vertical = 22.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Settings",
            color = TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsButton(label = "Create Reminders", onClick = onCustomRemindersClick)
            SettingsButton(label = "Account Settings", onClick = onPersonalInfoClick)
            SettingsButton(label = "Notifications", onClick = onNotificationsClick)
            SettingsButton(label = "About", onClick = onAboutClick)
        }
    }
}

@Composable
private fun SettingsButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF000000))
            .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(TextWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = label,
                tint = Color.Black,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}