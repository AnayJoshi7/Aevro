package com.anay.fitnesstracker.ui.theme

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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeEffect
// ---------- Shared colors ----------

val PrimaryGreen = Color(0xFF3DDC84)
val SelectedGreen = Color(0xFF2EAA68)

val TextWhite = Color.White
val TextMuted = Color(0xFF94A3B8)

// Neutral glass rather than the old green-filled surface.
val CardBackground = Color(0x1A3DDC84)
val InputFieldBg = Color.White.copy(alpha = 0.08f)

// Retained for existing references.
val gblue = Color(0xFF0673CB)
val royalblue = Color(0xFF014BAA)
val lightcream = Color(0xFFF8F3F0)

// ---------- Reusable glass tokens ----------

object GlassDefaults {
    val CornerRadius = 28.dp
    val BorderWidth = 0.5.dp

    val ContentPadding = PaddingValues(20.dp)

    val SurfaceTint = CardBackground

    val BorderHighlight = Color.White.copy(alpha = 0.32f)
    val BorderMiddle = Color.White.copy(alpha = 0.08f)
    val BorderBottom = Color.White.copy(alpha = 0.14f)

    val ShadowColor = Color.Black.copy(alpha = 0.25f)
    val ShadowRadius = 24.dp
    val ShadowOffsetY = 12.dp

    val InnerHighlight = Color(0xFF3ddc84).copy(alpha = 0.18f)
    val InnerShade = Color.White.copy(alpha = 0.20f)
}

// ---------- Material theme ----------

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color(0xFF052E16),

    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),

    tertiary = Color(0xFFA7F3D0),
    onTertiary = Color(0xFF064E3B),

    background = Color(0xFF0B0F14),
    onBackground = TextWhite,

    surface = Color(0xFF111827),
    onSurface = TextWhite,

    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = TextMuted,

    outline = Color.White.copy(alpha = 0.18f)
)

@Composable
fun FitnessTrackerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // This design intentionally uses a dark palette.
    // A light glass theme should have its own surface/text tokens.
    val colorScheme =
        if (
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
                        fontFamily = OneplusSlate
                    )
        ) {
            content()
        }
    }
}

// ---------- Screen background ----------

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

// ---------- Glass border ----------

fun Modifier.borderGradient(
    width: Dp = 0.5.dp,
    shape: Shape = RectangleShape
): Modifier = this.border(
    width = width,
    brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF3DDC84).copy(alpha = 0.05f),
            0.50f to Color(0xFF3DDC84).copy(alpha = 1.00f),
            1.00f to Color(0xFF3DDC84).copy(alpha = 0.05f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) // or adjust direction
    ),
    shape = shape
)

// ---------- Outer shadow ----------

// Name retained for compatibility.
// This is a visual approximation, not a pixel-exact Figma conversion.
fun Modifier.figmaDropShadow(
    color: Color = GlassDefaults.ShadowColor,
    blurRadius: Dp = GlassDefaults.ShadowRadius,
    offsetX: Dp = 0.dp,
    offsetY: Dp = GlassDefaults.ShadowOffsetY,
    borderRadius: Dp = GlassDefaults.CornerRadius
): Modifier = this.dropShadow(
    shape = RoundedCornerShape(borderRadius),
    shadow = Shadow(
        radius = blurRadius,
        spread = 0.dp,
        color = color,
        offset = DpOffset(offsetX, offsetY)
    )
)

// ---------- Reusable glass surface modifier ----------

fun Modifier.glassEffect(
    hazeState: HazeState? = null,
    blurRadius: Dp = 24.dp,
    cornerRadius: Dp = GlassDefaults.CornerRadius,
    tint: Color = GlassDefaults.SurfaceTint,
    shadowEnabled: Boolean = true,
    shadowColor: Color = GlassDefaults.ShadowColor
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    val outerShadow =
        if (shadowEnabled) {
            Modifier.figmaDropShadow(
                color = shadowColor,
                blurRadius = GlassDefaults.ShadowRadius,
                offsetX = 0.dp,
                offsetY = GlassDefaults.ShadowOffsetY,
                borderRadius = cornerRadius
            )
        } else {
            Modifier
        }

    val backdrop =
        if (hazeState != null) {
            Modifier.hazeEffect(state = hazeState) {
                this.blurRadius = blurRadius
                backgroundColor = Color(0xFF0B0F14)
                tints = emptyList()
                noiseFactor = 0f
            }
        } else {
            Modifier
        }

    return this
        .then(outerShadow)
        .clip(shape)
        .then(backdrop)
        .glassSurface(
            cornerRadius = cornerRadius,
            tint = tint,
            shadowEnabled = false
        )
}


fun Modifier.glassSurface(
    cornerRadius: Dp = GlassDefaults.CornerRadius,
    tint: Color = GlassDefaults.SurfaceTint,
    borderWidth: Dp = GlassDefaults.BorderWidth,
    shadowEnabled: Boolean = true
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    val shadowModifier =
        if (shadowEnabled) {
            Modifier.figmaDropShadow(
                borderRadius = cornerRadius
            )
        } else {
            Modifier
        }

    return this
        // Shadow must be outside the surface clipping.
        .then(shadowModifier)
        .clip(shape)
        .background(tint)

        // Bright upper inner edge.
        .innerShadow(
            shape = shape,
            shadow = Shadow(
                radius = 4.dp,
                spread = 0.dp,
                color = GlassDefaults.InnerHighlight,
                offset = DpOffset(0.dp, 2.dp)
            )
        )

        // Subtle depth along the lower inner edge.
        .innerShadow(
            shape = shape,
            shadow = Shadow(
                radius = 6.dp,
                spread = 0.dp,
                color = GlassDefaults.InnerShade,
                offset = DpOffset(0.dp, (-2).dp)
            )
        )

        .borderGradient(
            width = borderWidth,
            shape = shape
        )
}

// ---------- Reusable card ----------



@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    blurRadius: Dp = 24.dp,
    cornerRadius: Dp = GlassDefaults.CornerRadius,
    contentPadding: PaddingValues = GlassDefaults.ContentPadding,
    tint: Color = GlassDefaults.SurfaceTint,
    shadowEnabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassEffect(
                hazeState = hazeState,
                blurRadius = blurRadius,
                cornerRadius = cornerRadius,
                tint = tint,
                shadowEnabled = shadowEnabled
            )
            .padding(contentPadding),
        content = content
    )
}

