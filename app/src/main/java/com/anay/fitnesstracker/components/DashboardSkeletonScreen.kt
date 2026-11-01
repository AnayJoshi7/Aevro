package com.anay.fitnesstracker.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient

@Composable
fun DashboardSkeletonScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting shimmer
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Section header shimmer
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect()
        )

        // Today's workout card shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(22.dp))
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Progress section header shimmer
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect()
        )

        // Workouts completed card shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(22.dp))
                .shimmerEffect()
        )

        // Calories card shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(22.dp))
                .shimmerEffect()
        )

        // Steps card shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(22.dp))
                .shimmerEffect()
        )
    }
}