package com.anay.fitnesstracker.Screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import com.anay.fitnesstracker.ui.theme.glassEffect
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

private val PrimaryGreen = Color(0xFF3DDC84)
private val TextWhite = Color.White
private val TextMuted = Color(0xFF8E9094)
private val GraphRed = Color(0xFFFF3B30)
private val AxisColor = PrimaryGreen.copy(alpha = 0.50f)

enum class TimeframeSelection {
    THIS_WEEK, THIS_MONTH
}

@Composable
fun ProgressScreen(
    onNavigate: (String) -> Unit,
    fitnessViewModel: FitnessViewModel
) {
    val hazeState = rememberHazeState()

    val user by fitnessViewModel.currentUser.collectAsState()
    val loggedWorkouts = user?.loggedWorkouts ?: emptyList()
    val meals = user?.meals ?: emptyList()
    val waterLogs = user?.waterLogs ?: emptyList()
    val weightLogs = user?.weightLogs ?: emptyList()

    var selectedTimeframe by remember { mutableStateOf(TimeframeSelection.THIS_WEEK) }
    var dropdownOpen by remember { mutableStateOf(false) }

    val systemZone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }

    // Start of Week (Monday 00:00)
    val startOfWeekMillis = remember(today) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(systemZone)
            .toInstant()
            .toEpochMilli()
    }

    // Start of Month (1st of month 00:00)
    val startOfMonthMillis = remember(today) {
        today.withDayOfMonth(1)
            .atStartOfDay(systemZone)
            .toInstant()
            .toEpochMilli()
    }

    // ----------------- Aggregations -----------------
    val xLabels = remember(selectedTimeframe, today) {
        if (selectedTimeframe == TimeframeSelection.THIS_WEEK) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        } else {
            val monthAbbr = today.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            listOf("W1", "W2", "W3", "W4", "$monthAbbr end")
        }
    }

    // 1. Workouts Data
    val workoutPoints = remember(loggedWorkouts, selectedTimeframe, startOfWeekMillis, startOfMonthMillis) {
        if (selectedTimeframe == TimeframeSelection.THIS_WEEK) {
            val counts = FloatArray(7) { 0f }
            loggedWorkouts.filter { it.timestamp >= startOfWeekMillis }.forEach { item ->
                val day = Instant.ofEpochMilli(item.timestamp).atZone(systemZone).get(ChronoField.DAY_OF_WEEK) // 1..7
                if (day in 1..7) counts[day - 1] += 1f
            }
            counts.toList()
        } else {
            val counts = FloatArray(5) { 0f }
            loggedWorkouts.filter { it.timestamp >= startOfMonthMillis }.forEach { item ->
                val dayOfMonth = Instant.ofEpochMilli(item.timestamp).atZone(systemZone).dayOfMonth
                val bucket = ((dayOfMonth - 1) / 7).coerceIn(0, 4)
                counts[bucket] += 1f
            }
            counts.toList()
        }
    }

    // 2. Calories Data
    val caloriePoints = remember(meals, selectedTimeframe, startOfWeekMillis, startOfMonthMillis) {
        if (selectedTimeframe == TimeframeSelection.THIS_WEEK) {
            val sums = FloatArray(7) { 0f }
            meals.filter { it.timestamp >= startOfWeekMillis }.forEach { item ->
                val day = Instant.ofEpochMilli(item.timestamp).atZone(systemZone).get(ChronoField.DAY_OF_WEEK)
                if (day in 1..7) sums[day - 1] += item.calories.toFloat()
            }
            sums.toList()
        } else {
            val sums = FloatArray(5) { 0f }
            meals.filter { it.timestamp >= startOfMonthMillis }.forEach { item ->
                val dayOfMonth = Instant.ofEpochMilli(item.timestamp).atZone(systemZone).dayOfMonth
                val bucket = ((dayOfMonth - 1) / 7).coerceIn(0, 4)
                sums[bucket] += item.calories.toFloat()
            }
            sums.toList()
        }
    }

    // 3. Water Intake Data (Liters)
    val waterPoints = remember(waterLogs, selectedTimeframe, startOfWeekMillis, startOfMonthMillis) {
        if (selectedTimeframe == TimeframeSelection.THIS_WEEK) {
            val sums = FloatArray(7) { 0f }
            waterLogs.filter { it.timestamp >= startOfWeekMillis }.forEach { item ->
                val day = Instant.ofEpochMilli(item.timestamp).atZone(systemZone).get(ChronoField.DAY_OF_WEEK)
                if (day in 1..7) sums[day - 1] += (item.amountMl / 1000f)
            }
            sums.toList()
        } else {
            val sums = FloatArray(5) { 0f }
            waterLogs.filter { it.timestamp >= startOfMonthMillis }.forEach { item ->
                val dayOfMonth = Instant.ofEpochMilli(item.timestamp).atZone(systemZone).dayOfMonth
                val bucket = ((dayOfMonth - 1) / 7).coerceIn(0, 4)
                sums[bucket] += (item.amountMl / 1000f)
            }
            sums.toList()
        }
    }

    // 4. Weight Data (Kg)
    val fallbackWeight = user?.weightKg?.toFloat() ?: 70f
    val weightPoints = remember(weightLogs, selectedTimeframe, startOfWeekMillis, startOfMonthMillis, fallbackWeight) {
        if (selectedTimeframe == TimeframeSelection.THIS_WEEK) {
            val weekWeights = FloatArray(7) { fallbackWeight }
            weightLogs.filter { it.timestamp >= startOfWeekMillis }.forEach { item ->
                val day = Instant.ofEpochMilli(item.timestamp).atZone(systemZone).get(ChronoField.DAY_OF_WEEK)
                if (day in 1..7) weekWeights[day - 1] = item.weightKg.toFloat()
            }
            weekWeights.toList()
        } else {
            val monthWeights = FloatArray(5) { fallbackWeight }
            weightLogs.filter { it.timestamp >= startOfMonthMillis }.forEach { item ->
                val dayOfMonth = Instant.ofEpochMilli(item.timestamp).atZone(systemZone).dayOfMonth
                val bucket = ((dayOfMonth - 1) / 7).coerceIn(0, 4)
                monthWeights[bucket] = item.weightKg.toFloat()
            }
            monthWeights.toList()
        }
    }

    // 5. Steps Data
    val stepsPoints = remember(selectedTimeframe) {
        if (selectedTimeframe == TimeframeSelection.THIS_WEEK) {
            listOf(4200f, 6800f, 9400f, 8100f, 10200f, 7500f, 5800f)
        } else {
            listOf(48000f, 53000f, 61000f, 57000f, 49000f)
        }
    }

    // Averaging metrics (Whole number for Workouts)
    val avgWorkouts = workoutPoints.average().roundToInt()
    val avgCalories = caloriePoints.average().roundToInt()
    val avgWater = String.format(Locale.US, "%.1f", waterPoints.average().coerceAtLeast(0.0))
    val avgWeight = String.format(Locale.US, "%.1f", weightPoints.average().coerceAtLeast(0.0))
    val avgSteps = stepsPoints.average().roundToInt()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .appBackgroundGradient()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Progress",
                    color = PrimaryGreen,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Timeframe Selector Pill (This Week / This Month)
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { dropdownOpen = true }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (selectedTimeframe == TimeframeSelection.THIS_WEEK) "This Week" else "This Month",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Timeframe",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownOpen,
                        onDismissRequest = { dropdownOpen = false },
                        modifier = Modifier.background(Color(0xFF1E2022))
                    ) {
                        DropdownMenuItem(
                            text = { Text("This Week", color = TextWhite, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                selectedTimeframe = TimeframeSelection.THIS_WEEK
                                dropdownOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("This Month", color = TextWhite, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                selectedTimeframe = TimeframeSelection.THIS_MONTH
                                dropdownOpen = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 1. Workouts Graph
                ProgressMetricSection(
                    title = "Workouts :",
                    avgLabel = "Avg: $avgWorkouts / day",
                    points = workoutPoints,
                    labels = xLabels,
                    hazeState = hazeState
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Calories Graph
                ProgressMetricSection(
                    title = "Calories :",
                    avgLabel = "Avg: $avgCalories kcal",
                    points = caloriePoints,
                    labels = xLabels,
                    hazeState = hazeState
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Water Intake Graph
                ProgressMetricSection(
                    title = "Water Intake :",
                    avgLabel = "Avg: $avgWater L",
                    points = waterPoints,
                    labels = xLabels,
                    hazeState = hazeState
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Weight Graph
                ProgressMetricSection(
                    title = "Weight :",
                    avgLabel = "Avg: $avgWeight kg",
                    points = weightPoints,
                    labels = xLabels,
                    hazeState = hazeState
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Steps Graph
                ProgressMetricSection(
                    title = "Steps :",
                    avgLabel = "Avg: $avgSteps steps",
                    points = stepsPoints,
                    labels = xLabels,
                    hazeState = hazeState
                )

                Spacer(modifier = Modifier.height(30.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    color = Color(0xFF8E9094),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

@Composable
private fun ProgressMetricSection(
    title: String,
    avgLabel: String,
    points: List<Float>,
    labels: List<String>,
    hazeState: HazeState
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = avgLabel,
                color = PrimaryGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // GlassCard matching your original screen layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassEffect(
                    hazeState = hazeState,
                    blurRadius = 16.dp,
                    shadowEnabled = true
                )
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val bottomMargin = 28.dp.toPx()
                    val leftMargin = 22.dp.toPx()
                    val rightMargin = 12.dp.toPx()
                    val topMargin = 14.dp.toPx()

                    val chartWidth = w - leftMargin - rightMargin
                    val chartHeight = h - topMargin - bottomMargin

                    // Draw Green Axes
                    drawLine(
                        color = AxisColor,
                        start = Offset(leftMargin, topMargin),
                        end = Offset(leftMargin, h - bottomMargin),
                        strokeWidth = 2.dp.toPx()
                    )

                    drawLine(
                        color = AxisColor,
                        start = Offset(leftMargin, h - bottomMargin),
                        end = Offset(w - rightMargin, h - bottomMargin),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Draw Red Trend Line
                    if (points.isNotEmpty()) {
                        val minVal = points.minOrNull() ?: 0f
                        val maxVal = points.maxOrNull() ?: 1f
                        val range = if (maxVal == minVal) 1f else (maxVal - minVal)

                        val path = Path()
                        val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                        points.forEachIndexed { index, value ->
                            val x = leftMargin + (index * stepX)
                            val normalized = ((value - minVal) / range).coerceIn(0.05f, 0.95f)
                            val y = (h - bottomMargin) - (normalized * chartHeight)

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = GraphRed,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Render Clean Text Labels on X-Axis
                    val labelCount = labels.size
                    val stepLabelX = chartWidth / (labelCount - 1).coerceAtLeast(1)

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#8E9094")
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    labels.forEachIndexed { i, labelText ->
                        val x = leftMargin + (i * stepLabelX)
                        val y = h - 6.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            x,
                            y,
                            textPaint
                        )
                    }
                }
            }
        }
    }
}