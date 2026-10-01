package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.ui.theme.*
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel

@Composable
fun WorkoutSplitScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit
) {
    val highlightedSplit by viewModel.highlightedSplit.collectAsState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()

            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onBack() }
                .padding(vertical = 12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = null,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Back", color = PrimaryGreen, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SplitCard(
                title = "Push Pull Legs",
                isHighlighted = highlightedSplit == "Push Pull Legs",
                schedule = listOf(
                    "Monday - Push Day",
                    "Tuesday - Pull Day",
                    "Wednesday - Leg Day",
                    "Thursday - Push Day",
                    "Friday - Pull Day",
                    "Saturday - Leg Day"
                )
            )

            SplitCard(
                title = "Upper Body, Lower Body",
                isHighlighted = highlightedSplit == "Upper Body, Lower Body",
                schedule = listOf(
                    "Monday - Upper Body",
                    "Tuesday - Lower Body",
                    "Wednesday - Rest",
                    "Thursday - Upper Body",
                    "Friday - Lower Body"
                )
            )

            SplitCard(
                title = "Upper, Lower + Push Pull Legs",
                isHighlighted = highlightedSplit == "Upper, Lower + Push Pull Legs",
                schedule = listOf(
                    "Monday - Upper Body",
                    "Tuesday - Lower Body",
                    "Wednesday - Rest",
                    "Thursday - Push Day",
                    "Friday - Pull Day",
                    "Saturday - Leg Day"
                )
            )

            SplitCard(
                title = "Full Body",
                isHighlighted = highlightedSplit == "Full Body",
                schedule = listOf(
                    "Monday - Full Body",
                    "Tuesday - Rest",
                    "Wednesday - Full Body",
                    "Thursday - Rest",
                    "Friday - Full Body",
                    "Saturday - Cardio"
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SplitCard(
    title: String,
    isHighlighted: Boolean,
    schedule: List<String>
) {
    val bgModifier = if (isHighlighted) {
        Modifier.background(Color(0xFF3DDC84).copy(alpha = 0.25f))
    } else {
        Modifier.background(CardBackground)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .then(bgModifier)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = PrimaryGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            schedule.forEach { day ->
                Text(text = day, color = TextWhite, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = Color.White.copy(0.7f),
            modifier = Modifier.size(34.dp).padding(start = 8.dp)
        )
    }
}