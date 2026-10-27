package com.anay.fitnesstracker.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.Routes
import com.anay.fitnesstracker.ui.theme.glassEffect
import dev.chrisbanes.haze.HazeState

private val NavSurfaceTint = Color(0xFF1F2622)
private val PrimaryGreen = Color(0xFF3DDC84)
private val SelectedContent = Color(0xFF030906)
private val UnselectedContent = Color.White
private val UnselectedPill = PrimaryGreen.copy(alpha = 0.051f)
private val SelectedPillBorder = Color.White.copy(alpha = 0.18f)

private data class NavItemConfig(
    val route: String,
    @DrawableRes val iconRes: Int,
    val label: String,
    val wideIcon: Boolean = false
)

private val navItems = listOf(
    NavItemConfig(Routes.DASHBOARD, R.drawable.ic_home, "Home"),
    NavItemConfig(Routes.WORKOUTS, R.drawable.ic_dumbbell, "Workouts", wideIcon = true),
    NavItemConfig(Routes.PROGRESS, R.drawable.ic_progress, "Progress"),
    NavItemConfig(Routes.PROFILE, R.drawable.ic_profile, "Profile")
)

@Composable
fun AppBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    fontFamily: FontFamily? = MaterialTheme.typography.labelMedium.fontFamily
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .glassEffect(
                hazeState = hazeState,
                blurRadius = 24.dp,
                cornerRadius = 28.dp,
                tint = NavSurfaceTint,
                shadowEnabled = true
            )
            .selectableGroup()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val isSelected = currentRoute == item.route
            BottomNavItem(
                iconRes = item.iconRes,
                label = item.label,
                selected = isSelected,
                fontFamily = fontFamily,
                wideIcon = item.wideIcon,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    @DrawableRes iconRes: Int,
    label: String,
    selected: Boolean,
    fontFamily: FontFamily?,
    wideIcon: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    // Smooth color transfer between pills
    val animatedBgColor by animateColorAsState(
        targetValue = if (selected) PrimaryGreen else UnselectedPill,
        animationSpec = tween(durationMillis = 260),
        label = "bgColor"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) SelectedPillBorder else Color.Transparent,
        animationSpec = tween(durationMillis = 260),
        label = "borderColor"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (selected) SelectedContent else UnselectedContent,
        animationSpec = tween(durationMillis = 220),
        label = "contentColor"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconScale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shape)
            .background(animatedBgColor)
            .border(1.dp, animatedBorderColor, shape)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = animatedContentColor,
            modifier = Modifier
                .scale(animatedScale)
                .size(
                    width = if (wideIcon) 34.dp else 22.dp,
                    height = 22.dp
                )
        )

        Text(
            text = label,
            color = animatedContentColor,
            fontFamily = fontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}