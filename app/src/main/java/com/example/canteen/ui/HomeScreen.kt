package com.example.canteen.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    scansToday: Int,
    scanStatus: String,
    onScanClick: () -> Unit,
    onStatsClick: () -> Unit,
    onTodayUsersClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    var showDebugDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    if (showDebugDialog) {
        AlertDialog(
            onDismissRequest = { showDebugDialog = false },
            title = { Text("Debug Info") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text(scanStatus)
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(scanStatus))
                        Toast.makeText(context, "Information copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copy")
                    }
                    TextButton(onClick = { showDebugDialog = false }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onRefreshClick()
                    showDebugDialog = false
                }) {
                    Text("Force Refresh")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onAdminClick) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = { showDebugDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Debug Info"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Canteen Access",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onScanClick,
                modifier = Modifier
                    .size(220.dp)
                    .padding(8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SCAN",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Scans Today: $scansToday",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onTodayUsersClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Today's Users")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onStatsClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("View Stats")
            }
        }
    }
}
