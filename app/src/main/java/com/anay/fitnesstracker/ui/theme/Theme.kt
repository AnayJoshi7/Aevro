package com.anay.fitnesstracker.ui.theme

import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader as AndroidShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

// ---------- Shared colors ----------

// Existing names retained for compatibility.
val PrimaryGreen = Color(0xFF0673CB)
val SelectedGreen = Color(0xFF2EAA68)

val TextWhite = Color.White
val TextMuted = Color(0xFF94A3B8)

val CardBackground = Color(0xFFFFFFFF).copy(alpha = 0.075f)
val InputFieldBg = Color.White.copy(alpha = 0.08f)

val gblue = Color(0xFF0673CB)
val royalblue = Color(0xFF014BAA)
val lightcream = Color(0xFFF8F3F0)

val ScreenBackgroundTop = Color.Black

// Preserve the reference's float RGB values.
val ScreenBackgroundMiddle = Color(0xFF090D11)

val ScreenBackgroundBottom = Color(0xFF242424)

val ActionBackground = Color.Black
val ProgressTrackBackground = Color.Black
val ProgressTrackOutline = Color.White.copy(alpha = 0.18f)
val BadgeOutline = gblue.copy(alpha = 0.20f)
val DropdownIndicator = Color(0xFFD9D9D9)

// Retained for existing references.
// No additional glow is applied automatically.
val BackgroundGlowColor = gblue.copy(alpha = 0.15f)

// ---------- Reusable glass tokens ----------

object GlassDefaults {
    val CornerRadius = 28.dp
    val BorderWidth = 0.5.dp
    val WeightCardBorderWidth = 1.dp
    val ContentPadding = PaddingValues(20.dp)

    val SurfaceTint = CardBackground
    val SelectedPillTint = gblue.copy(alpha = 0.10f)

    val BorderHighlight = Color.White.copy(alpha = 0.05f)
    val BorderMiddle = Color.White
    val BorderBottom = Color.White.copy(alpha = 0.05f)
    val WorkoutBorderBottom = Color.White.copy(alpha = 0.06f)

    val BackgroundBlurRadius = 40.dp

    val InnerHighlight = Color.White.copy(alpha = 0.40f)
    val InnerShade = Color.Black.copy(alpha = 0.25f)
    val WorkoutInnerShade = Color.Black.copy(alpha = 0.20f)
    val SelectedPillInnerHighlight = gblue.copy(alpha = 0.40f)

    val InnerShadowRadius = 4.dp
    val InnerHighlightOffsetY = 2.dp
    val InnerShadeOffsetY = (-2).dp
    val InnerShadowSpread = 0.dp

    // The reference cards have no outer drop shadows.
    // Retained so existing callers still compile.
    val ShadowColor = Color.Transparent
    val ShadowRadius = 24.dp
    val ShadowOffsetY = 12.dp

    val BackgroundGlowBlurRadius = 120.dp

    val HeadingShadowColor = Color(0x40000000)
    val HeadingShadowRadius = 12.dp
    val HeadingShadowOffsetX = 0.dp
    val HeadingShadowOffsetY = 4.dp
}

// ---------- Exact reference gradient brushes ----------

object GlassBrushes {

    val ScreenBackground: Brush = FigmaLinearGradientBrush(
        colors = listOf(
            ScreenBackgroundTop,
            ScreenBackgroundMiddle,
            ScreenBackgroundBottom
        ),
        transform = floatArrayOf(
            0.09143556f, 0.9054094f, 0f,
            -0.9054094f,  0.8883870f, 0.5f,
            0f,          0f,         1f
        )
    )

    // Steps, Water Intake and Weight share this transform.
    val Standard: Brush = whiteBorder(
        transform = floatArrayOf(
            -0.8229905f,  0.17700952f, 0.8229905f,
            -0.17700951f, -0.17700951f, 0.6770095f,
            0f,           0f,          1f
        )
    )

    val Progress: Brush = whiteBorder(
        transform = floatArrayOf(
            -0.8974873f,  0.20253198f, 0.8320635f,
            -0.20253198f, -0.20033893f, 0.6945607f,
            0f,           0f,          1f
        )
    )

    val Calories: Brush = whiteBorder(
        transform = floatArrayOf(
            -0.83542305f,  0.18394904f, 0.81605095f,
            -0.18394902f, -0.17968352f, 0.6796835f,
            0f,           0f,          1f
        )
    )

    val Workout: Brush = whiteBorder(
        endAlpha = 0.06f,
        transform = floatArrayOf(
            -0.8445742f,  0.15542577f, 0.8445742f,
            -0.15542577f, -0.15542577f, 0.6554258f,
            0f,           0f,          1f
        )
    )

    val Navigation: Brush = whiteBorder(
        startAlpha = 13f / 255f,
        endAlpha = 13f / 255f,
        transform = floatArrayOf(
            0.94304037f, -0.056959637f, 0.056959637f,
            0.94304037f,  0.94304037f, -0.44304037f,
            0f,           0f,           1f
        )
    )

    val SelectedNavigation: Brush = FigmaLinearGradientBrush(
        colors = listOf(
            gblue.copy(alpha = 0.05f),
            gblue,
            gblue.copy(alpha = 0.05f)
        ),
        transform = floatArrayOf(
            -0.6037535f,  0.3962465f, 0.6037535f,
            -0.3962465f, -0.3962465f, 0.8962465f,
            0f,          0f,         1f
        )
    )

    private fun whiteBorder(
        transform: FloatArray,
        startAlpha: Float = 0.05f,
        endAlpha: Float = 0.05f
    ): Brush = FigmaLinearGradientBrush(
        colors = listOf(
            Color.White.copy(alpha = startAlpha),
            Color.White,
            Color.White.copy(alpha = endAlpha)
        ),
        transform = transform
    )
}


private class FigmaLinearGradientBrush(
    colors: List<Color>,
    transform: FloatArray
) : ShaderBrush() {

    private val gradientColors = colors.map { it.toArgb() }.toIntArray()
    private val gradientTransform = transform.copyOf()

    override fun createShader(size: Size): Shader {
        val figmaMatrix = Matrix().apply {
            setValues(gradientTransform)
        }

        val shaderMatrix = Matrix()
        check(figmaMatrix.invert(shaderMatrix)) {
            "Gradient transform must be invertible."
        }

        shaderMatrix.postScale(size.width, size.height)

        return LinearGradient(
            0f,
            0.5f,
            1f,
            0.5f,
            gradientColors,
            floatArrayOf(0f, 0.5f, 1f),
            AndroidShader.TileMode.CLAMP
        ).apply {
            setLocalMatrix(shaderMatrix)
        }
    }
}

// ---------- Material theme ----------

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = ActionBackground,

    secondary = gblue,
    onSecondary = ActionBackground,

    tertiary = gblue,
    onTertiary = ActionBackground,

    background = ScreenBackgroundTop,
    onBackground = TextWhite,

    surface = CardBackground,
    onSurface = TextWhite,

    surfaceVariant = CardBackground,
    onSurfaceVariant = TextMuted,

    outline = ProgressTrackOutline
)

@Composable
fun FitnessTrackerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Retains the existing dark-only behavior.
    val colorScheme = if (
        darkTheme &&
        dynamicColor &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides
                    MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = SfProRounded
                    )
        ) {
            content()
        }
    }
}

// ---------- Screen background ----------

fun Modifier.appBackgroundGradient(): Modifier =
    drawBehind {
        drawRect(brush = GlassBrushes.ScreenBackground)
    }

// ---------- General-purpose gradient border ----------

fun Modifier.borderGradient(
    width: Dp = GlassDefaults.BorderWidth,
    shape: Shape = RectangleShape,
    brush: Brush = GlassBrushes.Standard
): Modifier = border(
    width = width,
    brush = brush,
    shape = shape
)

// ---------- Exact inside stroke for rounded glass ----------

private fun Modifier.insideGlassStroke(
    brush: Brush,
    width: Dp,
    cornerRadius: Dp
): Modifier {
    if (width <= 0.dp) return this

    return drawWithCache {
        val strokeWidth = width.toPx()
        val inset = strokeWidth / 2f
        val radius = (cornerRadius.toPx() - inset).coerceAtLeast(0f)

        onDrawWithContent {
            drawContent()

            if (size.width > strokeWidth && size.height > strokeWidth) {
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(inset, inset),
                    size = Size(
                        width = size.width - strokeWidth,
                        height = size.height - strokeWidth
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

// ---------- Optional outer shadow ----------

fun Modifier.figmaDropShadow(
    color: Color = GlassDefaults.ShadowColor,
    blurRadius: Dp = GlassDefaults.ShadowRadius,
    offsetX: Dp = 0.dp,
    offsetY: Dp = GlassDefaults.ShadowOffsetY,
    borderRadius: Dp = GlassDefaults.CornerRadius
): Modifier {
    if (color.alpha == 0f) return this

    return dropShadow(
        shape = RoundedCornerShape(borderRadius),
        shadow = Shadow(
            radius = blurRadius,
            spread = 0.dp,
            color = color,
            offset = DpOffset(offsetX, offsetY)
        )
    )
}

// ---------- Shared inner shadows and stroke ----------

private fun Modifier.glassEdges(
    cornerRadius: Dp,
    borderWidth: Dp,
    borderBrush: Brush,
    innerHighlight: Color,
    innerShade: Color
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        .innerShadow(
            shape = shape,
            shadow = Shadow(
                radius = GlassDefaults.InnerShadowRadius,
                spread = GlassDefaults.InnerShadowSpread,
                color = innerHighlight,
                offset = DpOffset(
                    x = 0.dp,
                    y = GlassDefaults.InnerHighlightOffsetY
                )
            )
        )
        .innerShadow(
            shape = shape,
            shadow = Shadow(
                radius = GlassDefaults.InnerShadowRadius,
                spread = GlassDefaults.InnerShadowSpread,
                color = innerShade,
                offset = DpOffset(
                    x = 0.dp,
                    y = GlassDefaults.InnerShadeOffsetY
                )
            )
        )
        .insideGlassStroke(
            brush = borderBrush,
            width = borderWidth,
            cornerRadius = cornerRadius
        )
}

// ---------- Glass with background blur ----------

fun Modifier.glassEffect(
    hazeState: HazeState? = null,
    blurRadius: Dp = GlassDefaults.BackgroundBlurRadius,
    cornerRadius: Dp = GlassDefaults.CornerRadius,
    tint: Color = GlassDefaults.SurfaceTint,
    shadowEnabled: Boolean = true,
    shadowColor: Color = GlassDefaults.ShadowColor,
    borderWidth: Dp = GlassDefaults.BorderWidth,
    borderBrush: Brush = GlassBrushes.Standard,
    innerHighlight: Color = GlassDefaults.InnerHighlight,
    innerShade: Color = GlassDefaults.InnerShade
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    val outerShadow = if (shadowEnabled) {
        Modifier.figmaDropShadow(
            color = shadowColor,
            borderRadius = cornerRadius
        )
    } else {
        Modifier
    }

    val backdrop = if (hazeState != null) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = Color.Transparent,
                blurRadius = blurRadius,
                noiseFactor = 0f,
                tints = emptyList()
            )
        )
    } else {
        Modifier
    }

    return this
        .then(outerShadow)
        .clip(shape)
        .then(backdrop)
        // Draw the fill independently of Haze.
        // Preserve the exact color and alpha passed by the caller.
        .background(
            color = tint,
            shape = shape
        )
        .glassEdges(
            cornerRadius = cornerRadius,
            borderWidth = borderWidth,
            borderBrush = borderBrush,
            innerHighlight = innerHighlight,
            innerShade = innerShade
        )
}


// ---------- Glass appearance without background blur ----------

fun Modifier.glassSurface(
    cornerRadius: Dp = GlassDefaults.CornerRadius,
    tint: Color = GlassDefaults.SurfaceTint,
    borderWidth: Dp = GlassDefaults.BorderWidth,
    shadowEnabled: Boolean = false,
    borderBrush: Brush = GlassBrushes.Standard,
    innerHighlight: Color = GlassDefaults.InnerHighlight,
    innerShade: Color = GlassDefaults.InnerShade,
    shadowColor: Color = GlassDefaults.ShadowColor
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    val outerShadow = if (shadowEnabled) {
        Modifier.figmaDropShadow(
            color = shadowColor,
            borderRadius = cornerRadius
        )
    } else {
        Modifier
    }

    return this
        .then(outerShadow)
        .clip(shape)
        // Preserve the caller's actual tint and alpha.
        .background(
            color = tint,
            shape = shape
        )
        .glassEdges(
            cornerRadius = cornerRadius,
            borderWidth = borderWidth,
            borderBrush = borderBrush,
            innerHighlight = innerHighlight,
            innerShade = innerShade
        )
}

// ---------- Reusable card ----------

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    blurRadius: Dp = GlassDefaults.BackgroundBlurRadius,
    cornerRadius: Dp = GlassDefaults.CornerRadius,
    contentPadding: PaddingValues = GlassDefaults.ContentPadding,
    tint: Color = GlassDefaults.SurfaceTint,
    shadowEnabled: Boolean = true,
    borderWidth: Dp = GlassDefaults.BorderWidth,
    borderBrush: Brush = GlassBrushes.Standard,
    innerHighlight: Color = GlassDefaults.InnerHighlight,
    innerShade: Color = GlassDefaults.InnerShade,
    shadowColor: Color = GlassDefaults.ShadowColor,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassEffect(
                hazeState = hazeState,
                blurRadius = blurRadius,
                cornerRadius = cornerRadius,
                tint = tint,
                shadowEnabled = shadowEnabled,
                shadowColor = shadowColor,
                borderWidth = borderWidth,
                borderBrush = borderBrush,
                innerHighlight = innerHighlight,
                innerShade = innerShade
            )
            .padding(contentPadding),
        content = content
    )
}
