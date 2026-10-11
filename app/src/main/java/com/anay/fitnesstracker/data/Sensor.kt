package com.anay.fitnesstracker.data

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class StepSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val prefs: SharedPreferences = context.getSharedPreferences("step_tracker_prefs", Context.MODE_PRIVATE)

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    val isSensorAvailable: Boolean = stepCounterSensor != null

    fun startListening() {
        stepCounterSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalRawStepsSinceBoot = event.values[0].toInt()
            val todayDateKey = "step_baseline_${LocalDate.now()}"

            // If no baseline recorded for today, record the starting step count
            if (!prefs.contains(todayDateKey)) {
                prefs.edit().putInt(todayDateKey, totalRawStepsSinceBoot).apply()
            }

            val baseline = prefs.getInt(todayDateKey, totalRawStepsSinceBoot)

            // Handle phone reboot where totalRawSteps resets below the saved baseline
            val computedSteps = if (totalRawStepsSinceBoot >= baseline) {
                totalRawStepsSinceBoot - baseline
            } else {
                prefs.edit().putInt(todayDateKey, totalRawStepsSinceBoot).apply()
                0
            }

            // Clamps steps to a maximum of 10,000
            _todaySteps.value = computedSteps.coerceIn(0, 10000)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}