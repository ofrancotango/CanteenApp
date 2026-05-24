package com.example.canteen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.canteen.data.CloudScan
import com.example.canteen.data.db.ScanEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DisplayScan(
    val name: String,
    val company: String,
    val result: String,
    val reason: String?,
    val timestamp: Long,
    val isCloud: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayUsersScreen(
    localScans: List<ScanEvent>,
    cloudScans: List<CloudScan>,
    onBackClick: () -> Unit
) {
    val isCloudSync = cloudScans.isNotEmpty()

    val displayScans: List<DisplayScan> = if (isCloudSync) {
        cloudScans.map {
            DisplayScan(
                name = it.name,
                company = it.company,
                result = it.result,
                reason = null,
                timestamp = it.timestamp,
                isCloud = true
            )
        }
    } else {
        localScans.map {
            DisplayScan(
                name = it.matchedName ?: it.scannedCode,
                company = it.company ?: "",
                result = it.result,
                reason = it.reason,
                timestamp = it.timestamp,
                isCloud = false
            )
        }
    }

    val admitted = displayScans.count { it.result == "SUCCESS" }
    val bonus    = displayScans.count { it.result == "BONUS" }
    val denied   = displayScans.count { it.result == "DENIED" }

    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Today's Users", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${displayScans.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Icon(
                        imageVector = if (isCloudSync) Icons.Default.Cloud else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isCloudSync) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.padding(end = 16.dp).size(18.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (displayScans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No scans yet today",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Resets at midnight",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                item {
                    // Summary row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        SummaryPill(value = admitted, label = "admitted", color = Color(0xFF22C55E))
                        SummaryPill(value = bonus, label = "bonus", color = Color(0xFFF59E0B))
                        SummaryPill(value = denied, label = "denied", color = Color(0xFFEF4444))
                    }
                    Divider(color = MaterialTheme.colorScheme.outline)
                }

                items(displayScans, key = { "${it.name}_${it.timestamp}" }) { scan ->
                    ScanRow(scan = scan, timeFmt = timeFmt)
                    Divider(
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 56.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryPill(value: Int, label: String, color: Color) {
    Column {
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun TodayUserCard(scan: DisplayScan, timeFormat: SimpleDateFormat) {
    ScanRow(scan = scan, timeFmt = timeFormat)
}

@Composable
private fun ScanRow(scan: DisplayScan, timeFmt: SimpleDateFormat) {
    val dotColor = when (scan.result) {
        "SUCCESS" -> Color(0xFF22C55E)
        "BONUS"   -> Color(0xFFF59E0B)
        else      -> Color(0xFFEF4444)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scan.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (scan.company.isNotBlank()) {
                Text(
                    text = scan.company,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
            if (scan.result == "DENIED" && scan.reason != null) {
                Text(
                    text = scan.reason.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFEF4444).copy(alpha = 0.7f)
                )
            }
        }
        Text(
            text = timeFmt.format(Date(scan.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
