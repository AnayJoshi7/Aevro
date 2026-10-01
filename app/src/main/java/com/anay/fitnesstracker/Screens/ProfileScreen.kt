package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.Routes
import com.anay.fitnesstracker.data.UserProfile
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.components.AppBottomNavigationBar
import com.anay.fitnesstracker.util.ImageUtils

private val CardBackground = Color(0xFF070708)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val AvatarColor = Color(0xFFD4D4D6)
private val SettingsItemColor = Color(0xFF5A5B5E)
private val LogoutRed = Color(0xFFE53935)

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    fitnessViewModel: FitnessViewModel,
    onLogout: () -> Unit
) {
    val user by fitnessViewModel.currentUser.collectAsState()
    val avatarBitmap = remember(user?.avatarBase64) {
        ImageUtils.base64ToBitmap(user?.avatarBase64)
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1B1C1E),
            Color(0xFF131416),
            Color(0xFF2C2E31),
            Color(0xFF111214)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
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



            // Top Header with Logout Pill on Top-Right
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Profile",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Logout Pill
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF241515))
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

            Spacer(modifier = Modifier.height(24.dp))

            // Avatar Display
            Box(
                modifier = Modifier
                    .size(105.dp)
                    .clip(CircleShape)
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = user?.name?.ifBlank { "User Profile" } ?: "User Profile",
                color = PrimaryGreen,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(22.dp))

            UserInfoCard(user = user)

            Spacer(modifier = Modifier.height(26.dp))

            SettingsCard(
                onPersonalInfoClick = { onNavigate(Routes.PERSONAL_INFO) },
                onNotificationsClick = { onNavigate(Routes.NOTIFICATIONS) },
                onAboutClick = { onNavigate(Routes.ABOUT) }
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

            Spacer(modifier = Modifier.height(14.dp))

            AppBottomNavigationBar(
                currentRoute = Routes.PROFILE,
                onNavigate = onNavigate
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun UserInfoCard(user: UserProfile?) {
    val calculatedAge = if (user != null && user.birthYear > 0) {
        (2026 - user.birthYear).coerceAtLeast(0)
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileInfoRow(label = "Age", value = calculatedAge?.let { "$it yrs" } ?: "-")
        ProfileInfoRow(label = "Height", value = user?.heightCm?.let { "${it.toInt()} cm" } ?: "-")
        ProfileInfoRow(label = "Current Weight", value = user?.weightKg?.let { "${it.toInt()} kg" } ?: "-")
        ProfileInfoRow(label = "Goal", value = user?.fitnessGoal ?: "-")
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsCard(
    onPersonalInfoClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
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
            SettingsButton(label = "Personal Information", onClick = onPersonalInfoClick)
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
            .background(SettingsItemColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

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