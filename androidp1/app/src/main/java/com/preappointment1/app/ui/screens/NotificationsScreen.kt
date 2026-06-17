package com.preappointment1.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.preappointment1.app.ui.components.*
import com.preappointment1.app.ui.theme.*
import com.preappointment1.app.worker.NotificationScheduler

private data class ReminderUi(
    val id: String,
    val title: String,
    val description: String,
    val time: String
)

private val defaultReminders = listOf(
    ReminderUi(
        id = "photo",
        title = "Daily photo",
        description = "Guided photo of the tracked area",
        time = "9:00 AM"
    ),
    ReminderUi(
        id = "checkin",
        title = "Evening check-in",
        description = "Pain level and infection questions",
        time = "7:00 PM"
    ),
    ReminderUi(
        id = "missed",
        title = "Missed routine alert",
        description = "If nothing is recorded by this time",
        time = "8:30 PM"
    )
)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val enabledStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            defaultReminders.forEach { put(it.id, true) }
        }
    }
    var testSent by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                NotificationScheduler.scheduleRoutineReminder(
                    context = context,
                    title = "P1 — Daily routine",
                    message = "Time for your photo and check-in.",
                    delayMinutes = 0
                )
                testSent = true
            }
        }
    )

    fun sendTestNotification() {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationScheduler.scheduleRoutineReminder(
                context = context,
                title = "P1 — Daily routine",
                message = "Time for your photo and check-in.",
                delayMinutes = 0
            )
            testSent = true
        }
    }

    Scaffold(
        topBar = { LpmTopBar(title = "Notifications", onBack = onBack) },
        containerColor = White,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                LpmBodyText(
                    "Reminders keep your follow-up consistent. " +
                        "Each entry strengthens the report for your doctor."
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DAILY REMINDERS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Gray400,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(defaultReminders.size) { index ->
                val reminder = defaultReminders[index]
                ReminderRow(
                    reminder = reminder,
                    enabled = enabledStates[reminder.id] == true,
                    onToggle = { enabledStates[reminder.id] = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                LpmSecondaryButton(
                    text = if (testSent) "Test notification sent" else "Send a test notification",
                    onClick = { sendTestNotification() },
                    enabled = !testSent
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: ReminderUi,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    LpmCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Black else Gray400
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reminder.time,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) Gray600 else Gray400
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray400
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = White,
                    checkedTrackColor = Black,
                    uncheckedThumbColor = White,
                    uncheckedTrackColor = Gray200,
                    uncheckedBorderColor = Gray200
                )
            )
        }
    }
}
