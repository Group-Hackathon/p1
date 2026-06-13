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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.livingpatientmemory.data.model.FollowUpRules
import com.livingpatientmemory.ui.components.*
import com.livingpatientmemory.ui.theme.Black
import com.livingpatientmemory.ui.theme.Gray200
import com.livingpatientmemory.ui.theme.Gray600
import com.livingpatientmemory.ui.theme.White

private enum class RoutineStepType {
    Photo, Pain, Vitals, Done
}

@Composable
fun DailyRoutineScreen(
    followUpTitle: String = "Daily Routine",
    rules: FollowUpRules?,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember(rules) {
        val list = mutableListOf<RoutineStepType>()
        if (rules?.photos == true) list.add(RoutineStepType.Photo)
        if (rules?.pain != false) list.add(RoutineStepType.Pain) // Default to true if null
        if (rules?.temperature == true || rules?.bloodPressure == true || rules?.smartwatch == true) {
            list.add(RoutineStepType.Vitals)
        }
        list.add(RoutineStepType.Done)
        list
    }

    var stepIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = { LpmTopBar(title = followUpTitle, onBack = onBack) },
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
            LpmStepIndicator(currentStep = stepIndex + 1, totalSteps = steps.size)
            Spacer(modifier = Modifier.height(16.dp))

            when (steps[stepIndex]) {
                RoutineStepType.Photo -> PhotoStep(onPhotoTaken = { stepIndex++ })
                RoutineStepType.Pain -> PainStep(onContinue = { stepIndex++ })
                RoutineStepType.Vitals -> VitalsStep(rules = rules, onContinue = { stepIndex++ })
                RoutineStepType.Done -> DoneStep(onFinish = onComplete)
            }
        }
    }
}

@Composable
private fun PhotoStep(onPhotoTaken: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LpmBodyText(
                "Camera access is required to take your daily photo.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            LpmPrimaryButton(
                text = "Allow camera",
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LpmBodyText("Place the area to track inside the white frame.")
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageCapture = ImageCapture.Builder().build()
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("CameraX", "Binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .align(Alignment.Center)
                    .alpha(0.4f)
                    .border(2.dp, White, RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        LpmPrimaryButton(text = "Take photo", onClick = onPhotoTaken)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PainStep(onContinue: () -> Unit) {
    var painLevel by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        LpmSectionTitle("Pain Assessment")
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Pain level today",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0", color = Gray600, style = MaterialTheme.typography.bodySmall)
            Text("10", color = Gray600, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = painLevel,
            onValueChange = { painLevel = it },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = Black,
                activeTrackColor = Black,
                inactiveTrackColor = Gray200
            )
        )
        Text(
            text = "${painLevel.toInt()} / 10",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Black
        )

        Spacer(modifier = Modifier.weight(1f))
        LpmPrimaryButton(text = "Save", onClick = onContinue)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun VitalsStep(rules: FollowUpRules?, onContinue: () -> Unit) {
    var tempValue by remember { mutableStateOf("") }
    var bpValue by remember { mutableStateOf("") }
    var hrValue by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LpmSectionTitle("Vitals Check-in")
        Spacer(modifier = Modifier.height(24.dp))

        if (rules?.temperature == true) {
            OutlinedTextField(
                value = tempValue,
                onValueChange = { tempValue = it },
                label = { Text("Temperature (°C)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                singleLine = true
            )
        }

        if (rules?.bloodPressure == true) {
            OutlinedTextField(
                value = bpValue,
                onValueChange = { bpValue = it },
                label = { Text("Blood Pressure (mmHg)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                singleLine = true
            )
        }

        if (rules?.smartwatch == true) {
            OutlinedTextField(
                value = hrValue,
                onValueChange = { hrValue = it },
                label = { Text("Heart Rate (bpm)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        LpmPrimaryButton(text = "Save", onClick = onContinue)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DoneStep(onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        LpmSectionTitle("Routine saved")
        Spacer(modifier = Modifier.height(12.dp))
        LpmBodyText(
            "Today's data has been saved. Come back tomorrow for your next routine.",
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        LpmPrimaryButton(
            text = "Back to home",
            onClick = onFinish,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
