package com.example.canteen

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.canteen.work.EmailAlarmScheduler
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.canteen.ui.CompanyRulesScreen
import com.example.canteen.ui.HomeScreen
import com.example.canteen.ui.QRScannerScreen
import com.example.canteen.ui.ResultScreen
import com.example.canteen.ui.ServiceDisabledScreen
import com.example.canteen.ui.NoteInputScreen
import com.example.canteen.ui.StatsScreen
import com.example.canteen.ui.TodayUsersScreen
import com.example.canteen.ui.WhitelistManagerScreen
import com.example.canteen.ui.theme.AppAccent
import com.example.canteen.ui.theme.CanteenTheme
import kotlinx.coroutines.launch
import com.example.canteen.data.db.AppDatabase
import com.example.canteen.work.EmailSender

class MainActivity : ComponentActivity() {

    private lateinit var repository: AccessRepository
    private lateinit var firebaseRepo: FirebaseSyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = AccessRepository(applicationContext)
        firebaseRepo = FirebaseSyncRepository()
        firebaseRepo.startListening()
        scheduleDailyReport()

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

    private fun scheduleDailyReport() {
        // Cancel legacy WorkManager scheduling
        androidx.work.WorkManager.getInstance(applicationContext)
            .cancelUniqueWork("daily_canteen_report_periodic")

        // Use AlarmManager for exact-time delivery (reliable even in Doze/App Standby)
        EmailAlarmScheduler.schedule(applicationContext)
    }
}

enum class Screen {
    HOME,
    SCANNER,
    RESULT,
    NOTE_INPUT,
    STATS,
    TODAY_USERS,
    WHITELIST_MANAGER,
    COMPANY_RULES
}

private const val ADMIN_PIN = "6767"

@Composable
fun AppNavigation(repository: AccessRepository, firebaseRepo: FirebaseSyncRepository) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var lastResult by remember { mutableStateOf<VerificationResult?>(null) }
    var noteScanTargetTimestamp by remember { mutableStateOf<Long?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current

    val deviceId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    androidx.activity.compose.BackHandler(enabled = currentScreen != Screen.HOME) {
        currentScreen = Screen.HOME
    }

    val scanStatus by repository.lastFetchStatus.collectAsState(initial = "Idle")
    val isAppEnabled by firebaseRepo.isAppEnabled.collectAsState()
    val cloudScans by firebaseRepo.todayCloudScans.collectAsState()
    val todayLocalScans by repository.todayScans.collectAsState(initial = emptyList())
    val todayAdmittedCount by repository.todayAdmittedCount.collectAsState(initial = 0)
    val todayDeniedCount by repository.todayDeniedCount.collectAsState(initial = 0)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var expectedAttendance by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        expectedAttendance = repository.getExpectedAttendance()
    }

    // Firebase-synced rules
    val allowedCompanies by firebaseRepo.allowedCompanies.collectAsState()
    val forbiddenCompanies by firebaseRepo.forbiddenCompanies.collectAsState()
    val forbiddenEmployees by firebaseRepo.forbiddenEmployees.collectAsState()
    val manualEmployees by firebaseRepo.manualEmployees.collectAsState()

    // Sync Firebase rules into repository when they change
    LaunchedEffect(allowedCompanies, forbiddenCompanies, forbiddenEmployees) {
        repository.setFirebaseRules(allowedCompanies, forbiddenCompanies, forbiddenEmployees)
    }
    LaunchedEffect(manualEmployees) {
        repository.applyFirebaseManualEmployees(manualEmployees)
    }

    var showAdminDialog by remember { mutableStateOf(false) }
    var adminAuthenticated by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Day boundary timer: refresh stats and Firebase listener at midnight
    DisposableEffect(Unit) {
        var lastDay = repository.getTodayDateString()
        val timer = java.util.Timer()
        timer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                val currentDay = repository.getTodayDateString()
                if (currentDay != lastDay) {
                    lastDay = currentDay
                    repository.refreshTodayStats()
                    firebaseRepo.refreshTodayListenerIfNeeded()
                }
            }
        }, 60000L, 60000L) // check every 60 seconds
        onDispose {
            timer.cancel()
            firebaseRepo.refreshTodayListenerIfNeeded()
        }
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
            onOpenWhitelist = {
                showAdminDialog = false
                adminAuthenticated = false
                pinInput = ""
                currentScreen = Screen.WHITELIST_MANAGER
            },
            onOpenCompanyRules = {
                showAdminDialog = false
                adminAuthenticated = false
                pinInput = ""
                currentScreen = Screen.COMPANY_RULES
            },
            onSendTestEmail = {
                coroutineScope.launch {
                    try {
                        android.widget.Toast.makeText(context, "Mail in invio...", android.widget.Toast.LENGTH_SHORT).show()
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.scanEventDao()
                        val calendar = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        val start = calendar.timeInMillis
                        val end = start + 24 * 60 * 60 * 1000L
                        val events = dao.getEventsByDate(start, end)
                        if (events.isEmpty()) {
                            android.widget.Toast.makeText(context, "Nessuna scansione oggi, niente da inviare.", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                EmailSender.sendDailyReport(context, events)
                            }
                            android.widget.Toast.makeText(context, "Mail inviata con successo!", android.widget.Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Errore invio mail: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            },
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

    // todayAdmittedCount and todayDeniedCount now come from repository Flows (single source of truth from DB)

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                scansToday = todayAdmittedCount,
                scanStatus = "$scanStatus (Last Err: ${repository.lastError ?: "None"})",
                expectedAttendance = expectedAttendance,
                deniedCount = todayDeniedCount,
                onScanClick = { currentScreen = Screen.SCANNER },
                onStatsClick = { currentScreen = Screen.STATS },
                onTodayUsersClick = { currentScreen = Screen.TODAY_USERS },
                onRefreshClick = {
                    scope.launch { repository.refreshWhitelist() }
                },
                onAdminClick = { showAdminDialog = true }
            )
        }
        Screen.SCANNER -> {
            QRScannerScreen(
                onQrCodeScanned = { code ->
                    // If we are in "note scan mode", use this badge as note instead of normal verification
                    val targetTs = noteScanTargetTimestamp
                    if (targetTs != null) {
                        noteScanTargetTimestamp = null
                        scope.launch {
                            repository.addNoteToJimCateringScan(targetTs, code)
                            currentScreen = Screen.HOME
                        }
                        return@QRScannerScreen
                    }
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
                    // If Jim Catering scan, show note input screen
                    currentScreen = if (result is VerificationResult.Success && result.requiresNote) {
                        Screen.NOTE_INPUT
                    } else {
                        Screen.RESULT
                    }
                },
                onCancel = {
                    noteScanTargetTimestamp = null
                    currentScreen = Screen.HOME
                },
                scanCount = todayAdmittedCount
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
        Screen.NOTE_INPUT -> {
            val result = lastResult
            if (result is VerificationResult.Success) {
                NoteInputScreen(
                    scannedName = result.matchedName,
                    onSaveNote = { note ->
                        scope.launch {
                            if (note.isNotBlank()) {
                                repository.addNoteToJimCateringScan(result.timestamp, note)
                            }
                            lastResult = null
                            currentScreen = Screen.HOME
                        }
                    },
                    onSkip = {
                        lastResult = null
                        currentScreen = Screen.HOME
                    },
                    onScanBadgeForNote = {
                        noteScanTargetTimestamp = result.timestamp
                        currentScreen = Screen.SCANNER
                    }
                )
            } else {
                currentScreen = Screen.HOME
            }
        }
        Screen.STATS -> {
            val stats = repository.getStats()
            StatsScreen(
                stats = stats,
                expectedAttendance = expectedAttendance,
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
        Screen.WHITELIST_MANAGER -> {
            WhitelistManagerScreen(
                employees = manualEmployees,
                onAddEmployee = { name, company ->
                    firebaseRepo.addManualEmployee(name, company)
                },
                onRemoveEmployee = { key ->
                    firebaseRepo.removeManualEmployee(key)
                },
                onBackClick = { currentScreen = Screen.HOME }
            )
        }
        Screen.COMPANY_RULES -> {
            CompanyRulesScreen(
                allowedCompanies = allowedCompanies,
                forbiddenCompanies = forbiddenCompanies,
                onAddAllowed = { firebaseRepo.addAllowedCompany(it) },
                onRemoveAllowed = { firebaseRepo.removeAllowedCompany(it) },
                onAddForbidden = { firebaseRepo.addForbiddenCompany(it) },
                onRemoveForbidden = { firebaseRepo.removeForbiddenCompany(it) },
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
    onOpenWhitelist: () -> Unit,
    onOpenCompanyRules: () -> Unit,
    onSendTestEmail: () -> Unit,
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // App Enable/Disable toggle
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
                    Text(
                        text = "Toggle to enable or disable the app on ALL devices instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Whitelist Manager button
                    Button(
                        onClick = onOpenWhitelist,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                    ) {
                        Text("\uD83D\uDC64  Gestisci Whitelist Manuale")
                    }

                    // Company Rules button
                    Button(
                        onClick = onOpenCompanyRules,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                    ) {
                        Text("\uD83C\uDFE2  Gestisci Regole Aziende")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Test Email button
                    Button(
                        onClick = onSendTestEmail,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF10B981))
                    ) {
                        Text("\u2709\uFE0F  Invia Mail Test")
                    }
                    Text(
                        text = "Invia subito la mail di report per verificare che funzioni.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        },
        confirmButton = {
            if (!authenticated) {
                Button(onClick = onPinSubmit) { Text("Confirm") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
