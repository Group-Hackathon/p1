package com.livingpatientmemory.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livingpatientmemory.data.AuthHelper
import com.livingpatientmemory.data.api.ApiClient
import com.livingpatientmemory.data.model.RecommendRequest
import com.livingpatientmemory.data.model.SubscriptionRequest
import com.livingpatientmemory.data.model.TrackingRulesDto
import com.livingpatientmemory.ui.components.*
import com.livingpatientmemory.ui.theme.Black
import com.livingpatientmemory.ui.theme.Gray200
import com.livingpatientmemory.ui.theme.Gray600
import com.livingpatientmemory.ui.theme.White
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun OnboardingScreen(
    onBack: () -> Unit,
    onFollowUpCreated: (title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }
    var symptomText by remember { mutableStateOf("") }
    var appointmentDate by remember { mutableStateOf(LocalDate.now().plusDays(14)) }
    
    // Tracking rules state
    var ruleTemperature by remember { mutableStateOf(false) }
    var rulePain by remember { mutableStateOf(true) }
    var rulePhotos by remember { mutableStateOf(true) }
    var ruleSmartwatch by remember { mutableStateOf(false) }
    var ruleBp by remember { mutableStateOf(false) }

    var isAnalyzing by remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var generatedPlan by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { 
            LpmTopBar(
                title = "New Tracking", 
                onBack = { if (step > 1) step-- else onBack() }
            ) 
        },
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
            LpmStepIndicator(currentStep = step, totalSteps = 4)
            Spacer(modifier = Modifier.height(28.dp))

            when (step) {
                1 -> DescribeStep(
                    symptomText = symptomText,
                    onSymptomChange = { symptomText = it },
                    onNext = { step = 2 }
                )
                2 -> DateStep(
                    date = appointmentDate,
                    onDateChange = { appointmentDate = it },
                    onNext = { step = 3 }
                )
                3 -> RulesStep(
                    ruleTemperature = ruleTemperature,
                    onRuleTemperatureChange = { ruleTemperature = it },
                    rulePain = rulePain,
                    onRulePainChange = { rulePain = it },
                    rulePhotos = rulePhotos,
                    onRulePhotosChange = { rulePhotos = it },
                    ruleSmartwatch = ruleSmartwatch,
                    onRuleSmartwatchChange = { ruleSmartwatch = it },
                    ruleBp = ruleBp,
                    onRuleBpChange = { ruleBp = it },
                    isLoading = isAnalyzing,
                    errorMessage = errorMessage,
                    onGeneratePlan = {
                        isAnalyzing = true
                        errorMessage = null
                        scope.launch {
                            try {
                                if (!AuthHelper.ensureAuthenticated()) {
                                    errorMessage = "Unable to connect."
                                    return@launch
                                }
                                
                                val rulesDto = TrackingRulesDto(
                                    temperature = ruleTemperature,
                                    pain = rulePain,
                                    photos = rulePhotos,
                                    smartwatch = ruleSmartwatch,
                                    blood_pressure = ruleBp
                                )

                                val response = ApiClient.apiService.recommendAgent(
                                    RecommendRequest(
                                        symptoms = symptomText,
                                        appointment_date = appointmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        rules = rulesDto
                                    )
                                )
                                generatedPlan = response.description
                                step = 4
                            } catch (e: Exception) {
                                errorMessage = "Failed to generate plan: ${e.message}"
                            } finally {
                                isAnalyzing = false
                            }
                        }
                    }
                )
                4 -> ConfirmStep(
                    planText = generatedPlan,
                    isLoading = isStarting,
                    errorMessage = errorMessage,
                    onConfirm = {
                        isStarting = true
                        errorMessage = null
                        scope.launch {
                            try {
                                if (!AuthHelper.ensureAuthenticated()) {
                                    errorMessage = "Unable to connect."
                                    return@launch
                                }
                                val profileId = AuthHelper.ensureProfile() ?: throw IllegalStateException("Profile not found")
                                
                                val rulesMap = mapOf(
                                    "temperature" to ruleTemperature,
                                    "pain" to rulePain,
                                    "photos" to rulePhotos,
                                    "smartwatch" to ruleSmartwatch,
                                    "blood_pressure" to ruleBp
                                )

                                ApiClient.apiService.createSubscription(
                                    SubscriptionRequest(
                                        profile_id = profileId,
                                        agent_id = "dynamic-plan",
                                        parameters = mapOf(
                                            "symptoms" to symptomText,
                                            "next_appointment" to appointmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                            "rules" to rulesMap,
                                            "plan" to generatedPlan
                                        )
                                    )
                                )
                                onFollowUpCreated("Personalized Protocol")
                            } catch (e: Exception) {
                                errorMessage = "Failed: ${e.message}"
                            } finally {
                                isStarting = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DescribeStep(
    symptomText: String,
    onSymptomChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        LpmSectionTitle("What would you like to track?")
        Spacer(modifier = Modifier.height(8.dp))
        LpmBodyText("Describe your situation in a few sentences.")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = symptomText,
            onValueChange = onSymptomChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            placeholder = { Text("e.g. knee wound for 3 days, fever at 39°C...", color = Gray600) },
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Black,
                unfocusedBorderColor = Gray200,
                cursorColor = Black
            )
        )

        Spacer(modifier = Modifier.weight(1f))
        LpmPrimaryButton(
            text = "Continue",
            onClick = onNext,
            enabled = symptomText.isNotBlank()
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateStep(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onNext: () -> Unit
) {
    // For simplicity in the prototype, we use a basic date display with buttons to change
    // In a real app, we'd use DatePickerDialog
    Column(modifier = Modifier.fillMaxSize()) {
        LpmSectionTitle("When is your appointment?")
        Spacer(modifier = Modifier.height(8.dp))
        LpmBodyText("Select the exact date of your next medical checkup.")
        Spacer(modifier = Modifier.height(32.dp))

        LpmCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    OutlinedButton(onClick = { onDateChange(date.minusDays(1)) }) { Text("-") }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { onDateChange(date.plusDays(1)) }) { Text("+") }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        LpmPrimaryButton(text = "Continue", onClick = onNext)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RulesStep(
    ruleTemperature: Boolean, onRuleTemperatureChange: (Boolean) -> Unit,
    rulePain: Boolean, onRulePainChange: (Boolean) -> Unit,
    rulePhotos: Boolean, onRulePhotosChange: (Boolean) -> Unit,
    ruleSmartwatch: Boolean, onRuleSmartwatchChange: (Boolean) -> Unit,
    ruleBp: Boolean, onRuleBpChange: (Boolean) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onGeneratePlan: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        LpmSectionTitle("Your tracking rules")
        Spacer(modifier = Modifier.height(8.dp))
        LpmBodyText("Choose what you want to monitor. The AI will adapt your plan accordingly.")
        Spacer(modifier = Modifier.height(24.dp))

        RuleToggle("🌡️", "Temperature", "Manual entries", ruleTemperature, onRuleTemperatureChange)
        RuleToggle("🤒", "Pain tracking", "Daily pain level", rulePain, onRulePainChange)
        RuleToggle("📸", "Daily photos", "Guided photo capture", rulePhotos, onRulePhotosChange)
        RuleToggle("⌚", "Smartwatch data", "Heart rate, steps", ruleSmartwatch, onRuleSmartwatchChange)
        RuleToggle("💓", "Blood pressure", "Manual or connected", ruleBp, onRuleBpChange)

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = Gray600, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(32.dp))
        LpmPrimaryButton(
            text = "Generate my plan",
            onClick = onGeneratePlan,
            loading = isLoading
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RuleToggle(
    emoji: String, title: String, subtitle: String,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    LpmCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = Black)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Gray600)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
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

@Composable
private fun ConfirmStep(
    planText: String,
    isLoading: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        LpmSectionTitle("Your AI Plan")
        Spacer(modifier = Modifier.height(8.dp))
        LpmBodyText("Gemini has built this routine based on your rules.")
        Spacer(modifier = Modifier.height(20.dp))

        LpmCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Daily Routine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Simple parser for bullet points
                val lines = planText.split("\n").filter { it.isNotBlank() }
                lines.forEach { line ->
                    val cleanLine = line.removePrefix("- ").removePrefix("* ")
                    DailyTaskItem(cleanLine)
                }
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = Gray600, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(32.dp))
        LpmPrimaryButton(
            text = "Start my tracking",
            onClick = onConfirm,
            loading = isLoading
        )
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
