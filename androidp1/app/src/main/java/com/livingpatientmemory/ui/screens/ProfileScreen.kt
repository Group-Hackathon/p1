package com.livingpatientmemory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livingpatientmemory.ui.components.LpmCard
import com.livingpatientmemory.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fake local state for toggles
    var watchConnected by remember { mutableStateOf(false) }
    var thermometerConnected by remember { mutableStateOf(true) }
    var bpConnected by remember { mutableStateOf(false) }
    var scaleConnected by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = White,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                // Header Profile
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Gray50)
                        .padding(top = 48.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            color = White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Alex",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "alex@example.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray600
                    )
                }
            }

            item {
                SectionTitle("CONNECTED DEVICES")
                DeviceRow(
                    emoji = "⌚",
                    name = "Smartwatch",
                    desc = "Steps, heart rate, sleep",
                    checked = watchConnected,
                    onCheckedChange = { watchConnected = it }
                )
                DeviceRow(
                    emoji = "🌡️",
                    name = "Digital thermometer",
                    desc = "Manual temperature entries",
                    checked = thermometerConnected,
                    onCheckedChange = { thermometerConnected = it }
                )
                DeviceRow(
                    emoji = "💓",
                    name = "Blood pressure monitor",
                    desc = "Bluetooth BP device",
                    checked = bpConnected,
                    onCheckedChange = { bpConnected = it }
                )
                DeviceRow(
                    emoji = "⚖️",
                    name = "Connected scale",
                    desc = "Weight tracking",
                    checked = scaleConnected,
                    onCheckedChange = { scaleConnected = it }
                )
            }

            item {
                SectionTitle("PREFERENCES")
                MenuRow(emoji = "🌡️", label = "Temperature unit", value = "°C")
                MenuRow(emoji = "🔔", label = "Reminder times", value = "9am, 7pm")
            }

            item {
                SectionTitle("ACCOUNT")
                MenuRow(emoji = "✏️", label = "Edit profile")
                MenuRow(emoji = "📦", label = "Export my data")
                MenuRow(emoji = "🚪", label = "Sign out", isDestructive = true)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = Gray400,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun DeviceRow(
    emoji: String,
    name: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text = desc, color = Gray500, fontSize = 13.sp)
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

@Composable
private fun MenuRow(
    emoji: String,
    label: String,
    value: String? = null,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO */ }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = if (isDestructive) Color(0xFFEF4444) else Black,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Gray200)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray600
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "›", fontSize = 20.sp, color = Gray400)
    }
}
