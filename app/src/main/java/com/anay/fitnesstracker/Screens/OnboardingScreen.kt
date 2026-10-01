package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.components.SplashTopIcons
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient



private val DarkPillColor = Color(0xFF000000)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val PlaceholderGray = Color(0xFF8E9094)

@Composable
fun OnboardingScreen(
    viewModel: FitnessViewModel,
    onNext: () -> Unit
) {

    var name by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") }
    var contactNo by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }

    // Dropdown state for Gender
    val genderList = listOf("Male", "Female", "Other")
    var selectedGender by remember { mutableStateOf("Gender") }
    var genderDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()

            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Screen Heading
        Text(
            text = "Let’s Get You\nOnboarded!",
            color = PrimaryGreen,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 38.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(44.dp))

        // 1. Name Pill
        OnboardingPillField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Name"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Gender Dropdown Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(DarkPillColor)
                .clickable { genderDropdownOpen = true }
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedGender,
                    color = if (selectedGender == "Gender") PlaceholderGray else TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = Color.Gray
                )
            }

            DropdownMenu(
                expanded = genderDropdownOpen,
                onDismissRequest = { genderDropdownOpen = false },
                modifier = Modifier.background(Color(0xFF1E1E20))
            ) {
                genderList.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = TextWhite) },
                        onClick = {
                            selectedGender = option
                            genderDropdownOpen = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Date of Birth (Year) Pill
        OnboardingPillField(
            value = birthYear,
            onValueChange = { birthYear = it },
            placeholder = "Birth year",
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Contact No. Pill
        OnboardingPillField(
            value = contactNo,
            onValueChange = { contactNo = it },
            placeholder = "Contact No.",
            keyboardType = KeyboardType.Phone
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Height Pill (with CM unit)
        OnboardingPillField(
            value = heightText,
            onValueChange = { heightText = it },
            placeholder = "Height",
            unitLabel = "CM",
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 6. Weight Pill (with KG unit)
        OnboardingPillField(
            value = weightText,
            onValueChange = { weightText = it },
            placeholder = "Weight",
            unitLabel = "KG",
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(24.dp))

        SplashTopIcons()

        Spacer(modifier = Modifier.height(48.dp))

        // Next Button Pill
        Row(
            modifier = Modifier
                .width(180.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(DarkPillColor)
                .clickable {
                    val year = birthYear.toIntOrNull() ?: 2000
                    val height = heightText.toDoubleOrNull() ?: 175.0
                    val weight = weightText.toDoubleOrNull() ?: 70.0

                    viewModel.updateInitialDetails(
                        name = name.ifBlank { "User" },
                        gender = if (selectedGender != "Gender") selectedGender else "Male",
                        dobYear = year,
                        contact = contactNo,
                        heightCm = height,
                        weightKg = weight
                    )
                    onNext()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Next",
                color = PrimaryGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun OnboardingPillField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    unitLabel: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(DarkPillColor)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = PlaceholderGray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = TextStyle(
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!unitLabel.isNullOrEmpty()) {
                Text(
                    text = unitLabel,
                    color = PlaceholderGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}