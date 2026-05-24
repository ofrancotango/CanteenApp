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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val isCloudConnected = cloudScans.isNotEmpty() || true
    val displayScans: List<DisplayScan> = if (cloudScans.isNotEmpty()) {
        cloudScans.map {
            DisplayScan(
                name = it.name,
                company = it.company,
                result = it.result,
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
                timestamp = it.timestamp,
                isCloud = false
            )
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val cloudIcon = if (cloudScans.isNotEmpty()) Icons.Default.Cloud else Icons.Default.CloudOff
    val cloudTint = if (cloudScans.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFF9E9E9E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Today's Users")
                        Text(
                            text = "${displayScans.size} scanned today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                        imageVector = cloudIcon,
                        contentDescription = if (cloudScans.isNotEmpty()) "Cloud synced" else "Local only",
                        tint = cloudTint,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        if (displayScans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No scans yet today",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This list resets at midnight",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    SummaryBar(scans = displayScans)
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(displayScans, key = { "${it.name}_${it.timestamp}" }) { scan ->
                    TodayUserCard(scan = scan, timeFormat = timeFormat)
                }
            }
        }
    }
}

@Composable
private fun SummaryBar(scans: List<DisplayScan>) {
    val successCount = scans.count { it.result == "SUCCESS" }
    val bonusCount = scans.count { it.result == "BONUS" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SummaryChip(label = "Admitted", value = successCount.toString(), color = Color(0xFF4CAF50))
        SummaryChip(label = "Bonus", value = bonusCount.toString(), color = Color(0xFFFF9800))
        SummaryChip(label = "Total", value = scans.size.toString(), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SummaryChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun TodayUserCard(scan: DisplayScan, timeFormat: SimpleDateFormat) {
    val resultColor = when (scan.result) {
        "SUCCESS" -> Color(0xFF4CAF50)
        "BONUS" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(resultColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scan.name,
                    style = MaterialTheme.typography.titleSmall
                )
                if (scan.company.isNotBlank()) {
                    Text(
                        text = scan.company,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeFormat.format(Date(scan.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = scan.result,
                    style = MaterialTheme.typography.labelSmall,
                    color = resultColor
                )
            }
        }
    }
}
