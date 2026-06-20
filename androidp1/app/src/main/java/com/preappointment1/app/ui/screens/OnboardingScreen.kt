package com.preappointment1.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.preappointment1.app.billing.BillingManager
import com.preappointment1.app.data.AuthHelper
import com.preappointment1.app.data.api.ApiClient
import com.preappointment1.app.data.model.RecommendRequest
import com.preappointment1.app.data.model.SubscriptionRequest
import com.preappointment1.app.data.model.TrackingRulesDto
import com.preappointment1.app.ui.components.*
import com.preappointment1.app.ui.theme.Black
import com.preappointment1.app.ui.theme.Gray200
import com.preappointment1.app.ui.theme.Gray400
import com.preappointment1.app.ui.theme.Gray50
import com.preappointment1.app.ui.theme.Gray600
import com.preappointment1.app.ui.theme.White
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OnboardingScreen(
    onBack: () -> Unit,
    onFollowUpCreated: (subscriptionId: String) -> Unit,
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
    
    var generatedTitle by remember { mutableStateOf("") }
    var generatedPlan by remember { mutableStateOf("") }
    var generatedSchedule by remember { mutableStateOf<Map<String, List<String>>?>(null) }

    val scope = rememberCoroutineScope()
    
    val purchaseSuccess by BillingManager.purchaseSuccessFlow.collectAsState()

    LaunchedEffect(purchaseSuccess) {
        if (purchaseSuccess != null) {
            BillingManager.clearPurchaseSuccess()
            isStarting = true
            scope.launch {
                try {
                    if (!AuthHelper.ensureAuthenticated()) throw Exception("Authentication required")
                    val profileId = AuthHelper.ensureProfile() ?: throw IllegalStateException("Profile not found")
                    val duration = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), appointmentDate).toInt().coerceAtLeast(1)
                    val rulesMap = mapOf(
                        "temperature" to ruleTemperature,
                        "pain" to rulePain,
                        "photos" to rulePhotos,
                        "smartwatch" to ruleSmartwatch,
                        "blood_pressure" to ruleBp
                    )
                    val params = mutableMapOf<String, Any>(
                        "title" to generatedTitle,
                        "symptoms" to symptomText,
                        "next_appointment" to appointmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        "rules" to rulesMap,
                        "plan" to generatedPlan
                    )
                    generatedSchedule?.let { params["schedule"] = it }

                    val response = ApiClient.apiService.createSubscription(
                        SubscriptionRequest(
                            profile_id = profileId,
                            agent_id = "dynamic-plan",
                            duration_days = duration,
                            parameters = params
                        )
                    )
                    onFollowUpCreated(response.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isStarting = false
                }
            }
        }
    }

    Scaffold(
        topBar = { 
            LpmTopBar(
                title = if (step == 6) "Manual Setup" else "New Tracking", 
                onBack = { 
                    if (step > 1 && step != 4 && step != 5 && step != 6) step-- 
                    else if (step == 5 || step == 6) step = 3
                    else onBack() 
                }
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
            if (step <= 3) {
                LpmStepIndicator(currentStep = step, totalSteps = 3)
                Spacer(modifier = Modifier.height(28.dp))
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { width -> width }).togetherWith(
                        fadeOut() + slideOutHorizontally { width -> -width }
                    )
                },
                label = "onboarding_steps"
            ) { targetStep ->
                when (targetStep) {
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
                        ruleTemperature = ruleTemperature, onRuleTemperatureChange = { ruleTemperature = it },
                        rulePain = rulePain, onRulePainChange = { rulePain = it },
                        rulePhotos = rulePhotos, onRulePhotosChange = { rulePhotos = it },
                        ruleSmartwatch = ruleSmartwatch, onRuleSmartwatchChange = { ruleSmartwatch = it },
                        ruleBp = ruleBp, onRuleBpChange = { ruleBp = it },
                        isLoading = isAnalyzing,
                        onGeneratePlan = {
                            isAnalyzing = true
                            scope.launch {
                                try {
                                    if (!AuthHelper.ensureAuthenticated()) throw Exception("Authentication required")
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
                                    generatedTitle = response.name
                                    generatedPlan = response.description
                                    generatedSchedule = response.schedule
                                    step = 4 // Premium Preview
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    step = 5 // Offline / Error Fallback
                                } finally {
                                    isAnalyzing = false
                                }
                            }
                        }
                    )
                    4 -> PremiumPreviewStep(
                        title = generatedTitle,
                        planText = generatedPlan,
                        appointmentDate = appointmentDate,
                        isLoading = isStarting,
                        onConfirm = {
                            // Handled via Purchase Flow and LaunchedEffect above
                        },
                        onManualSetup = { step = 6 }
                    )
                    5 -> OfflineFallbackStep(
                        onRetry = { step = 3 },
                        onManualSetup = { step = 6 }
                    )
                    6 -> ManualEntryStep(
                        appointmentDate = appointmentDate,
                        isLoading = isStarting,
                        onConfirm = { customTitle, customSchedule ->
                            isStarting = true
                            scope.launch {
                                try {
                                    if (!AuthHelper.ensureAuthenticated()) throw Exception("Authentication required")
                                    val profileId = AuthHelper.ensureProfile() ?: throw IllegalStateException("Profile not found")
                                    val duration = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), appointmentDate).toInt().coerceAtLeast(1)
                                    
                                    val params = mutableMapOf<String, Any>(
                                        "title" to customTitle,
                                        "symptoms" to symptomText,
                                        "next_appointment" to appointmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        "plan" to "Manual custom tracking",
                                        "schedule" to customSchedule
                                    )

                                    val response = ApiClient.apiService.createSubscription(
                                        SubscriptionRequest(
                                            profile_id = profileId,
                                            agent_id = "dynamic-plan",
                                            duration_days = duration,
                                            parameters = params
                                        )
                                    )
                                    onFollowUpCreated(response.id)
                                } catch (e: Exception) {
                                    e.printStackTrace()
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
                    text = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)),
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
    onGeneratePlan: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        LpmSectionTitle("Your tracking rules")
        Spacer(modifier = Modifier.height(8.dp))
        LpmBodyText("Choose the health metrics you want to monitor.")
        Spacer(modifier = Modifier.height(24.dp))

        RuleToggle("Temperature", "Manual entries", ruleTemperature, onRuleTemperatureChange)
        RuleToggle("Pain tracking", "Daily pain level", rulePain, onRulePainChange)
        RuleToggle("Daily photos", "Guided photo capture", rulePhotos, onRulePhotosChange)
        RuleToggle("Smartwatch data", "Heart rate, steps", ruleSmartwatch, onRuleSmartwatchChange)
        RuleToggle("Blood pressure", "Manual or connected", ruleBp, onRuleBpChange)

        Spacer(modifier = Modifier.height(32.dp))
        LpmPrimaryButton(
            text = "Prepare my protocol",
            onClick = onGeneratePlan,
            loading = isLoading
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RuleToggle(
    title: String, subtitle: String,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    LpmCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
private fun PremiumPreviewStep(
    title: String,
    planText: String,
    appointmentDate: LocalDate,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onManualSetup: () -> Unit
) {
    val durationDays = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), appointmentDate).toInt().coerceAtLeast(1)
    
    val productId = when {
        durationDays <= 5 -> "p1_pack_short"
        durationDays <= 14 -> "p1_pack_medium"
        else -> "p1_pack_long"
    }

    val prices by BillingManager.pricesFlow.collectAsState()
    val displayPrice = prices[productId] ?: "Loading..."
    
    val context = LocalContext.current
    val activity = context as? Activity

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Highlighting the premium/proprietary aspect
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, contentDescription = "Analyzed", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Analyzed from thousands of medical records", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Here is the optimal $durationDays-day tracking program until your appointment.", style = MaterialTheme.typography.bodyMedium, color = Gray600)
        
        Spacer(modifier = Modifier.height(24.dp))

        // Schedule Preview Box (No blur)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Gray50, RoundedCornerShape(12.dp))
                .padding(2.dp) // border space
                .background(White, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                val lines = planText.split("\n").filter { it.isNotBlank() }
                lines.forEach { line ->
                    val cleanLine = line.removePrefix("- ").removePrefix("* ")
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("•", color = Black, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cleanLine, color = Gray600, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Google Play Payment Button
        Button(
            onClick = {
                if (activity != null) BillingManager.launchPurchaseFlow(activity, productId)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Black)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = White, strokeWidth = 2.dp)
            } else {
                Text(
                    "Pay $displayPrice via Google Play",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Opt-out
        Text(
            text = "Or create my planning manually for free",
            style = MaterialTheme.typography.labelLarge,
            color = Gray400,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable { onManualSetup() }.padding(8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OfflineFallbackStep(
    onRetry: () -> Unit,
    onManualSetup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Warning, contentDescription = "Offline", tint = Color(0xFFFFA726), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Connection Unavailable",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "We cannot analyze your symptoms at the moment to propose the optimal tracking protocol.",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Try again", color = Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onManualSetup,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Black)
        ) {
            Text("Proceed with manual setup", color = White, fontWeight = FontWeight.Bold)
        }
    }
}

class DayConfig {
    var mornPain by mutableStateOf(false)
    var mornTemp by mutableStateOf(false)
    var mornPhoto by mutableStateOf(false)
    
    var noonPain by mutableStateOf(false)
    var noonTemp by mutableStateOf(false)
    var noonPhoto by mutableStateOf(false)
    
    var evePain by mutableStateOf(false)
    var eveTemp by mutableStateOf(false)
    var evePhoto by mutableStateOf(false)
}

@Composable
private fun ManualEntryStep(
    appointmentDate: LocalDate,
    isLoading: Boolean,
    onConfirm: (title: String, schedule: Map<String, List<String>>) -> Unit
) {
    val durationDays = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), appointmentDate).toInt().coerceAtLeast(1)
    var trackingTitle by remember { mutableStateOf("") }
    
    // Create state for EVERY single day to make manual entry extremely tedious
    val dayConfigs = remember(durationDays) {
        List(durationDays) { DayConfig() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Manual Tracking Setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Please manually configure your tracking requirements for every single day until your appointment ($durationDays days).", style = MaterialTheme.typography.bodyMedium, color = Gray600)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = trackingTitle,
            onValueChange = { trackingTitle = it },
            label = { Text("Tracking Name (e.g. My Custom Tracking)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Use LazyColumn for performance since it could be many days
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(durationDays) { index ->
                val config = dayConfigs[index]
                val dayDate = LocalDate.now().plusDays(index.toLong())
                val formattedDate = dayDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d", java.util.Locale.ENGLISH))

                LpmCard(modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Day ${index + 1} — $formattedDate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Black)
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Gray200)

                        // Morning
                        Text("Morning (08:00)", fontWeight = FontWeight.SemiBold, color = Black, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ManualCheckbox("Pain", config.mornPain) { config.mornPain = it }
                            ManualCheckbox("Temp", config.mornTemp) { config.mornTemp = it }
                            ManualCheckbox("Photo", config.mornPhoto) { config.mornPhoto = it }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Noon
                        Text("Noon (12:00)", fontWeight = FontWeight.SemiBold, color = Black, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ManualCheckbox("Pain", config.noonPain) { config.noonPain = it }
                            ManualCheckbox("Temp", config.noonTemp) { config.noonTemp = it }
                            ManualCheckbox("Photo", config.noonPhoto) { config.noonPhoto = it }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Evening
                        Text("Evening (20:00)", fontWeight = FontWeight.SemiBold, color = Black, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ManualCheckbox("Pain", config.evePain) { config.evePain = it }
                            ManualCheckbox("Temp", config.eveTemp) { config.eveTemp = it }
                            ManualCheckbox("Photo", config.evePhoto) { config.evePhoto = it }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        LpmPrimaryButton(
            text = "Create Free Tracking",
            loading = isLoading,
            enabled = trackingTitle.isNotBlank(),
            onClick = {
                val schedule = mutableMapOf<String, List<String>>()
                
                val mornSet = mutableSetOf<String>()
                val noonSet = mutableSetOf<String>()
                val eveSet = mutableSetOf<String>()

                // Aggregate everything requested across all days
                for (config in dayConfigs) {
                    if (config.mornPain) mornSet.add("pain")
                    if (config.mornTemp) mornSet.add("temperature")
                    if (config.mornPhoto) mornSet.add("photo")

                    if (config.noonPain) noonSet.add("pain")
                    if (config.noonTemp) noonSet.add("temperature")
                    if (config.noonPhoto) noonSet.add("photo")

                    if (config.evePain) eveSet.add("pain")
                    if (config.eveTemp) eveSet.add("temperature")
                    if (config.evePhoto) eveSet.add("photo")
                }
                
                if (mornSet.isNotEmpty()) schedule["08:00"] = mornSet.toList()
                if (noonSet.isNotEmpty()) schedule["12:00"] = noonSet.toList()
                if (eveSet.isNotEmpty()) schedule["20:00"] = eveSet.toList()
                
                onConfirm(trackingTitle, schedule)
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ManualCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = Black)
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = Gray600)
    }
}
