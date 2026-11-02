package com.anay.fitnesstracker.data.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.anay.fitnesstracker.MainActivity
import com.anay.fitnesstracker.R
import com.anay.fitnesstracker.data.SessionManager
import com.anay.fitnesstracker.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

class TodayStatsWidget : GlanceAppWidget() {

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (steps, calories, workouts) = loadRealDashboardMetrics(context)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(24.dp)
                        .background(ColorProvider(Color(0xFF0F2217)))
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {
                        // Title Header
                        Text(
                            text = "Today’s Stats",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFE0E0E0)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Compact 3 Pods
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModernStatCircle(
                                title = "Steps",
                                iconRes = R.drawable.ic_sneakers,
                                value = steps.toString()
                            )

                            Spacer(modifier = GlanceModifier.width(10.dp))

                            ModernStatCircle(
                                title = "Calories",
                                iconRes = R.drawable.ic_apple,
                                value = calories.toString()
                            )

                            Spacer(modifier = GlanceModifier.width(10.dp))

                            ModernStatCircle(
                                title = "Workouts",
                                iconRes = R.drawable.ic_barbell,
                                value = workouts.toString()
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadRealDashboardMetrics(context: Context): Triple<Int, Int, Int> {
        val session = SessionManager(context)
        val username = session.getUsername()?.trim()?.lowercase()

        // 1. Step Count (from StepSensorManager preference storage)
        val stepPrefs = context.getSharedPreferences("step_sensor_prefs", Context.MODE_PRIVATE)
        val steps = stepPrefs.getInt("today_steps", 0)

        var calories = 0
        var workouts = 0

        // 2. Read Meals & Workouts from Firestore Local Cache
        if (!username.isNullOrBlank()) {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(username)
                    .get(Source.CACHE)
                    .await()

                if (doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    val startOfToday = LocalDate.now()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()

                    calories = profile?.meals
                        ?.filter { it.timestamp >= startOfToday }
                        ?.sumOf { it.calories } ?: 0

                    workouts = profile?.loggedWorkouts
                        ?.filter { it.timestamp >= startOfToday }
                        ?.map { it.exerciseName.trim().lowercase() }
                        ?.distinct()
                        ?.size ?: 0
                }
            } catch (_: Exception) {
                // If offline / un-cached, fallback gracefully
            }
        }

        return Triple(steps, calories, workouts)
    }
}

@SuppressLint("RestrictedApi")
@androidx.compose.runtime.Composable
private fun ModernStatCircle(
    title: String,
    iconRes: Int,
    value: String
) {
    Box(
        modifier = GlanceModifier
            .size(74.dp)
            .cornerRadius(37.dp)
            .background(ColorProvider(Color(0xFF040A07)))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxHeight()
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF9E9E9E)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Image(
                provider = ImageProvider(iconRes),
                contentDescription = title,
                modifier = GlanceModifier.size(20.dp)
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text = value,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF27D07F)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class TodayStatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayStatsWidget()
}