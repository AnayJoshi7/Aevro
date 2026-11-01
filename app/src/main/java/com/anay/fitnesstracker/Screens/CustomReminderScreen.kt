package com.anay.fitnesstracker.Screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.anay.fitnesstracker.data.ReminderSettings
import com.anay.fitnesstracker.data.reminder.ReminderScheduler
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import java.time.LocalTime
import java.util.Locale

private val CardDarkBg = Color(0xFF0F1E16)
private val CardBorderColor = Color(0xFF27D07F).copy(alpha = 0.22f)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF8E9094)
private val InnerPillBg = Color(0xFF16251E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomReminderScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val saved = user?.reminderSettings ?: ReminderSettings()

    var isWaterCardExpanded by remember { mutableStateOf(true) }
    var wakeUpTime by remember(saved.wakeUpTime) { mutableStateOf(saved.wakeUpTime) }
    var bedtime by remember(saved.bedtime) { mutableStateOf(saved.bedtime) }
    var selectedInterval by remember(saved.intervalMins) { mutableIntStateOf(saved.intervalMins) }
    var waterReminderEnabled by remember(saved.waterReminderEnabled) { mutableStateOf(saved.waterReminderEnabled) }
    var workoutReminderEnabled by remember(saved.workoutReminderEnabled) { mutableStateOf(saved.workoutReminderEnabled) }

    // Dialog pickers
    var showWakePicker by remember { mutableStateOf(false) }
    var showBedPicker by remember { mutableStateOf(false) }

    // Dynamic next reminder calculation
    val nextReminderDisplay = remember(saved, wakeUpTime, bedtime, selectedInterval, waterReminderEnabled) {
        if (!waterReminderEnabled) "Disabled"
        else ReminderScheduler.getNextReminderTime(
            saved.copy(
                waterReminderEnabled = true,
                wakeUpTime = wakeUpTime,
                bedtime = bedtime,
                intervalMins = selectedInterval
            )
        )
    }

    // Permission handling
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notifications are required to receive reminders", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBattery = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Create Reminders",
            color = PrimaryGreen,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Background Optimization Prompt Pill
        if (!isIgnoringBattery && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryGreen.copy(alpha = 0.12f))
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                    .clickable {
                        @SuppressLint("BatteryLife")
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                    .padding(14.dp)
            ) {
                Text(
                    text = "Allow app to run in background for reliable alarm delivery (Tap to enable)",
                    color = PrimaryGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        // 1. Water Intake Reminder Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardDarkBg)
                .border(1.dp, CardBorderColor, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Water intake reminder",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (waterReminderEnabled) "Active" else "Tap + to enable & configure",
                            color = if (waterReminderEnabled) PrimaryGreen else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen)
                            .clickable {
                                isWaterCardExpanded = !isWaterCardExpanded
                                if (!waterReminderEnabled) {
                                    waterReminderEnabled = true
                                    val newSettings = saved.copy(
                                        waterReminderEnabled = true,
                                        wakeUpTime = wakeUpTime,
                                        bedtime = bedtime,
                                        intervalMins = selectedInterval
                                    )
                                    viewModel.updateReminderSettings(newSettings)
                                    ReminderScheduler.scheduleWaterReminders(context, newSettings)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWaterCardExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isWaterCardExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Wake-up time", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        TimeSelectorPill(time = wakeUpTime, icon = "🕒") {
                            showWakePicker = true
                        }

                        Text("Bedtime", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        TimeSelectorPill(time = bedtime, icon = "🌙") {
                            showBedPicker = true
                        }

                        Text("Reminder duration", color = PrimaryGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(30, 60, 90).forEach { mins ->
                                val isSelected = selectedInterval == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) PrimaryGreen else InnerPillBg)
                                        .border(1.dp, if (isSelected) PrimaryGreen else CardBorderColor, RoundedCornerShape(16.dp))
                                        .clickable {
                                            selectedInterval = mins
                                            val newSettings = saved.copy(
                                                waterReminderEnabled = true,
                                                wakeUpTime = wakeUpTime,
                                                bedtime = bedtime,
                                                intervalMins = mins
                                            )
                                            waterReminderEnabled = true
                                            viewModel.updateReminderSettings(newSettings)
                                            ReminderScheduler.scheduleWaterReminders(context, newSettings)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$mins mins",
                                        color = if (isSelected) Color.Black else TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Next Reminder Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardDarkBg)
                .border(1.dp, CardBorderColor, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Next Reminder :", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (waterReminderEnabled) "🕒  $nextReminderDisplay - Drink Water" else "Reminders paused",
                        color = if (waterReminderEnabled) TextWhite else TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Daily Workout Reminder Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardDarkBg)
                .border(1.dp, CardBorderColor, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Remind me to workout daily", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Morning notification at 08:00 AM", color = TextMuted, fontSize = 12.sp)
                }

                Switch(
                    checked = workoutReminderEnabled,
                    onCheckedChange = { checked ->
                        workoutReminderEnabled = checked
                        val newSettings = saved.copy(workoutReminderEnabled = checked)
                        viewModel.updateReminderSettings(newSettings)
                        ReminderScheduler.scheduleWorkoutReminder(context, checked)
                        if (checked) {
                            Toast.makeText(context, "Workout reminders enabled", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = PrimaryGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Google Clock Style Dial Dialogs
    if (showWakePicker) {
        val parsed = remember(wakeUpTime) { ReminderScheduler.parseTime(wakeUpTime) }
        ClockTimePickerDialog(
            initialHour = parsed.hour,
            initialMinute = parsed.minute,
            onDismiss = { showWakePicker = false },
            onConfirm = { hour, minute ->
                val formatted = ReminderScheduler.formatTime(LocalTime.of(hour, minute))
                wakeUpTime = formatted
                showWakePicker = false
                val newSettings = saved.copy(
                    waterReminderEnabled = true,
                    wakeUpTime = formatted,
                    bedtime = bedtime,
                    intervalMins = selectedInterval
                )
                waterReminderEnabled = true
                viewModel.updateReminderSettings(newSettings)
                ReminderScheduler.scheduleWaterReminders(context, newSettings)
            }
        )
    }

    if (showBedPicker) {
        val parsed = remember(bedtime) { ReminderScheduler.parseTime(bedtime) }
        ClockTimePickerDialog(
            initialHour = parsed.hour,
            initialMinute = parsed.minute,
            onDismiss = { showBedPicker = false },
            onConfirm = { hour, minute ->
                val formatted = ReminderScheduler.formatTime(LocalTime.of(hour, minute))
                bedtime = formatted
                showBedPicker = false
                val newSettings = saved.copy(
                    waterReminderEnabled = true,
                    wakeUpTime = wakeUpTime,
                    bedtime = formatted,
                    intervalMins = selectedInterval
                )
                waterReminderEnabled = true
                viewModel.updateReminderSettings(newSettings)
                ReminderScheduler.scheduleWaterReminders(context, newSettings)
            }
        )
    }
}

@Composable
fun TimeSelectorPill(
    time: String,
    icon: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(InnerPillBg)
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$icon  $time", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Pick time", tint = PrimaryGreen)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF161719),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    color = PrimaryGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(20.dp))

                // The TimePicker with its proper full width
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Color(0xFF0F1E16),
                        clockDialSelectedContentColor = Color.Black,
                        clockDialUnselectedContentColor = TextWhite,
                        selectorColor = PrimaryGreen,
                        containerColor = Color(0xFF161719),
                        periodSelectorBorderColor = CardBorderColor,
                        periodSelectorSelectedContainerColor = PrimaryGreen,
                        periodSelectorUnselectedContainerColor = InnerPillBg,
                        periodSelectorSelectedContentColor = Color.Black,
                        periodSelectorUnselectedContentColor = TextWhite,
                        timeSelectorSelectedContainerColor = PrimaryGreen,
                        timeSelectorUnselectedContainerColor = InnerPillBg,
                        timeSelectorSelectedContentColor = Color.Black,
                        timeSelectorUnselectedContentColor = TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
