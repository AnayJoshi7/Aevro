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
import androidx.compose.ui.draw.drawBehind
import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.nativeCanvas


import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color



val CardBackground = Color(0x1A3ddc84)
val PrimaryGreen = Color(0xFF3DDC84)
val gblue = Color(0xFF0673CB)
val royalblue = Color(0xFF014BAA)
val lightcream = Color(0xFFF8F3F0)


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
            0.00f to Color(0xFF030906),
            0.52f to Color(0xFF030906), // 52% stop from Figma
            0.92f to Color(0xFF1F2622), // 92% stop from Figma
            1.00f to Color(0xFF1F2622)
        ),
        start = Offset(x = 0f, y = 0f),                         // Top-Left origin
        end = Offset(x = size.width * 0.90f, y = size.height)   // Diagonally toward bottom-right
    )
    drawRect(brush = gradientBrush)
}
fun Modifier.figmaDropShadow(
    color: Color = Color(0xFF3DDC84).copy(alpha = 0.15f), // Color: 3DDC84 15%
    blurRadius: Dp = 40.dp,                               // Blur: 40
    offsetX: Dp = 0.dp,                                   // X: 0
    offsetY: Dp = 16.dp,                                  // Y: 16
    borderRadius: Dp = 24.dp                              // Card corner radius
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val blurRadiusPx = blurRadius.toPx()
        val paint = Paint().asFrameworkPaint().apply {
            this.color = color.toArgb()
            if (blurRadiusPx > 0f) {
                // Creates the exact feathered gaussian blur from Figma
                maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
            }
        }

        val left = offsetX.toPx()
        val top = offsetY.toPx()
        val right = size.width + left
        val bottom = size.height + top
        val radiusPx = borderRadius.toPx()

        canvas.nativeCanvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radiusPx,
            radiusPx,
            paint
        )
    }
}