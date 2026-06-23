package com.example.canteen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canteen.data.CloudScan
import com.example.canteen.data.db.ScanEvent
import com.example.canteen.ui.theme.AppAccent
import com.example.canteen.ui.theme.AppBackground
import com.example.canteen.ui.theme.AppBorder
import com.example.canteen.ui.theme.AppMuted
import com.example.canteen.ui.theme.AppSurface
import com.example.canteen.ui.theme.AppText
import com.example.canteen.ui.theme.SuccessGreen
import com.example.canteen.ui.theme.ErrorRed
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

    val admitted = displayScans.count { it.result == "SUCCESS" || it.result == "BONUS" }
    val denied = displayScans.count { it.result == "DENIED" }

    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Custom top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppSurface)
                        .border(1.dp, AppBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u2190", color = AppMuted, fontSize = 18.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today's Users",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppText
                )
                Text(
                    text = "${displayScans.size} entries",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppMuted
                )
            }
            Icon(
                imageVector = if (isCloudSync) Icons.Default.Cloud else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (isCloudSync) SuccessGreen else AppMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        if (displayScans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AppSurface)
                            .border(1.dp, AppBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\ud83d\udccb", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No scans yet today",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppMuted
                    )
                    Text(
                        text = "Resets at midnight",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppMuted.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxSize()
            ) {
                item {
                    // Summary row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryBox(
                            value = admitted,
                            label = "admitted",
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryBox(
                            value = denied,
                            label = "denied",
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(displayScans, key = { "${it.name}_${it.timestamp}" }) { scan ->
                    UserCard(scan = scan, timeFmt = timeFmt)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryBox(value: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppMuted
                )
            }
        }
    }
}

@Composable
private fun UserCard(scan: DisplayScan, timeFmt: SimpleDateFormat) {
    val dotColor = when (scan.result) {
        "SUCCESS", "BONUS" -> SuccessGreen
        else -> ErrorRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scan.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppText
                )
                if (scan.company.isNotBlank()) {
                    Text(
                        text = scan.company,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppMuted
                    )
                }
                if (scan.result == "DENIED" && scan.reason != null) {
                    Text(
                        text = scan.reason.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed.copy(alpha = 0.8f)
                    )
                }
            }
            Text(
                text = timeFmt.format(Date(scan.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = AppMuted
            )
        }
    }
}
