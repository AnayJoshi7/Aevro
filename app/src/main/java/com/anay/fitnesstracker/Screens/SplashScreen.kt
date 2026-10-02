package com.anay.fitnesstracker.Screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient

private val CardBackground = Color(0xFF000000)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF8E8E93)

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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // App Logo
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Aevro Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(26.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Main Tagline
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Train.",
                    color = TextWhite,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Fuel.",
                    color = PrimaryGreen,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Progress.",
                    color = TextWhite,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle
            Text(
                text = "Your journey to peak performance starts here.",
                color = TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Login Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(CardBackground)
                    .padding(horizontal = 18.dp, vertical = 22.dp)
            ) {
                // Typable Username Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (username.isEmpty()) {
                        Text(
                            text = "Enter Username",
                            color = Color(0xFF8A8A8E),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    BasicTextField(
                        value = username,
                        onValueChange = { username = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Forgot Username Link
                Text(
                    text = "Forgot Username?",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .clickable {
                            Toast.makeText(
                                context,
                                "Username format: INITIALS + YEAR + LAST 4 DIGITS OF PHONE",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Log In Button
                Button(
                    onClick = {
                        if (username.isBlank()) {
                            Toast.makeText(context, "Enter your unique username", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        viewModel.login(
                            username = username.trim(),
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
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Log In",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // NEW? Section Header
            Text(
                text = "NEW?",
                color = PrimaryGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sign Up Button Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBackground)
                    .clickable { onNavigateSignUp() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Click here to sign-up",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Bottom Graphic (Shoe, Kettlebell, Muscle Arm)
            Image(
                painter = painterResource(id = R.drawable.ic_splash),
                contentDescription = "Fitness Icons",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
