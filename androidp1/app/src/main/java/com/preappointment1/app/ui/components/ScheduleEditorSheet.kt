package com.preappointment1.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.preappointment1.app.schedule.ScheduleLogic
import com.preappointment1.app.schedule.ScheduleSlot
import com.preappointment1.app.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorSheet(
    title: String,
    schedule: Map<String, List<String>>,
    onDismiss: () -> Unit,
    onSave: (Map<String, List<String>>) -> Unit,
    isSaving: Boolean = false
) {
    var editedSchedule by remember(schedule) { mutableStateOf(schedule) }
    var editingSlot by remember { mutableStateOf<ScheduleSlot?>(null) }

    val slots = remember(editedSchedule) { ScheduleLogic.parseScheduleSlots(editedSchedule) }

    if (editingSlot != null) {
        val slot = editingSlot!!
        val pickerState = rememberTimePickerState(
            initialHour = slot.time.hour,
            initialMinute = slot.time.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = { Text("Change time for ${slot.timeKey}") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTime = LocalTime.of(pickerState.hour, pickerState.minute)
                        editedSchedule = ScheduleLogic.replaceSlotTime(editedSchedule, slot.timeKey, newTime)
                        editingSlot = null
                    }
                ) { Text("OK", color = Black) }
            },
            dismissButton = {
                TextButton(onClick = { editingSlot = null }) { Text("Cancel") }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LpmBodyText("Tap a time to adjust when reminders and check-ins open.")
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                items(slots, key = { it.timeKey }) { slot ->
                    LpmCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    slot.timeKey,
                                    fontWeight = FontWeight.Bold,
                                    color = Black
                                )
                                Text(
                                    slot.actions.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray600
                                )
                            }
                            TextButton(onClick = { editingSlot = slot }) {
                                Text("Edit time", color = Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            LpmPrimaryButton(
                text = "Save schedule",
                loading = isSaving,
                onClick = { onSave(editedSchedule) }
            )
        }
    }
}

fun formatSlotMeasures(actions: List<String>): String {
    return actions.joinToString(", ") { action ->
        when (action.lowercase()) {
            "pain" -> "pain level"
            "temperature" -> "temperature"
            "photo" -> "photo"
            else -> action
        }
    }
}

fun formatDisplayTime(timeKey: String): String {
    return runCatching {
        val time = LocalTime.parse(timeKey)
        time.format(DateTimeFormatter.ofPattern("h:mm a"))
    }.getOrDefault(timeKey)
}
