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
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = screen == AppScreen.Home || screen == AppScreen.Journey || screen == AppScreen.Profile,
        drawerContent = {
            ModalDrawerSheet(
                containerColor = com.livingpatientmemory.ui.theme.White
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "LIVING PATIENT MEMORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray400,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    letterSpacing = 1.sp
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text("Active Trackings") },
                    selected = screen == AppScreen.Home,
                    onClick = {
                        screen = AppScreen.Home
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text("My Profile") },
                    selected = screen == AppScreen.Profile,
                    onClick = {
                        screen = AppScreen.Profile
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
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
                topBar = {
                    LpmTopBar(
                        title = "My Trackings",
                        onOpenDrawer = { scope.launch { drawerState.open() } }
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
                topBar = {
                    LpmTopBar(
                        title = "Profile",
                        onOpenDrawer = { scope.launch { drawerState.open() } }
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
                        onStartRoutine = { screen = AppScreen.Routine },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LpmTopBar(
    title: String,
    onOpenDrawer: () -> Unit
) {
    androidx.compose.material3.TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
            androidx.compose.material3.IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menu")
            }
        },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = com.livingpatientmemory.ui.theme.White,
            titleContentColor = com.livingpatientmemory.ui.theme.Black
        )
    )
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
