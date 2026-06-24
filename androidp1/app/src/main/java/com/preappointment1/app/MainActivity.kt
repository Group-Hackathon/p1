package com.preappointment1.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.preappointment1.app.R
import com.preappointment1.app.data.AuthHelper
import com.preappointment1.app.data.SessionManager
import com.preappointment1.app.data.api.ApiClient
import com.preappointment1.app.billing.BillingManager
import com.preappointment1.app.ui.screens.*
import com.preappointment1.app.ui.theme.Black
import com.preappointment1.app.ui.theme.Gray200
import com.preappointment1.app.ui.theme.Gray400
import com.preappointment1.app.ui.theme.White
import com.preappointment1.app.ui.theme.LivingPatientMemoryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)
        BillingManager.initialize(this)

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
    Splash, Welcome, Home, NewFollowUp, Journey, Routine, Notifications, Profile, Report
}

@Composable
private fun AppRoot() {
    var screen by remember { mutableStateOf(AppScreen.Splash) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var selectedFollowUp by remember { mutableStateOf<FollowUpUi?>(null) }
    var hasSeenWelcome by remember { mutableStateOf(SessionManager.getToken() != null) }
    var pendingFollowUpId by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var followUps by remember { mutableStateOf<List<FollowUpUi>>(emptyList()) }

    LaunchedEffect(refreshKey) {
        if (!hasSeenWelcome) return@LaunchedEffect
        try {
            val subscriptions = ApiClient.apiService.getSubscriptions()
            val agents = ApiClient.apiService.getAgents().associateBy { it.id }
            followUps = subscriptions.map { it.toFollowUpUi(agents) }
            
            if (pendingFollowUpId != null) {
                val found = followUps.find { it.id == pendingFollowUpId }
                if (found != null) {
                    selectedFollowUp = found
                    screen = AppScreen.Journey
                }
                pendingFollowUpId = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        delay(1500)
        screen = if (hasSeenWelcome) AppScreen.Home else AppScreen.Welcome
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = screen == AppScreen.Home || screen == AppScreen.Journey || screen == AppScreen.Profile,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = White,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    // Profile Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(com.preappointment1.app.ui.theme.Gray200)
                            .clickable {
                                screen = AppScreen.Profile
                                scope.launch { drawerState.close() }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("P", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.patient_name_placeholder), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Black)
                            Text(stringResource(R.string.view_profile), fontSize = 12.sp, color = com.preappointment1.app.ui.theme.Gray600)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.my_trackings),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.preappointment1.app.ui.theme.Gray400,
                        letterSpacing = 1.sp
                    )
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(followUps) { followUp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFollowUp = followUp
                                        screen = AppScreen.Journey
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (followUp.isActive) Black else Gray200, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = followUp.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Black
                                )
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        screen = AppScreen.NewFollowUp
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.AddCircle, contentDescription = "Add", tint = Gray400, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.start_new_tracking),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Gray400
                                )
                            }
                        }
                    }
                }
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
                    MainTopBar(
                        title = stringResource(R.string.app_name),
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        hasPendingTasks = false, // could be dynamic later
                        onOpenNotifications = { screen = AppScreen.Notifications }
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
                    MainTopBar(
                        title = stringResource(R.string.profile_title),
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            ) { padding ->
                ProfileScreen(
                    onBack = { screen = AppScreen.Home },
                    onLogout = {
                        hasSeenWelcome = false
                        screen = AppScreen.Welcome
                    },
                    modifier = Modifier.padding(padding)
                )
            }

            AppScreen.NewFollowUp -> OnboardingScreen(
                onBack = { screen = AppScreen.Home },
                onFollowUpCreated = { newId ->
                    pendingFollowUpId = newId
                    refreshKey++
                }
            )

            AppScreen.Journey -> {
                val followUp = selectedFollowUp
                if (followUp == null) {
                    screen = AppScreen.Home
                } else {
                    JourneyScreen(
                        followUp = followUp,
                        onBack = {
                            refreshKey++
                            screen = AppScreen.Home
                        },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenReport = { screen = AppScreen.Report },
                        onFollowUpUpdated = { updated ->
                            selectedFollowUp = updated
                            // Also update the list
                            followUps = followUps.map { if (it.id == updated.id) updated else it }
                        }
                    )
                }
            }

            AppScreen.Routine -> {
                val followUp = selectedFollowUp
                if (followUp == null) {
                    screen = AppScreen.Home
                } else {
                    DailyRoutineScreen(
                        followUpTitle = followUp.title,
                        rules = followUp.rules,
                        onBack = { screen = AppScreen.Journey },
                        onComplete = {
                            refreshKey++
                            screen = AppScreen.Home
                        }
                    )
                }
            }

            AppScreen.Notifications -> NotificationsScreen(
                onBack = { screen = AppScreen.Home }
            )

            AppScreen.Report -> {
                val followUp = selectedFollowUp
                if (followUp == null) {
                    screen = AppScreen.Journey
                } else {
                    ReportScreen(
                        followUp = followUp,
                        onBack = { screen = AppScreen.Journey }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    title: String,
    onOpenDrawer: () -> Unit,
    hasPendingTasks: Boolean = false,
    onOpenNotifications: (() -> Unit)? = null
) {
    androidx.compose.material3.TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-1).sp) },
        navigationIcon = {
            androidx.compose.material3.IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            if (onOpenNotifications != null) {
                Box {
                    androidx.compose.material3.IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = com.preappointment1.app.ui.theme.Black)
                    }
                    if (hasPendingTasks) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .padding(top = 12.dp, end = 12.dp)
                                .background(com.preappointment1.app.ui.theme.Black, CircleShape)
                        )
                    }
                }
            }
        },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = com.preappointment1.app.ui.theme.White,
            titleContentColor = com.preappointment1.app.ui.theme.Black
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
                    text = stringResource(R.string.app_name),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    color = Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.pre_appointment),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp,
                    color = Gray400
                )
            }
        }
    }
}
