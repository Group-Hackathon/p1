package com.livingpatientmemory

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livingpatientmemory.data.AuthHelper
import com.livingpatientmemory.data.SessionManager
import com.livingpatientmemory.ui.screens.*
import com.livingpatientmemory.ui.theme.Black
import com.livingpatientmemory.ui.theme.Gray400
import com.livingpatientmemory.ui.theme.LivingPatientMemoryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            val ok = AuthHelper.ensureAuthenticated()
            Log.d("LPM_APP", if (ok) "Auth OK" else "Auth failed")
        }

        setContent {
            LivingPatientMemoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}

private enum class AppScreen {
    Splash, Welcome, Home, NewFollowUp, Journey, Routine, Notifications, Profile
}

@Composable
private fun AppRoot() {
    var screen by remember { mutableStateOf(AppScreen.Splash) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var selectedFollowUp by remember { mutableStateOf<FollowUpUi?>(null) }
    var hasSeenWelcome by remember { mutableStateOf(SessionManager.getToken() != null) }

    LaunchedEffect(screen) {
        if (screen == AppScreen.Splash) {
            delay(1400)
            screen = if (hasSeenWelcome) AppScreen.Home else AppScreen.Welcome
        }
    }

    when (screen) {
        AppScreen.Splash -> SplashScreen()

        AppScreen.Welcome -> WelcomeScreen(
            onStartTracking = {
                hasSeenWelcome = true
                screen = AppScreen.NewFollowUp
            },
            onGoToHome = {
                hasSeenWelcome = true
                screen = AppScreen.Home
            }
        )

        AppScreen.Home -> Scaffold(
            bottomBar = {
                LpmTabBar(
                    currentScreen = screen,
                    onNavigate = { screen = it }
                )
            }
        ) { padding ->
            DashboardScreen(
                refreshKey = refreshKey,
                onNewFollowUp = { screen = AppScreen.NewFollowUp },
                onOpenJourney = { followUp ->
                    selectedFollowUp = followUp
                    screen = AppScreen.Journey
                },
                onOpenNotifications = { screen = AppScreen.Notifications },
                modifier = Modifier.padding(padding)
            )
        }

        AppScreen.Profile -> Scaffold(
            bottomBar = {
                LpmTabBar(
                    currentScreen = screen,
                    onNavigate = { screen = it }
                )
            }
        ) { padding ->
            ProfileScreen(
                onBack = { screen = AppScreen.Home },
                onLogout = {
                    hasSeenWelcome = false
                    screen = AppScreen.Splash
                },
                modifier = Modifier.padding(padding)
            )
        }

        AppScreen.NewFollowUp -> OnboardingScreen(
            onBack = { screen = AppScreen.Home },
            onFollowUpCreated = {
                refreshKey++
                screen = AppScreen.Home
            }
        )

        AppScreen.Journey -> {
            val followUp = selectedFollowUp
            if (followUp == null) {
                screen = AppScreen.Home
            } else {
                JourneyScreen(
                    followUp = followUp,
                    onBack = { screen = AppScreen.Home },
                    onStartRoutine = { screen = AppScreen.Routine }
                )
            }
        }

        AppScreen.Routine -> DailyRoutineScreen(
            followUpTitle = selectedFollowUp?.title ?: "Daily Routine",
            onBack = { screen = AppScreen.Journey },
            onComplete = {
                refreshKey++
                screen = AppScreen.Home
            }
        )

        AppScreen.Notifications -> NotificationsScreen(
            onBack = { screen = AppScreen.Home }
        )
    }
}

@Composable
private fun LpmTabBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = com.livingpatientmemory.ui.theme.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentScreen == AppScreen.Home,
            onClick = { onNavigate(AppScreen.Home) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = com.livingpatientmemory.ui.theme.Black,
                unselectedIconColor = com.livingpatientmemory.ui.theme.Gray400,
                indicatorColor = com.livingpatientmemory.ui.theme.Gray200
            )
        )
        NavigationBarItem(
            icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentScreen == AppScreen.Profile,
            onClick = { onNavigate(AppScreen.Profile) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = com.livingpatientmemory.ui.theme.Black,
                unselectedIconColor = com.livingpatientmemory.ui.theme.Gray400,
                indicatorColor = com.livingpatientmemory.ui.theme.Gray200
            )
        )
    }
}

@Composable
private fun SplashScreen() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600)),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "P1",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    color = Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LIVING PATIENT MEMORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp,
                    color = Gray400
                )
            }
        }
    }
}
