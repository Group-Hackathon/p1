package com.livingpatientmemory.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.livingpatientmemory.MainTopBar
import com.livingpatientmemory.data.api.ApiClient
import com.livingpatientmemory.data.model.TimelineEventRequest
import com.livingpatientmemory.data.model.TimelineEventResponse
import com.livingpatientmemory.ui.components.*
import com.livingpatientmemory.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private enum class MeasurementStep {
    Pain, Temperature, Photo
}

private sealed class TimelineItem {
    data class PastEvent(val userEvent: TimelineEventResponse, val aiEvent: TimelineEventResponse?) : TimelineItem()
    data class FutureDay(val dayNumber: Int, val label: String) : TimelineItem()
}

@Composable
fun JourneyScreen(
    followUp: FollowUpUi,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<TimelineEventResponse>>(emptyList()) }
    var isFormMode by remember { mutableStateOf(false) }

    val now = LocalTime.now()
    val schedule = followUp.schedule ?: mapOf(
        "08:00" to listOf("pain", "temperature"), 
        "20:00" to listOf("pain", "temperature", "photo")
    )
    
    val sortedTimes = schedule.keys.mapNotNull { 
        runCatching { LocalTime.parse(it) }.getOrNull() 
    }.sorted()

    var activeWindowTime: LocalTime? = null
    var nextWindowTime: LocalTime? = null
    var isMeasurementWindow = false

    for (time in sortedTimes) {
        val windowEnd = time.plusHours(4)
        if ((now.isAfter(time) || now == time) && now.isBefore(windowEnd)) {
            activeWindowTime = time
            isMeasurementWindow = true
            break
        }
    }

    if (!isMeasurementWindow) {
        for (time in sortedTimes) {
            if (time.isAfter(now)) {
                nextWindowTime = time
                break
            }
        }
        if (nextWindowTime == null && sortedTimes.isNotEmpty()) {
            nextWindowTime = sortedTimes.first()
        }
    }

    val isInitial = events.isEmpty()
    if (isInitial && sortedTimes.isNotEmpty() && !isMeasurementWindow) {
        isMeasurementWindow = true
        activeWindowTime = null
    }

    val periodName = activeWindowTime?.toString() ?: if (isInitial) "Initial" else "Routine"

    val nextWindowName = nextWindowTime?.toString()?.let { "Check-in at $it" } ?: "Next Check-in"

    LaunchedEffect(followUp.id) {
        while (true) {
            try {
                val remoteEvents = ApiClient.apiService.getTimeline(followUp.id)
                events = remoteEvents.sortedBy { it.created_at }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(3000)
        }
    }

    val timelineItems = remember(events, followUp) {
        val items = mutableListOf<TimelineItem>()
        var i = 0
        while (i < events.size) {
            val event = events[i]
            if (event.type == "user") {
                var aiEvent: TimelineEventResponse? = null
                if (i + 1 < events.size && events[i+1].type == "ai") {
                    aiEvent = events[i+1]
                    i++
                }
                items.add(TimelineItem.PastEvent(event, aiEvent))
            } else if (event.type == "ai") {
                items.add(TimelineItem.PastEvent(event, null))
            }
            i++
        }
        
        val daysDone = followUp.totalDays - followUp.daysRemaining
        val startFutureDay = if (daysDone < 1) 1 else daysDone + 1
        
        for (i in startFutureDay..followUp.totalDays) {
            items.add(TimelineItem.FutureDay(dayNumber = i, label = "Day $i - Scheduled tracking"))
        }
        items
    }

    Box(modifier = modifier.fillMaxSize().background(White)) {
        Scaffold(
            topBar = { MainTopBar(title = followUp.title, onOpenDrawer = onOpenDrawer) },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Vertical central line
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Gray200)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        item {
                            JourneySummary(followUp = followUp, events = events)
                            TopInfoCard(followUp, periodName, isMeasurementWindow, nextWindowName, nextWindowTime)
                        }

                        if (events.isEmpty()) {
                            item {
                                EmptyStateWelcome()
                            }
                        }

                        items(timelineItems) { item ->
                            when (item) {
                                is TimelineItem.PastEvent -> {
                                    CentralTimelineEvent(
                                        userEvent = item.userEvent,
                                        aiEvent = item.aiEvent,
                                        onDelete = { eventId ->
                                            coroutineScope.launch {
                                                try {
                                                    ApiClient.apiService.deleteTimelineEvent(followUp.id, eventId)
                                                    val remoteEvents = ApiClient.apiService.getTimeline(followUp.id)
                                                    events = remoteEvents.sortedBy { it.created_at }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    )
                                }
                                is TimelineItem.FutureDay -> {
                                    FutureTimelineEvent(item.dayNumber, item.label)
                                }
                            }
                        }
                    }
                }

                BottomChatAndActions(
                    followUp = followUp,
                    isMeasurementWindow = isMeasurementWindow,
                    periodName = periodName,
                    onStartRoutine = { isFormMode = true }
                )
            }
        }

        // Focus Mode Overlay
        if (isFormMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black.copy(alpha = 0.5f))
            ) {
                FocusModeForm(
                    followUp = followUp,
                    periodName = periodName,
                    onClose = { isFormMode = false }
                )
            }
        }
    }
}

@Composable
private fun EmptyStateWelcome() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(White, RoundedCornerShape(12.dp))
                .border(1.dp, Gray200, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                "Your personalized protocol has started. Let's begin your first measurement.",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray600,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun JourneySummary(followUp: FollowUpUi, events: List<TimelineEventResponse>) {
    val expectedMeasurements = followUp.totalDays * (followUp.schedule?.size ?: 1)
    val actualMeasurements = events.count { it.type == "user" && !it.date_label.contains("Question") }
    val progressPercent = if (expectedMeasurements > 0) {
        ((actualMeasurements.toFloat() / expectedMeasurements.toFloat()) * 100).toInt().coerceAtMost(100)
    } else 0
    val progressFloat = if (expectedMeasurements > 0) {
        (actualMeasurements.toFloat() / expectedMeasurements.toFloat()).coerceAtMost(1f)
    } else 0f

    val apptDate = java.time.LocalDate.now().plusDays(followUp.daysRemaining.toLong())
    val formattedApptDate = apptDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH))

    LpmCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(value = formattedApptDate, label = "Appt Date")
                SummaryItem(value = "${followUp.daysRemaining}", label = "Days left")
                SummaryItem(value = "$progressPercent%", label = "Complete")
            }
            Spacer(modifier = Modifier.height(16.dp))
            LpmProgressBar(progress = progressFloat)
        }
    }
}

@Composable
private fun SummaryItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Black)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Gray400)
    }
}

@Composable
private fun TopInfoCard(followUp: FollowUpUi, periodName: String, isWindow: Boolean, nextWindowName: String, nextWindowTime: LocalTime?) {
    var countdownText by remember { mutableStateOf("") }
    
    LaunchedEffect(nextWindowTime, isWindow) {
        if (nextWindowTime == null || isWindow) {
            countdownText = ""
            return@LaunchedEffect
        }
        while(true) {
            val now = LocalTime.now()
            var durationSeconds = java.time.temporal.ChronoUnit.SECONDS.between(now, nextWindowTime)
            if (durationSeconds < 0) {
                durationSeconds += 24 * 3600
            }
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            countdownText = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            kotlinx.coroutines.delay(1000)
        }
    }

    LpmCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .background(Gray200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (followUp.isActive) "Ongoing" else "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Next Appt: In ${followUp.daysRemaining} days", style = MaterialTheme.typography.bodySmall, color = Gray600)
            
            val actionText = if (isWindow) {
                "Next Action: $periodName Check-in (Now)"
            } else {
                if (nextWindowTime != null) "Next Action: $nextWindowName (in $countdownText)"
                else "Next Action: Check-in (Pending)"
            }
            
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall,
                color = Black,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CentralTimelineEvent(userEvent: TimelineEventResponse, aiEvent: TimelineEventResponse?, onDelete: (String) -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Record") },
            text = { Text("Are you sure you want to delete this record?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(userEvent.id)
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    val formatTime = { timeStr: String ->
        runCatching {
            val zdt = java.time.ZonedDateTime.parse(timeStr)
            zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrDefault("")
    }

    // Wrap the entire interaction block
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        // --- USER EVENT ---
        val userDateLabel = userEvent.date_label.ifEmpty { "USER" }.uppercase()
        val userTime = formatTime(userEvent.created_at)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(0.45f))
            Box(modifier = Modifier.weight(0.1f), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(10.dp).background(Black, CircleShape))
            }
            Column(
                modifier = Modifier.weight(0.45f).padding(start = 12.dp, end = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(if (userTime.isNotEmpty()) "$userDateLabel • $userTime" else userDateLabel, style = MaterialTheme.typography.labelSmall, color = Gray400)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(White, RoundedCornerShape(12.dp))
                        .border(1.dp, Gray200, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { if (userEvent.type == "user") showDeleteConfirm = true }
                        )
                ) {
                    Text(userEvent.content, style = MaterialTheme.typography.bodySmall, color = Black)
                }
            }
        }

        // --- AI EVENT ---
        if (aiEvent != null) {
            val aiDateLabel = aiEvent.date_label.ifEmpty { "ASSISTANT" }.uppercase()
            val aiTime = formatTime(aiEvent.created_at)
            
            // Connection line if we want visual link between the two boxes
            Row(modifier = Modifier.fillMaxWidth().height(16.dp)) {}
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(0.45f).padding(end = 12.dp, start = 20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(if (aiTime.isNotEmpty()) "$aiDateLabel • $aiTime" else aiDateLabel, style = MaterialTheme.typography.labelSmall, color = Gray400)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(White, RoundedCornerShape(12.dp))
                            .border(1.dp, Gray200, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(aiEvent.content, style = MaterialTheme.typography.bodySmall, color = Black)
                    }
                }
                Box(modifier = Modifier.weight(0.1f), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF4CAF50), CircleShape)) // Green dot
                }
                Spacer(modifier = Modifier.weight(0.45f))
            }
        }
    }
}

@Composable
private fun FutureTimelineEvent(day: Int, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).alpha(0.5f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(0.45f))
        Box(modifier = Modifier.weight(0.1f), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(10.dp).background(Gray200, CircleShape))
        }
        Column(
            modifier = Modifier.weight(0.45f).padding(start = 12.dp, end = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("UPCOMING", style = MaterialTheme.typography.labelSmall, color = Gray400)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(Gray50, RoundedCornerShape(12.dp))
                    .border(1.dp, Gray200, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = Gray600)
            }
        }
    }
}

@Composable
private fun BottomChatAndActions(
    followUp: FollowUpUi,
    isMeasurementWindow: Boolean,
    periodName: String,
    onStartRoutine: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        color = White,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isMeasurementWindow) {
                Button(
                    onClick = onStartRoutine,
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Black)
                ) {
                    Text("📋 Fill measurements ($periodName)", color = White, fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Ask a question...", color = Gray400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gray200,
                        unfocusedBorderColor = Gray200,
                        cursorColor = Black
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (isSending || text.isBlank()) return@IconButton
                                isSending = true
                                coroutineScope.launch {
                                    try {
                                        ApiClient.apiService.postTimelineEvent(
                                            followUp.id,
                                            TimelineEventRequest(content = text.trim(), date_label = "Question")
                                        )
                                        text = ""
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isSending = false
                                    }
                                }
                            }
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Black)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FocusModeForm(followUp: FollowUpUi, periodName: String, onClose: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val answers = remember { androidx.compose.runtime.mutableStateMapOf<MeasurementStep, String>() }
    var isSending by remember { mutableStateOf(false) }
    
    val steps = remember(followUp.schedule, periodName) {
        val list = mutableListOf<MeasurementStep>()
        val actions = followUp.schedule?.get(periodName) ?: listOf("pain", "temperature")
        
        actions.forEach { action ->
            when (action.lowercase()) {
                "pain" -> list.add(MeasurementStep.Pain)
                "temperature" -> list.add(MeasurementStep.Temperature)
                "photo" -> list.add(MeasurementStep.Photo)
            }
        }
        if (list.isEmpty()) {
            list.add(MeasurementStep.Pain)
        }
        list
    }

    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    
    var currentStepIndex by remember { mutableIntStateOf(0) }
    
    val advanceOrClose = {
        if (currentStepIndex < steps.size - 1) {
            currentStepIndex++
        } else {
            isSending = true
            coroutineScope.launch {
                val content = buildString {
                    appendLine("Routine Check-in ($periodName):")
                    answers[MeasurementStep.Pain]?.let { appendLine("• Pain Level: $it/10") }
                    answers[MeasurementStep.Temperature]?.let { appendLine("• Temperature: $it °C") }
                    answers[MeasurementStep.Photo]?.let { appendLine("• Photo: $it") }
                }
                try {
                    ApiClient.apiService.postTimelineEvent(
                        followUp.id,
                        TimelineEventRequest(content = content.trim(), date_label = periodName)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isSending = false
                    onClose()
                }
            }
        }
    }

    val currentStep = steps[currentStepIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume click
                ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = White
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text(
                    "Please enter your measurement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isSending) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Black)
                    }
                } else {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(tween(300))).togetherWith(
                                slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(tween(300))
                            )
                        },
                        label = "StepTransition"
                    ) { step ->
                        when (step) {
                            MeasurementStep.Pain -> {
                                Column {
                                    var pain by remember { mutableFloatStateOf(0f) }
                                    Text("Pain Level (0-10)", style = MaterialTheme.typography.labelMedium)
                                    Slider(
                                        value = pain,
                                        onValueChange = { pain = it },
                                        valueRange = 0f..10f,
                                        steps = 9,
                                        colors = SliderDefaults.colors(thumbColor = Black, activeTrackColor = Black)
                                    )
                                    Text("${pain.toInt()}/10", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    LpmPrimaryButton("Next", onClick = {
                                        answers[MeasurementStep.Pain] = pain.toInt().toString()
                                        advanceOrClose()
                                    })
                                }
                            }
                            MeasurementStep.Temperature -> {
                                Column {
                                    var temp by remember { mutableStateOf("") }
                                    Text("Quick Select", style = MaterialTheme.typography.bodySmall, color = Gray600)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf("36.5", "37.0", "37.5", "38.0").forEach { preset ->
                                            androidx.compose.material3.FilterChip(
                                                selected = temp == preset,
                                                onClick = { temp = preset },
                                                label = { Text("$preset°") },
                                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Black,
                                                    selectedLabelColor = White
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    OutlinedTextField(
                                        value = temp,
                                        onValueChange = { temp = it },
                                        label = { Text("Or enter specific (°C)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    LpmPrimaryButton("Next", onClick = {
                                        answers[MeasurementStep.Temperature] = temp
                                        advanceOrClose()
                                    })
                                }
                            }
                            MeasurementStep.Photo -> {
                                Column {
                                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Gray200, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Text("Camera Preview", color = Gray400)
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    LpmPrimaryButton("Finish", onClick = {
                                        answers[MeasurementStep.Photo] = "Captured"
                                        advanceOrClose()
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
