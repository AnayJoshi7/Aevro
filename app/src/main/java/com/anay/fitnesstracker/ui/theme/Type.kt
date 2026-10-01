package com.anay.fitnesstracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.R

val SfProRounded = FontFamily(
    Font(R.font.sf_pro_rounded_regular, FontWeight.Normal),
    Font(R.font.sf_pro_rounded_medium, FontWeight.Medium),
    Font(R.font.sf_pro_rounded_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_rounded_bold, FontWeight.Bold)
)

val OneplusSlate = FontFamily(
    Font(R.font.oneplus_slate_regular, FontWeight.Normal),
    Font(R.font.oneplus_slate_medium, FontWeight.Medium),
    Font(R.font.oneplus_slate_semibold, FontWeight.SemiBold),
    Font(R.font.oneplus_slate_bold, FontWeight.Bold)
)

private val defaultTextStyle = TextStyle(
    //fontFamily = SfProRounded,
    fontFamily = OneplusSlate,
    fontWeight = FontWeight.Normal
)

val AppTypography = Typography(
    displayLarge = defaultTextStyle.copy(fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = defaultTextStyle.copy(fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = defaultTextStyle.copy(fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = defaultTextStyle.copy(fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = defaultTextStyle.copy(fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = defaultTextStyle.copy(fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = defaultTextStyle.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = defaultTextStyle.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = defaultTextStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    // CRITICAL FOR TEXT FIELDS:
    bodyLarge = defaultTextStyle.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = defaultTextStyle.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = defaultTextStyle.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = defaultTextStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = defaultTextStyle.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = defaultTextStyle.copy(fontSize = 11.sp, lineHeight = 16.sp)
)