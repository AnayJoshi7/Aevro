package com.anay.fitnesstracker.components

import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader as AndroidShader
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.Routes
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val NavBlue = Color(0xFF0673CB)
private val NavShape = RoundedCornerShape(28.dp)

/*
 * Figma gradient transforms, preserved rather than replaced with
 * generic diagonal gradients.
 */
private val BarStroke = FigmaGradientBrush(
    color = Color.White,
    edgeAlpha = 13f / 255f,
    transform = floatArrayOf(
        0.94304037f, -0.056959637f, 0.056959637f,
        0.94304037f,  0.94304037f, -0.44304037f,
        0f,          0f,           1f
    )
)

private val PillStroke = FigmaGradientBrush(
    color = NavBlue,
    edgeAlpha = 0.05f,
    transform = floatArrayOf(
        -0.6037535f,  0.3962465f, 0.6037535f,
        -0.3962465f, -0.3962465f, 0.8962465f,
        0f,          0f,         1f
    )
)

private data class NavItemConfig(
    val route: String,
    @DrawableRes val iconRes: Int,
    val label: String,
    val iconWidth: Dp = 22.dp
)

private val navItems = listOf(
    NavItemConfig(
        route = Routes.DASHBOARD,
        iconRes = R.drawable.ic_home,
        label = "Home"
    ),
    NavItemConfig(
        route = Routes.WORKOUTS,
        iconRes = R.drawable.ic_dumbbell,
        label = "Workouts",
        iconWidth = 36.dp
    ),
    NavItemConfig(
        route = Routes.PROGRESS,
        iconRes = R.drawable.ic_progress,
        label = "Progress"
    ),
    NavItemConfig(
        route = Routes.PROFILE,
        iconRes = R.drawable.ic_profile,
        label = "Profile"
    )
)

@Composable
fun AppBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    fontFamily: FontFamily? =
        MaterialTheme.typography.labelMedium.fontFamily
) {
    val selectedIndex = navItems.indexOfFirst {
        it.route == currentRoute
    }

    val barStyle = remember {
        navigationBlurStyle(
            tint = Color.White.copy(alpha = 0.075f)
        )
    }

    val pillStyle = remember {
        navigationBlurStyle(
            tint = NavBlue.copy(alpha = 0.10f)
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .navigationGlass(
                hazeState = hazeState,
                style = barStyle,
                highlight = Color.White.copy(alpha = 0.40f),
                stroke = BarStroke
            )
            .selectableGroup()
    ) {

        val horizontalScale = maxWidth / 358.dp

        if (selectedIndex >= 0) {
            val pillX = when (selectedIndex) {
                0 -> 7.dp
                1 -> 91.dp
                2 -> 179.dp
                else -> 264.dp
            }

            // The Profile state is authored at y = 8, not y = 7.
            val pillY = if (selectedIndex == 3) 8.dp else 7.dp

            Box(
                modifier = Modifier
                    .offset(
                        x = pillX * horizontalScale,
                        y = pillY
                    )
                    .size(
                        width = 85.dp * horizontalScale,
                        height = 74.dp
                    )
                    .navigationGlass(
                        hazeState = hazeState,
                        style = pillStyle,
                        highlight = NavBlue.copy(alpha = 0.40f),
                        stroke = PillStroke
                    )
            )
        }

        navItems.forEachIndexed { index, item ->
            val selected = index == selectedIndex

            val itemX = when (index) {
                0 -> 7.dp
                1 -> if (selectedIndex == 1) 94.dp else 97.dp
                2 -> 182.dp
                else -> 267.dp
            }

            val itemY = when {
                index == 0 -> 7.dp
                index == 1 && selectedIndex == 1 -> 10.dp
                else -> 12.dp
            }

            val itemWidth = if (index == 0) 85.dp else 79.dp
            val itemHeight = if (index == 0) 74.dp else 64.dp


            val contentColor =
                if (index == 0 && selected) NavBlue else Color.White

            BottomNavItem(
                item = item,
                selected = selected,
                contentColor = contentColor,
                fontFamily = fontFamily,
                onClick = { onNavigate(item.route) },
                modifier = Modifier
                    .offset(
                        x = itemX * horizontalScale,
                        y = itemY
                    )
                    .size(
                        width = itemWidth * horizontalScale,
                        height = itemHeight
                    )
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    item: NavItemConfig,
    selected: Boolean,
    contentColor: Color,
    fontFamily: FontFamily?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(NavShape)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 5.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(
                width = item.iconWidth,
                height = 22.dp
            )
        )

        Text(
            text = item.label,
            color = contentColor,
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun navigationBlurStyle(tint: Color) = HazeStyle(
    backgroundColor = Color.Transparent,
    blurRadius = 40.dp,
    noiseFactor = 0f,
    tints = listOf(HazeTint(tint))
)

private fun Modifier.navigationGlass(
    hazeState: HazeState,
    style: HazeStyle,
    highlight: Color,
    stroke: Brush
): Modifier = this
    .clip(NavShape)
    .hazeEffect(
        state = hazeState,
        style = style
    )
    .innerShadow(
        shape = NavShape,
        shadow = Shadow(
            radius = 4.dp,
            spread = 0.dp,
            color = highlight,
            offset = DpOffset(x = 0.dp, y = 2.dp)
        )
    )
    .innerShadow(
        shape = NavShape,
        shadow = Shadow(
            radius = 4.dp,
            spread = 0.dp,
            color = Color.Black.copy(alpha = 0.25f),
            offset = DpOffset(x = 0.dp, y = (-2).dp)
        )
    )
    .insideGradientStroke(
        brush = stroke,
        width = 0.5.dp,
        radius = 28.dp
    )

/**
 * Draws the full stroke inside the component bounds.
 * It does not reduce the content area or change its position.
 */
private fun Modifier.insideGradientStroke(
    brush: Brush,
    width: Dp,
    radius: Dp
): Modifier = drawWithCache {
    val strokeWidth = width.toPx()
    val inset = strokeWidth / 2f
    val corner = (radius.toPx() - inset).coerceAtLeast(0f)

    onDrawWithContent {
        drawContent()

        drawRoundRect(
            brush = brush,
            topLeft = Offset(inset, inset),
            size = Size(
                width = (size.width - strokeWidth).coerceAtLeast(0f),
                height = (size.height - strokeWidth).coerceAtLeast(0f)
            ),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )
    }
}

/**
 * Converts Figma's normalized gradient transform to Android shader
 * coordinates, including non-square bounds.
 */
private class FigmaGradientBrush(
    private val color: Color,
    private val edgeAlpha: Float,
    transform: FloatArray
) : ShaderBrush() {

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
            intArrayOf(
                color.copy(alpha = edgeAlpha).toArgb(),
                color.toArgb(),
                color.copy(alpha = edgeAlpha).toArgb()
            ),
            floatArrayOf(0f, 0.5f, 1f),
            AndroidShader.TileMode.CLAMP
        ).apply {
            setLocalMatrix(shaderMatrix)
        }
    }
}
