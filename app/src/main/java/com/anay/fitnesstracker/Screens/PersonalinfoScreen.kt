package com.anay.fitnesstracker.Screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.components.DeleteAccountDialog
import com.anay.fitnesstracker.components.EditUsernameDialog
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import com.anay.fitnesstracker.util.ImageUtils
import java.time.LocalDate

private val CardBackground = Color(0x1A3ddc84)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9E9E)
private val AvatarColor = Color(0xFFD4D4D6)

@Composable
fun PersonalInfoScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showEditUsernameDialog by remember { mutableStateOf(false) }

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
                .border(1.dp, Color(0xFF3ddc84).copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Avatar Box
            Box(
                modifier = Modifier
                    .size(105.dp)
                    .clip(CircleShape)
                    .background(AvatarColor)
                    .border(1.dp, Color(0xFF3ddc84).copy(alpha = 0.18f), CircleShape)
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
                // View Mode: All details displayed as clean informational rows
                val currentYear = LocalDate.now().year
                val age = user?.birthYear?.let { (currentYear - it).coerceAtLeast(0) } ?: "-"

                InfoFieldRow(label = "Username", value = user?.username?.let { "@$it" } ?: "-")
                Spacer(modifier = Modifier.height(12.dp))
                InfoFieldRow(label = "Name", value = user?.name ?: "-")
                Spacer(modifier = Modifier.height(12.dp))
                InfoFieldRow(label = "Age", value = "$age")
                Spacer(modifier = Modifier.height(12.dp))
                InfoFieldRow(label = "Contact No.", value = user?.contactNo ?: "-")
                Spacer(modifier = Modifier.height(12.dp))
                InfoFieldRow(label = "Birth-Year", value = "${user?.birthYear ?: "-"}")
            } else {
                // Edit Mode: Input fields enabled
                Text(
                    text = "Click on avatar or fields to edit",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Editable Username Row (Opens dialog for uniqueness validation)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Username",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x80FFFFFF))
                            .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { showEditUsernameDialog = true }
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user?.username?.let { "@$it" } ?: "Set username",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Username",
                            tint = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1AFF5252))
                    .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable { showDeleteDialog = true }
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Delete Account",
                    color = Color(0xFFFF5252),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (showDeleteDialog) {
                DeleteAccountDialog(
                    username = user?.username ?: "",
                    isDeleting = isDeleting,
                    onConfirm = {
                        isDeleting = true
                        viewModel.deleteAccount(
                            onSuccess = {
                                isDeleting = false
                                showDeleteDialog = false
                                Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                onAccountDeleted()
                            },
                            onError = { err ->
                                isDeleting = false
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onDismiss = { showDeleteDialog = false }
                )
            }

            // Edit / Save Pill Button
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x90E4E4E6))
                    .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.18f), RoundedCornerShape(20.dp))
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

    if (showEditUsernameDialog) {
        EditUsernameDialog(
            currentUsername = user?.username ?: "",
            viewModel = viewModel,
            onDismiss = { showEditUsernameDialog = false }
        )
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
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x80FFFFFF))
                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
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