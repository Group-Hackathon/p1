package com.livingpatientmemory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livingpatientmemory.data.AuthHelper
import com.livingpatientmemory.data.api.ApiClient
import com.livingpatientmemory.data.model.AgentResponse
import com.livingpatientmemory.data.model.SubscriptionResponse
import com.livingpatientmemory.ui.components.*
import com.livingpatientmemory.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class FollowUpUi(
    val id: String,
    val title: String,
    val daysRemaining: Int,
    val totalDays: Int,
    val progress: Float,
    val isActive: Boolean
)

@Composable
fun DashboardScreen(
    refreshKey: Int,
    onNewFollowUp: () -> Unit,
    onOpenJourney: (FollowUpUi) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    var followUps by remember { mutableStateOf<List<FollowUpUi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showComingSoon by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        isLoading = true
        errorMessage = null
        try {
            if (!AuthHelper.ensureAuthenticated()) {
                errorMessage = "Unable to connect. Check your network."
                followUps = emptyList()
                return@LaunchedEffect
            }
            val subscriptions = ApiClient.apiService.getSubscriptions()
            val agents = ApiClient.apiService.getAgents().associateBy { it.id }
            followUps = subscriptions.map { it.toFollowUpUi(agents) }
        } catch (e: Exception) {
            errorMessage = "Unable to load your follow-ups."
            followUps = emptyList()
        } finally {
            isLoading = false
        }
    }

    if (showComingSoon) {
        ComingSoonDialog(onDismiss = { showComingSoon = false })
    }

    val activeFollowUps = followUps.filter { it.isActive }
    val today = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
    )

    Scaffold(
        topBar = {
            DashboardHeader(
                hasPendingTasks = activeFollowUps.isNotEmpty(),
                onOpenNotifications = onOpenNotifications
            )
        },
        containerColor = White,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = today,
                    style = MaterialTheme.typography.labelLarge,
                    color = Gray400,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Black, strokeWidth = 2.dp)
                    }
                }
            }

            errorMessage?.let { msg ->
                item { Text(msg, color = Gray600, style = MaterialTheme.typography.bodyMedium) }
            }

            if (!isLoading && errorMessage == null) {
                if (activeFollowUps.isNotEmpty()) {
                    item { TodayCard(count = activeFollowUps.size) }
                }

                item {
                    Text(
                        text = "ACTIVE FOLLOW-UPS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Gray400,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (followUps.isEmpty()) {
                    item {
                        EmptyFollowUps(onNewFollowUp = onNewFollowUp)
                    }
                } else {
                    items(followUps, key = { it.id }) { followUp ->
                        FollowUpCard(
                            followUp = followUp,
                            onClick = { onOpenJourney(followUp) },
                            onDelete = {
                                scope.launch {
                                    try {
                                        ApiClient.apiService.deleteSubscription(followUp.id)
                                        followUps = followUps.filter { it.id != followUp.id }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to delete subscription"
                                    }
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (followUps.isNotEmpty()) {
                        LpmPrimaryButton(text = "Start a new follow-up", onClick = onNewFollowUp)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    LpmSecondaryButton(
                        text = "Scan your doctor's protocol",
                        onClick = { showComingSoon = true }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    hasPendingTasks: Boolean,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "P1",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
            color = Black
        )
        Box {
            IconButton(onClick = onOpenNotifications) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Black
                )
            }
            if (hasPendingTasks) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .background(Black, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun TodayCard(count: Int) {
    LpmCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TODAY",
                style = MaterialTheme.typography.labelMedium,
                color = Gray400,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (count == 1) "1 routine to complete" else "$count routines to complete",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Photo and check-in expected before 8:00 PM",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray600
            )
        }
    }
}

@Composable
private fun FollowUpCard(
    followUp: FollowUpUi,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    LpmCard(onClick = onClick) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = followUp.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (followUp.isActive) {
                            "Day ${followUp.totalDays - followUp.daysRemaining + 1} of ${followUp.totalDays}"
                        } else {
                            "Completed"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray600
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Gray400
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    DayBadge(
                        current = followUp.totalDays - followUp.daysRemaining + 1,
                        total = followUp.totalDays,
                        isActive = followUp.isActive
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LpmProgressBar(progress = followUp.progress)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (followUp.isActive) "View plan and today's routine" else "View summary",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Black
            )
        }
    }
}

@Composable
private fun DayBadge(current: Int, total: Int, isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(if (isActive) Black else Gray200, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isActive) "$current" else "End",
            color = if (isActive) White else Gray600,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyFollowUps(onNewFollowUp: () -> Unit) {
    LpmCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.DateRange,
                contentDescription = null,
                tint = Gray400,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No active follow-up",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Describe what you want to track and we will build your daily protocol, ready for your next appointment.",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray600,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            LpmPrimaryButton(text = "Start a follow-up", onClick = onNewFollowUp)
        }
    }
}

@Composable
fun ComingSoonDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RoundedCornerShape(4.dp),
        title = {
            Text(
                "Coming soon",
                fontWeight = FontWeight.Bold,
                color = Black
            )
        },
        text = {
            Text(
                "Scanning prescriptions and doctor protocols will be available in a future update.",
                color = Gray600
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = Black, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

private fun SubscriptionResponse.toFollowUpUi(agents: Map<String, AgentResponse>): FollowUpUi {
    val start = runCatching { Instant.parse(starts_at) }.getOrNull() ?: Instant.now()
    val end = runCatching { Instant.parse(expires_at) }.getOrNull() ?: start.plus(14, ChronoUnit.DAYS)
    val now = Instant.now()
    val totalDays = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1).toInt()
    val elapsedDays = ChronoUnit.DAYS.between(start, now).coerceAtLeast(0)
    val daysRemaining = ChronoUnit.DAYS.between(now, end).coerceAtLeast(0).toInt()
    val progress = (elapsedDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    val title = agents[agent_id]?.name ?: agent_id.replace("-", " ").replaceFirstChar { it.uppercase() }

    return FollowUpUi(
        id = id,
        title = title,
        daysRemaining = daysRemaining,
        totalDays = totalDays,
        progress = progress,
        isActive = daysRemaining > 0
    )
}
