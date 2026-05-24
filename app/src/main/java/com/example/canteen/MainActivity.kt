package com.example.canteen

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.canteen.data.AccessRepository
import com.example.canteen.data.FirebaseSyncRepository
import com.example.canteen.data.VerificationResult
import com.example.canteen.ui.HomeScreen
import com.example.canteen.ui.QRScannerScreen
import com.example.canteen.ui.ResultScreen
import com.example.canteen.ui.ServiceDisabledScreen
import com.example.canteen.ui.StatsScreen
import com.example.canteen.ui.TodayUsersScreen
import com.example.canteen.ui.theme.CanteenTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: AccessRepository
    private lateinit var firebaseRepo: FirebaseSyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = AccessRepository(applicationContext)
        firebaseRepo = FirebaseSyncRepository()
        firebaseRepo.startListening()

        lifecycleScope.launch {
            repository.refreshWhitelist()
        }

        setContent {
            CanteenTheme {
                AppNavigation(repository, firebaseRepo)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseRepo.stopListening()
    }
}

enum class Screen {
    HOME,
    SCANNER,
    RESULT,
    STATS,
    TODAY_USERS
}

// Admin PIN - change this to whatever you want
private const val ADMIN_PIN = "0000"

@Composable
fun AppNavigation(repository: AccessRepository, firebaseRepo: FirebaseSyncRepository) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var lastResult by remember { mutableStateOf<VerificationResult?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current

    val deviceId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    androidx.activity.compose.BackHandler(enabled = currentScreen != Screen.HOME) {
        currentScreen = Screen.HOME
    }

    val scanStatus by repository.lastFetchStatus.collectAsState(initial = "Idle")
    val currentScans by repository.currentScanCount.collectAsState(initial = 0)
    val isAppEnabled by firebaseRepo.isAppEnabled.collectAsState()
    val cloudScans by firebaseRepo.todayCloudScans.collectAsState()

    val todayLocalScans by repository.todayScans.collectAsState(initial = emptyList())

    var showAdminDialog by remember { mutableStateOf(false) }
    var adminAuthenticated by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { firebaseRepo.refreshTodayListenerIfNeeded() }
    }

    if (showAdminDialog) {
        AdminDialog(
            authenticated = adminAuthenticated,
            pinInput = pinInput,
            pinError = pinError,
            isAppEnabled = isAppEnabled,
            onPinChange = { pinInput = it; pinError = false },
            onPinSubmit = {
                if (pinInput == ADMIN_PIN) {
                    adminAuthenticated = true
                    pinError = false
                } else {
                    pinError = true
                }
            },
            onToggleApp = { firebaseRepo.setAppEnabled(it) },
            onDismiss = {
                showAdminDialog = false
                adminAuthenticated = false
                pinInput = ""
                pinError = false
            }
        )
    }

    if (!isAppEnabled) {
        ServiceDisabledScreen(
            onAdminUnlock = { showAdminDialog = true }
        )
        return
    }

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                scansToday = currentScans,
                scanStatus = "$scanStatus (Last Err: ${repository.lastError ?: "None"})",
                onScanClick = { currentScreen = Screen.SCANNER },
                onStatsClick = { currentScreen = Screen.STATS },
                onTodayUsersClick = { currentScreen = Screen.TODAY_USERS },
                onRefreshClick = {
                    scope.launch {
                        repository.refreshWhitelist()
                    }
                },
                onAdminClick = { showAdminDialog = true }
            )
        }
        Screen.SCANNER -> {
            QRScannerScreen(
                onQrCodeScanned = { code ->
                    val result = repository.verifyAccess(code)
                    if (result is VerificationResult.Success) {
                        val matchedName = result.matchedName.removeSuffix(" (BONUS)")
                        val isBonus = result.matchedName.endsWith("(BONUS)")
                        val company = repository.getWhitelistRawList()
                            .firstOrNull { it.name.equals(matchedName, ignoreCase = true) }
                            ?.company ?: ""
                        firebaseRepo.pushScan(
                            name = matchedName,
                            company = company,
                            result = if (isBonus) "BONUS" else "SUCCESS",
                            timestamp = System.currentTimeMillis(),
                            deviceId = deviceId
                        )
                    }
                    lastResult = result
                    currentScreen = Screen.RESULT
                },
                onCancel = { currentScreen = Screen.HOME },
                scanCount = currentScans
            )
        }
        Screen.RESULT -> {
            lastResult?.let { result ->
                ResultScreen(
                    result = result,
                    onNextClick = { currentScreen = Screen.SCANNER },
                    onHomeClick = { currentScreen = Screen.HOME }
                )
            }
        }
        Screen.STATS -> {
            val stats = repository.getStats()
            StatsScreen(
                stats = stats,
                expectedAttendance = repository.getExpectedAttendance(),
                repository = repository,
                onBackClick = { currentScreen = Screen.HOME }
            )
        }
        Screen.TODAY_USERS -> {
            TodayUsersScreen(
                localScans = todayLocalScans,
                cloudScans = cloudScans,
                onBackClick = { currentScreen = Screen.HOME }
            )
        }
    }
}

@Composable
private fun AdminDialog(
    authenticated: Boolean,
    pinInput: String,
    pinError: Boolean,
    isAppEnabled: Boolean,
    onPinChange: (String) -> Unit,
    onPinSubmit: () -> Unit,
    onToggleApp: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin Panel") },
        text = {
            if (!authenticated) {
                Column {
                    Text(
                        "Enter admin PIN to continue",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = onPinChange,
                        label = { Text("PIN") },
                        singleLine = true,
                        isError = pinError,
                        supportingText = if (pinError) {
                            { Text("Incorrect PIN", color = Color.Red) }
                        } else null
                    )
                }
            } else {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "App Status",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isAppEnabled) "Active" else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAppEnabled) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                        Switch(
                            checked = isAppEnabled,
                            onCheckedChange = { onToggleApp(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Toggle the switch to enable or disable the app on ALL devices instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            if (!authenticated) {
                Button(onClick = onPinSubmit) {
                    Text("Confirm")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
