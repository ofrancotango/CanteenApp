package com.example.canteen.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.canteen.data.AccessRepository
import com.example.canteen.data.db.DailyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    stats: Map<String, Any>,
    expectedAttendance: Int,
    repository: AccessRepository,
    onBackClick: () -> Unit
) {
    var dailyHistory by remember { mutableStateOf<List<DailyStats>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        // Load history
        dailyHistory = repository.getDailyHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            
            // Current Stats
            Text(
                text = "Today's Summary",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    StatItem(
                        label = "Total Scans",
                        value = stats["total_scans_today"]?.toString() ?: "0"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    StatItem(
                        label = "Unique Users",
                        value = stats["unique_users_served"]?.toString() ?: "0"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            StatItem(
                label = "Expected Attendance",
                value = expectedAttendance.toString()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            

            
            Spacer(modifier = Modifier.height(16.dp))
            
            // EXPORT LOGS BUTTON
            val context = androidx.compose.ui.platform.LocalContext.current
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            
            Button(
                onClick = {
                    scope.launch(Dispatchers.Main) {
                        try {
                            val csvData = repository.exportLogs()
                            val filename = "scan_logs_${System.currentTimeMillis()}.csv"
                            
                            val file = java.io.File(context.cacheDir, filename)
                            file.writeText(csvData)
                            
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Logs"))
                            
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Show toast or snackbar
                            android.widget.Toast.makeText(context, "Export Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("EXPORT LOGS (CSV)")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Daily History",
                style = MaterialTheme.typography.titleLarge
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(dailyHistory) { item ->
                    HistoryItem(item)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun HistoryItem(stats: DailyStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stats.date,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scans: ${stats.totalScans} | Unique: ${stats.uniqueUsers}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
