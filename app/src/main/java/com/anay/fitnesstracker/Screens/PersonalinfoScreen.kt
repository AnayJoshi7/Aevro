package com.anay.fitnesstracker.Screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import com.anay.fitnesstracker.util.ImageUtils

private val BgGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1B1C1E),
        Color(0xFF131416),
        Color(0xFF2C2E31),
        Color(0xFF111214)
    )
)
private val CardBackground = Color(0xFF070708)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9E9E)
private val AvatarColor = Color(0xFFD4D4D6)

@Composable
fun PersonalInfoScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    var isEditing by remember { mutableStateOf(false) }

    var editableName by remember(user) { mutableStateOf(user?.name ?: "") }
    var editableYear by remember(user) { mutableStateOf(user?.birthYear?.toString() ?: "2000") }
    var editableContact by remember(user) { mutableStateOf(user?.contactNo ?: "") }

    val avatarBitmap = remember(user?.avatarBase64) {
        ImageUtils.base64ToBitmap(user?.avatarBase64)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.updateProfile(
                name = editableName,
                birthYear = editableYear.toIntOrNull() ?: 2000,
                contactNo = editableContact,
                avatarUri = uri
            )
        }
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Personal\nInformation",
            color = PrimaryGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Main Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Avatar Box with Translucent Overlay & Pen Icon when in Edit Mode
            Box(
                modifier = Modifier
                    .size(105.dp)
                    .clip(CircleShape)
                    .background(AvatarColor)
                    .clickable(enabled = isEditing) { photoPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isEditing) Modifier.alpha(0.55f) else Modifier),
                        contentScale = ContentScale.Crop
                    )
                }

                // If editing: Show darker translucent tint and Pen icon over the avatar
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change photo",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user?.name ?: "User",
                color = PrimaryGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (!isEditing) {
                // View Mode
                val age = user?.birthYear?.let { (2026 - it).coerceAtLeast(0) } ?: "-"
                InfoFieldRow(label = "Name", value = user?.name ?: "-")
                Spacer(modifier = Modifier.height(12.dp))
                InfoFieldRow(label = "Age", value = "$age")
                Spacer(modifier = Modifier.height(12.dp))
                InfoFieldRow(label = "Contact No.", value = user?.contactNo ?: "-")
                Spacer(modifier = Modifier.height(12.dp))
                InfoFieldRow(label = "Birth-Year", value = "${user?.birthYear ?: "-"}")
            } else {
                // Edit Mode
                Text(
                    text = "Click on avatar to edit",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name Input
                EditFieldInput(
                    label = "Name",
                    value = editableName,
                    onValueChange = { editableName = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Birth Year Input
                EditFieldInput(
                    label = "Birth Year",
                    value = editableYear,
                    keyboardType = KeyboardType.Number,
                    onValueChange = { editableYear = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Contact No Input
                EditFieldInput(
                    label = "Contact No.",
                    value = editableContact,
                    keyboardType = KeyboardType.Phone,
                    onValueChange = { editableContact = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Edit / Save Pill Button
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE4E4E6))
                    .clickable {
                        if (isEditing) {
                            viewModel.updateProfile(
                                name = editableName,
                                birthYear = editableYear.toIntOrNull() ?: 2000,
                                contactNo = editableContact,
                                avatarUri = null
                            )
                        }
                        isEditing = !isEditing
                    }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = if (isEditing) "Save" else "Edit",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEditing) "Save" else "Edit",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EditFieldInput(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InfoFieldRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = TextWhite, fontSize = 16.sp, modifier = Modifier.weight(1.2f))
        Text(text = ":", color = TextWhite, fontSize = 16.sp, modifier = Modifier.weight(0.3f))
        Text(text = value, color = TextWhite, fontSize = 16.sp, modifier = Modifier.weight(1.5f))
    }
}