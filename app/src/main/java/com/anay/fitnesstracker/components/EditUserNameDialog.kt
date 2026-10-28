package com.anay.fitnesstracker.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.data.viewmodel.UsernameCheckState
private val CardBorderColor = Color(0xFF3DDC84).copy(alpha = 0.20f)
private val DialogDarkBg = Color(0xFF161719)
private val PillGrayBg = Color(0xFFAEB2B0)
private val PrimaryGreen = Color(0xFF27D07F)
private val ErrorRed = Color(0xFFFF5252)
private val TextWhite = Color(0xFFFFFFFF)

@Composable
fun EditUsernameDialog(
    currentUsername: String,
    viewModel: FitnessViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputUsername by remember { mutableStateOf(currentUsername.removePrefix("@")) }
    val checkState by viewModel.usernameCheckState.collectAsState()
    var isSaving by remember { mutableStateOf(false) }
    val isAvailable = checkState is UsernameCheckState.Available || checkState == UsernameCheckState.Available
    val isChanged = inputUsername.trim().lowercase() != currentUsername.trim().lowercase().removePrefix("@")
    val canSave = isAvailable && isChanged && !isSaving

    // Run check immediately on load
    LaunchedEffect(Unit) {
        if (inputUsername.isNotBlank()) {
            viewModel.checkUsernameAvailability(inputUsername)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetUsernameCheckState()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DialogDarkBg)
                .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Username",
                    color = PrimaryGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextWhite.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose a unique handle for your profile",
                color = TextWhite.copy(alpha = 0.7f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Text Input Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PillGrayBg)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@",
                        color = Color(0xFF4A4E4C),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    BasicTextField(
                        value = inputUsername,
                        onValueChange = { raw ->
                            // Strip any typed '@' or spaces automatically
                            val filtered = raw.replace("@", "").replace(" ", "")
                            inputUsername = filtered
                            viewModel.checkUsernameAvailability(filtered)
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Trailing Status Indicator
                    when (checkState) {
                        UsernameCheckState.Checking -> {
                            CircularProgressIndicator(
                                color = Color(0xFF131416),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        UsernameCheckState.Available -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Available",
                                tint = Color(0xFF0F8A4B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        is UsernameCheckState.Unavailable -> {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Unavailable",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        else -> { /* Idle */ }
                    }
                }
            }

            // Status Message Display
            Spacer(modifier = Modifier.height(6.dp))
            when (val state = checkState) {
                UsernameCheckState.Available -> {
                    Text(
                        text = "Username is available!",
                        color = PrimaryGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                is UsernameCheckState.Unavailable -> {
                    Text(
                        text = state.message,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                else -> {
                    Text(
                        text = "3-20 characters: letters, numbers, _, .",
                        color = TextWhite.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cancel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF242528))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val isAvailable = checkState == UsernameCheckState.Available
                val isChanged = inputUsername.trim().lowercase() != currentUsername.trim().lowercase().removePrefix("@")
                val canSave = isAvailable && isChanged && !isSaving

                // Save
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (canSave) PrimaryGreen else PrimaryGreen.copy(alpha = 0.25f))
                        .clickable(enabled = canSave) {
                            isSaving = true
                            viewModel.updateUsername(
                                newUsername = inputUsername.trim(),
                                onSuccess = {
                                    isSaving = false
                                    Toast.makeText(context, "Username updated!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                onError = { errorMsg ->
                                    isSaving = false
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "Save",
                            color = if (canSave) Color.Black else Color.Black.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}