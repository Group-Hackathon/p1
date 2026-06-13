package com.livingpatientmemory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livingpatientmemory.LpmTopBar
import com.livingpatientmemory.data.api.ApiClient
import com.livingpatientmemory.data.model.TimelineEventRequest
import com.livingpatientmemory.data.model.TimelineEventResponse
import com.livingpatientmemory.ui.components.*
import com.livingpatientmemory.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun JourneyScreen(
    followUp: FollowUpUi,
    onBack: () -> Unit,
    onStartRoutine: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<TimelineEventResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(followUp.id) {
        try {
            events = ApiClient.apiService.getTimeline(followUp.id)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }
    
    // Auto-scroll to bottom on new event
    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            listState.animateScrollToItem(events.size)
        }
    }

    Scaffold(
        topBar = { LpmTopBar(title = followUp.title, onOpenDrawer = onOpenDrawer) },
        containerColor = White,
        modifier = modifier
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Black)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(White)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    item {
                        JourneySummary(followUp = followUp)
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    if (events.isEmpty()) {
                        item {
                            Text(
                                text = "No timeline events yet. The assistant will guide you shortly.",
                                color = Gray400,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    } else {
                        itemsIndexed(events) { index, event ->
                            val isUser = event.type == "user"
                            TimelineBubble(event = event, isRight = isUser)
                        }
                    }
                }

                // BOTTOM INPUT AREA
                BottomInputArea(followUpId = followUp.id) {
                    coroutineScope.launch {
                        try {
                            events = ApiClient.apiService.getTimeline(followUp.id)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneySummary(followUp: FollowUpUi) {
    LpmCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    value = "${followUp.totalDays - followUp.daysRemaining}",
                    label = "Days done"
                )
                SummaryItem(
                    value = "${followUp.daysRemaining}",
                    label = "Days left"
                )
                SummaryItem(
                    value = "${(followUp.progress * 100).toInt()}%",
                    label = "Complete"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LpmProgressBar(progress = followUp.progress)
        }
    }
}

@Composable
private fun SummaryItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Black
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Gray400
        )
    }
}

@Composable
private fun TimelineBubble(event: TimelineEventResponse, isRight: Boolean) {
    val arrangement = if (isRight) Arrangement.End else Arrangement.Start
    val bgColor = if (isRight) Black else Gray100
    val textColor = if (isRight) White else Black
    val dateColor = if (isRight) Gray200 else Gray600

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = arrangement
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bgColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = event.date_label,
                    fontSize = 11.sp,
                    color = dateColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = event.content,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomInputArea(followUpId: String, onSent: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var painValue by remember { mutableFloatStateOf(3f) }
    val coroutineScope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }

    Surface(
        color = White,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Evening Check-in",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Black
            )
            Text(
                text = "What is your current pain level?",
                fontSize = 13.sp,
                color = Gray600,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = painValue,
                    onValueChange = { painValue = it },
                    valueRange = 0f..10f,
                    steps = 9,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Black,
                        activeTrackColor = Black,
                        inactiveTrackColor = Gray200
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${painValue.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Black,
                    modifier = Modifier.width(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Or type a message...", color = Gray400) },
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
                            if (isSending) return@IconButton
                            isSending = true
                            val msg = text.ifBlank { "Pain level recorded at ${painValue.toInt()}/10." }
                            coroutineScope.launch {
                                try {
                                    ApiClient.apiService.postTimelineEvent(
                                        followUpId,
                                        TimelineEventRequest(content = msg, date_label = "Evening Check-in")
                                    )
                                    text = ""
                                    onSent()
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
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Black)
                        }
                    }
                }
            )
        }
    }
}
