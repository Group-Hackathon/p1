package com.livingpatientmemory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livingpatientmemory.ui.components.*
import com.livingpatientmemory.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DayStatus { Done, Today, Upcoming }

private data class JourneyDay(
    val dayNumber: Int,
    val date: LocalDate,
    val status: DayStatus
)

@Composable
fun JourneyScreen(
    followUp: FollowUpUi,
    onBack: () -> Unit,
    onStartRoutine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val days = remember(followUp) { buildJourney(followUp) }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
    }

    Scaffold(
        topBar = { LpmTopBar(title = followUp.title, onBack = onBack) },
        containerColor = White,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                JourneySummary(followUp = followUp)
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "YOUR PLAN",
                    style = MaterialTheme.typography.labelMedium,
                    color = Gray400,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(days.size) { index ->
                val day = days[index]
                JourneyNode(
                    day = day,
                    dateFormatter = dateFormatter,
                    isLast = index == days.size - 1,
                    zigzagRight = index % 2 == 1,
                    onStartRoutine = onStartRoutine
                )
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

private fun buildJourney(followUp: FollowUpUi): List<JourneyDay> {
    val todayIndex = (followUp.totalDays - followUp.daysRemaining)
        .coerceIn(0, followUp.totalDays - 1)
    val startDate = LocalDate.now().minusDays(todayIndex.toLong())

    return (0 until followUp.totalDays).map { i ->
        JourneyDay(
            dayNumber = i + 1,
            date = startDate.plusDays(i.toLong()),
            status = when {
                i < todayIndex -> DayStatus.Done
                i == todayIndex && followUp.isActive -> DayStatus.Today
                else -> DayStatus.Upcoming
            }
        )
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
private fun JourneyNode(
    day: JourneyDay,
    dateFormatter: DateTimeFormatter,
    isLast: Boolean,
    zigzagRight: Boolean,
    onStartRoutine: () -> Unit
) {
    val startPadding = if (zigzagRight) 56.dp else 0.dp

    Column(modifier = Modifier.padding(start = startPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DayCircle(day = day)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Day ${day.dayNumber}" + if (day.status == DayStatus.Today) " — Today" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (day.status == DayStatus.Upcoming) Gray400 else Black
                )
                Text(
                    text = day.date.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray400
                )
                if (day.status != DayStatus.Done) {
                    Text(
                        text = "Photo 9:00 AM · Check-in 7:00 PM",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (day.status == DayStatus.Today) Gray600 else Gray400
                    )
                }
            }
        }

        if (day.status == DayStatus.Today) {
            Spacer(modifier = Modifier.height(12.dp))
            LpmPrimaryButton(
                text = "Start today's routine",
                onClick = onStartRoutine,
                modifier = Modifier.padding(start = 64.dp, end = if (zigzagRight) 0.dp else 56.dp)
            )
        }

        if (!isLast) {
            Box(
                modifier = Modifier
                    .padding(start = 23.dp)
                    .width(2.dp)
                    .height(28.dp)
                    .background(if (day.status == DayStatus.Done) Black else Gray200)
            )
        }
    }
}

@Composable
private fun DayCircle(day: JourneyDay) {
    when (day.status) {
        DayStatus.Done -> Box(
            modifier = Modifier
                .size(48.dp)
                .background(Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Done",
                tint = White,
                modifier = Modifier.size(22.dp)
            )
        }

        DayStatus.Today -> Box(
            modifier = Modifier
                .size(48.dp)
                .border(3.dp, Black, CircleShape)
                .background(White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${day.dayNumber}",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Black
            )
        }

        DayStatus.Upcoming -> Box(
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, Gray200, CircleShape)
                .background(White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${day.dayNumber}",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Gray400
            )
        }
    }
}
