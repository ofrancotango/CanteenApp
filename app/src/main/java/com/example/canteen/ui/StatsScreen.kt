package com.example.canteen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canteen.data.AccessRepository
import com.example.canteen.data.db.DailyStats
import com.example.canteen.ui.theme.AppAccent
import com.example.canteen.ui.theme.AppAccentLight
import com.example.canteen.ui.theme.AppBackground
import com.example.canteen.ui.theme.AppBorder
import com.example.canteen.ui.theme.AppMuted
import com.example.canteen.ui.theme.AppSurface
import com.example.canteen.ui.theme.AppText
import com.example.canteen.ui.theme.AppWhite
import com.example.canteen.ui.theme.SuccessGreen
import com.example.canteen.ui.theme.ErrorRed

@Composable
fun StatsScreen(
    stats: Map<String, Any>,
    expectedAttendance: Int,
    repository: AccessRepository,
    onBackClick: () -> Unit
) {
    var dailyHistory by remember { mutableStateOf<List<DailyStats>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) { dailyHistory = repository.getDailyHistory() }

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
            Column {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppText
                )
                Text(
                    text = "Last 30 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppMuted
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Today's stat cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Total Scans",
                        value = stats["total_scans_today"]?.toString() ?: "0",
                        modifier = Modifier.weight(1f),
                        gradient = listOf(AppAccent, AppAccentLight)
                    )
                    StatCard(
                        label = "Unique Users",
                        value = stats["unique_users_served"]?.toString() ?: "0",
                        modifier = Modifier.weight(1f),
                        gradient = listOf(AppAccent, AppAccentLight)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                StatCard(
                    label = "Expected Attendance",
                    value = expectedAttendance.toString(),
                    modifier = Modifier.fillMaxWidth(),
                    gradient = listOf(AppAccent, AppAccentLight)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Export button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppSurface)
                        .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                try {
                                    val csvData = repository.exportLogs()
                                    val filename = "scan_logs_${System.currentTimeMillis()}.csv"
                                    val file = java.io.File(context.cacheDir, filename)
                                    file.writeText(csvData)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context, "${context.packageName}.provider", file
                                    )
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Export Logs"))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "\u2193",
                            fontSize = 20.sp,
                            color = AppAccent
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Export Logs",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = AppText
                            )
                            Text(
                                text = "Download CSV file",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppMuted
                            )
                        }
                        Text(
                            text = "CSV",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppAccent
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // History header
            item {
                Text(
                    text = "HISTORY",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    color = AppMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // History items
            items(dailyHistory) { item ->
                HistoryRow(item)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    gradient: List<androidx.compose.ui.graphics.Color>
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .padding(vertical = 2.dp)
        ) {
            // gradient line on top
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppText
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AppMuted
            )
        }
    }
}

@Composable
private fun HistoryRow(stats: DailyStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stats.date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppText
                )
                Text(
                    text = "${stats.uniqueUsers} unique users",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppAccent.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${stats.totalScans} scans",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
