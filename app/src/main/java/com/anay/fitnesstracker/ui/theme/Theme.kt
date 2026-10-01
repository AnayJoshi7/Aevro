package com.anay.fitnesstracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind



import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color



val CardBackground = Color(0xFF070708)
val PrimaryGreen = Color(0xFF27D07F)
val SelectedGreen = Color(0xFF2EAA68)
val TextWhite = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF9E9E9E)
val InputFieldBg = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun FitnessTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = DarkColorScheme, // your existing color scheme
        typography = AppTypography
    ) {
        // This ensures BasicTextField and any unstyled text automatically use SF Pro Rounded!
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(
                fontFamily = OneplusSlate
            )
        ) {
            content()
        }
    }
}
fun Modifier.appBackgroundGradient(): Modifier = this.drawBehind {
    val gradientBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF1A1A1A),
            0.52f to Color(0xFF1A1A1A), // 52% stop from Figma
            0.92f to Color(0xFF595959), // 92% stop from Figma
            1.00f to Color(0xFF595959)
        ),
        start = Offset(x = 0f, y = 0f),                         // Top-Left origin
        end = Offset(x = size.width * 0.90f, y = size.height)   // Diagonally toward bottom-right
    )
    drawRect(brush = gradientBrush)
}