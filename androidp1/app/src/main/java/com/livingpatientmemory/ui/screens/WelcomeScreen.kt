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
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSlide by remember { mutableIntStateOf(0) }
    
    val slides = listOf(
        Pair(
            "Prepare your medical appointments",
            "Track symptoms, vitals, and photos daily. Your doctor gets a complete picture, ready at appointment time."
        ),
        Pair(
            "You define the rules",
            "Choose what to track — temperature, pain, photos, connected devices. You're in full control of your own tracking plan."
        ),
        Pair(
            "Evidence-based tracking",
            "We prepare the best tracking schedule based on thousands of medical records, optimized for your next appointment."
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
                LpmPrimaryButton(
                    text = if (currentSlide == slides.size - 1) "Get started" else "Next",
                    onClick = {
                        if (currentSlide < slides.size - 1) {
                            currentSlide++
                        } else {
                            onFinish()
                        }
                    }
                )
                
                if (currentSlide < slides.size - 1) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Skip — explore the app",
                            color = Gray500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp)) // Maintain height when skip is gone
                }
            }
        }
    }
}
