package com.livingpatientmemory.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livingpatientmemory.ui.components.LpmPrimaryButton
import com.livingpatientmemory.ui.components.LpmSecondaryButton
import com.livingpatientmemory.ui.theme.Black
import com.livingpatientmemory.ui.theme.Gray200
import com.livingpatientmemory.ui.theme.Gray500
import com.livingpatientmemory.ui.theme.White

@Composable
fun WelcomeScreen(
    onStartTracking: () -> Unit,
    onGoToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSlide by remember { mutableIntStateOf(0) }
    
    val slides = listOf(
        Pair(
            "Prepare your medical appointments",
            "Track symptoms, vitals, and photos daily. Your doctor gets a complete picture, ready at appointment time."
        ),
        Pair(
            "Evidence-based tracking rules",
            "Choose what to track (vitals, photos). We prepare the best personalized protocol based on thousands of medical records."
        )
    )

    Scaffold(
        containerColor = White,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Slide Content
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "slide_transition"
            ) { targetSlide ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = slides[targetSlide].first,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = slides[targetSlide].second,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Gray500,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Dots indicator
            Row(
                modifier = Modifier.padding(vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(slides.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (currentSlide == index) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (currentSlide == index) Black else Gray200)
                    )
                }
            }

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentSlide < slides.size - 1) {
                    LpmPrimaryButton(
                        text = "Next",
                        onClick = { currentSlide++ }
                    )
                    TextButton(
                        onClick = onGoToHome,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Skip — go to dashboard",
                            color = Gray500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LpmPrimaryButton(
                        text = "Start a new tracking",
                        onClick = onStartTracking
                    )
                    TextButton(
                        onClick = onGoToHome,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Go to Dashboard",
                            color = Gray500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
