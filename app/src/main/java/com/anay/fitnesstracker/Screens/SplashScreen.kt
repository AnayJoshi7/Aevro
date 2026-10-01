package com.anay.fitnesstracker.Screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.components.SplashBottomIcons
import com.anay.fitnesstracker.ui.theme.*
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.components.SplashTopIcons

@Composable
fun SplashScreen(
    viewModel: FitnessViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateSignUp: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()

            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))




        Spacer(modifier = Modifier.height(16.dp))

        SplashTopIcons()

        Spacer(modifier = Modifier.height(28.dp))



        Text(
            text = "Welcome\nFitness Freak!",
            color = PrimaryGreen,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 38.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(44.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground)
                .padding(24.dp)
        ) {
            Text(text = "Login", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(20.dp))

            TextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text("Enter Username", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = InputFieldBg,
                    unfocusedContainerColor = InputFieldBg,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (username.isBlank()) {
                        Toast.makeText(context, "Enter your unique username", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    viewModel.login(
                        username = username,
                        onSuccess = {
                            isLoading = false
                            onLoginSuccess()
                        },
                        onError = { err ->
                            isLoading = false
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                } else {
                    Text("Log In", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Forgot Username?",
                color = Color.LightGray,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    Toast.makeText(context, "Username format: INITIALS + YEAR + LAST 4 DIGITS OF PHONE", Toast.LENGTH_LONG).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "NEW?",
            color = PrimaryGreen,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = 6.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNavigateSignUp,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
            modifier = Modifier.fillMaxWidth().height(62.dp)
        ) {
            Text(text = "Click here to sign-up", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(36.dp))
        SplashBottomIcons()
        Spacer(modifier = Modifier.height(28.dp))
    }
}