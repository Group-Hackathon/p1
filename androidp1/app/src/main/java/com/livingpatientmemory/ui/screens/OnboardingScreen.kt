package com.livingpatientmemory.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livingpatientmemory.data.AuthHelper
import com.livingpatientmemory.data.api.ApiClient
import com.livingpatientmemory.data.model.RecommendRequest
import com.livingpatientmemory.data.model.SubscriptionRequest
import com.livingpatientmemory.ui.components.*
import com.livingpatientmemory.ui.theme.Black
import com.livingpatientmemory.ui.theme.Gray200
import com.livingpatientmemory.ui.theme.Gray600
import com.livingpatientmemory.ui.theme.White
import kotlinx.coroutines.launch

private val appointmentOptions = listOf(
    "This week",
    "In 2 weeks",
    "In a month",
    "Not scheduled yet"
)

@Composable
fun OnboardingScreen(
    onBack: () -> Unit,
    onFollowUpCreated: (title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }
    var symptomText by remember { mutableStateOf("") }
    var appointmentChoice by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showComingSoon by remember { mutableStateOf(false) }
    var recommendedAgentId by remember { mutableStateOf("wound-monitoring") }
    var recommendedAgentName by remember { mutableStateOf("") }
    var recommendedAgentDesc by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    if (showComingSoon) {
        ComingSoonDialog(onDismiss = { showComingSoon = false })
    }

    Scaffold(
        topBar = { LpmTopBar(title = "New Follow-up", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            LpmStepIndicator(currentStep = step, totalSteps = 2)
            Spacer(modifier = Modifier.height(28.dp))

            when (step) {
                1 -> DescribeStep(
                    symptomText = symptomText,
                    onSymptomChange = { symptomText = it },
                    appointmentChoice = appointmentChoice,
                    onAppointmentChange = { appointmentChoice = it },
                    onScanPrescription = { showComingSoon = true },
                    errorMessage = errorMessage,
                    isLoading = isAnalyzing,
                    onAnalyze = {
                        isAnalyzing = true
                        errorMessage = null
                        scope.launch {
                            try {
                                if (!AuthHelper.ensureAuthenticated()) {
                                    errorMessage = "Unable to connect."
                                    return@launch
                                }
                                val response = ApiClient.apiService.recommendAgent(
                                    RecommendRequest(symptoms = symptomText)
                                )
                                recommendedAgentId = response.id
                                recommendedAgentName = response.name
                                recommendedAgentDesc = response.description
                                step = 2
                            } catch (_: Exception) {
                                recommendedAgentId = "wound-monitoring"
                                recommendedAgentName = "Wound & Injury Monitoring"
                                recommendedAgentDesc = "Daily photos, pain tracking, and infection checks."
                                step = 2
                            } finally {
                                isAnalyzing = false
                            }
                        }
                    }
                )
                2 -> ConfirmStep(
                    agentName = recommendedAgentName.ifEmpty { "Wound & Injury Monitoring" },
                    agentDesc = recommendedAgentDesc.ifEmpty {
                        "14-day protocol: guided photos, pain questionnaire, infection check."
                    },
                    appointmentChoice = appointmentChoice,
                    errorMessage = errorMessage,
                    isLoading = isStarting,
                    onConfirm = {
                        isStarting = true
                        errorMessage = null
                        scope.launch {
                            try {
                                if (!AuthHelper.ensureAuthenticated()) {
                                    errorMessage = "Unable to connect."
                                    return@launch
                                }
                                val profileId = AuthHelper.ensureProfile()
                                    ?: throw IllegalStateException("Profile not found")
                                ApiClient.apiService.createSubscription(
                                    SubscriptionRequest(
                                        profile_id = profileId,
                                        agent_id = recommendedAgentId,
                                        parameters = mapOf(
                                            "symptoms" to symptomText,
                                            "next_appointment" to (appointmentChoice ?: "unknown")
                                        )
                                    )
                                )
                                onFollowUpCreated(recommendedAgentName.ifEmpty { "Follow-up" })
                            } catch (e: Exception) {
                                errorMessage = "Failed: ${e.message}"
                            } finally {
                                isStarting = false
                            }
                        }
                    },
                    onBack = { step = 1 }
                )
            }
        }
    }
}

@Composable
private fun DescribeStep(
    symptomText: String,
    onSymptomChange: (String) -> Unit,
    appointmentChoice: String?,
    onAppointmentChange: (String) -> Unit,
    onScanPrescription: () -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LpmSectionTitle("What would you like to track?")
        Spacer(modifier = Modifier.height(8.dp))
        LpmBodyText("Describe your situation in a few sentences.")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = symptomText,
            onValueChange = onSymptomChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = {
                Text(
                    "e.g. knee wound for 3 days, fever at 39°C, spreading rash…",
                    color = Gray600
                )
            },
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Black,
                unfocusedBorderColor = Gray200,
                cursorColor = Black
            )
        )

        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "When is your next doctor appointment?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            appointmentOptions.chunked(2).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { option ->
                        SelectableChip(
                            label = option,
                            selected = appointmentChoice == option,
                            onClick = { onAppointmentChange(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowOptions.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        LpmSecondaryButton(
            text = "Scan your prescription or appointment",
            onClick = onScanPrescription
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = Gray600, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(28.dp))
        LpmPrimaryButton(
            text = "Find my protocol",
            onClick = onAnalyze,
            enabled = symptomText.isNotBlank(),
            loading = isLoading
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (selected) Black else Gray200),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Black else White,
            contentColor = if (selected) White else Black
        )
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun ConfirmStep(
    agentName: String,
    agentDesc: String,
    appointmentChoice: String?,
    errorMessage: String?,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LpmSectionTitle("Your protocol")
        Spacer(modifier = Modifier.height(20.dp))

        LpmCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = agentName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = agentDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray600
                )
                appointmentChoice?.let {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Gray200)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Next appointment: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Every day:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )
        Spacer(modifier = Modifier.height(12.dp))
        DailyTaskItem("1 guided photo — morning")
        DailyTaskItem("1 pain questionnaire — evening")
        DailyTaskItem("1 infection check — evening")

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = Gray600, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.weight(1f))
        LpmPrimaryButton(
            text = "Start my follow-up",
            onClick = onConfirm,
            loading = isLoading
        )
        Spacer(modifier = Modifier.height(12.dp))
        LpmSecondaryButton(text = "Edit my description", onClick = onBack)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DailyTaskItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("—", color = Black, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Black)
    }
}
