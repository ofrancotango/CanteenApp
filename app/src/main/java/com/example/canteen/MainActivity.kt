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
import com.example.canteen.ui.Area2ManagerScreen
import com.example.canteen.ui.CompanyRulesScreen
import com.example.canteen.ui.HomeScreen
import com.example.canteen.ui.QRScannerScreen
import com.example.canteen.ui.ResultScreen
import com.example.canteen.ui.ServiceDisabledScreen
import com.example.canteen.ui.NoteInputScreen
import com.example.canteen.ui.NoteTakenScreen
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
        androidx.work.WorkManager.getInstance(applicationContext)
            .cancelUniqueWork("daily_canteen_report_periodic")
        EmailAlarmScheduler.schedule(applicationContext)
    }
}

enum class Screen {
    HOME,
    SCANNER,
    RESULT,
    NOTE_INPUT,
    NOTE_TAKEN,
    STATS,
    TODAY_USERS,
    WHITELIST_MANAGER,
    COMPANY_RULES,
    AREA2_MANAGER
}

private const val ADMIN_PIN = "6767"

// Area 2 purple accent
private val Area2ButtonColor = Color(0xFF7C3AED)

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

    // ── Area 2 state ─────────────────────────────────────────────────────────
    // Mode is LOCAL to this device only (SharedPrefs). The employee list syncs via Firebase.
    val area2Employees by firebaseRepo.area2Employees.collectAsState()

    // Initialised from sticky SharedPrefs — survives restarts and offline use.
    var isArea2Mode by remember { mutableStateOf(repository.isArea2Mode) }

    // Sync area2 employee list into repository
    LaunchedEffect(area2Employees) {
        repository.applyArea2Employees(area2Employees)
    }

    var expectedDay   by remember { mutableStateOf(0) }
    var expectedNight by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        val (d, n) = repository.getExpectedAttendanceByShift()
        expectedDay   = d
        expectedNight = n
    }

    fun shiftFor(ts: Long): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = ts
        val minutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        return if (minutes in 360 until 900) "DAY" else "NIGHT"
    }

    val allowedCompanies by firebaseRepo.allowedCompanies.collectAsState()
    val forbiddenCompanies by firebaseRepo.forbiddenCompanies.collectAsState()
    val forbiddenEmployees by firebaseRepo.forbiddenEmployees.collectAsState()
    val manualEmployees by firebaseRepo.manualEmployees.collectAsState()

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

    // Day boundary timer
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
        }, 60000L, 60000L)
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
            isArea2Mode = isArea2Mode,
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
            onToggleArea2Mode = { enable ->
                // Local only — only this device switches mode
                repository.isArea2Mode = enable
                isArea2Mode = enable
            },
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
            onOpenArea2List = {
                showAdminDialog = false
                adminAuthenticated = false
                pinInput = ""
                currentScreen = Screen.AREA2_MANAGER
            },
            onSendTestEmail = {
                coroutineScope.launch {
                    try {
                        android.widget.Toast.makeText(context, "Sending email...", android.widget.Toast.LENGTH_SHORT).show()
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
                            android.widget.Toast.makeText(context, "No scans today, nothing to send.", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                EmailSender.sendDailyReport(context, events)
                            }
                            android.widget.Toast.makeText(context, "Email sent successfully!", android.widget.Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Email error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
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

    // Counts are isolated per area: in Area 2 only AREA2 scans count, otherwise only MAIN scans.
    val currentArea = if (isArea2Mode) "AREA2" else "MAIN"
    val areaCloudScans = cloudScans.filter { it.area == currentArea }

    val todayCloudAdmittedCount = areaCloudScans.count { it.result == "SUCCESS" || it.result == "BONUS" }
        .takeIf { cloudScans.isNotEmpty() } ?: todayAdmittedCount

    val todayCloudDayCount   = areaCloudScans.count { (it.result == "SUCCESS" || it.result == "BONUS") && shiftFor(it.timestamp) == "DAY"   }
        .takeIf { cloudScans.isNotEmpty() } ?: 0
    val todayCloudNightCount = areaCloudScans.count { (it.result == "SUCCESS" || it.result == "BONUS") && shiftFor(it.timestamp) == "NIGHT" }
        .takeIf { cloudScans.isNotEmpty() } ?: 0

    val currentShift         = repository.getCurrentShift()
    val currentShiftCount    = if (currentShift == "DAY") todayCloudDayCount   else todayCloudNightCount
    val currentShiftExpected = if (currentShift == "DAY") expectedDay           else expectedNight

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                scansToday = if (isArea2Mode) todayCloudAdmittedCount else currentShiftCount,
                scanStatus = "$scanStatus (Last Err: ${repository.lastError ?: "None"})",
                expectedAttendance = currentShiftExpected,
                deniedCount = todayDeniedCount,
                dayCount = if (isArea2Mode) todayCloudAdmittedCount else todayCloudDayCount,
                nightCount = if (isArea2Mode) 0 else todayCloudNightCount,
                isArea2Mode = isArea2Mode,
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
                    val targetTs = noteScanTargetTimestamp
                    if (targetTs != null) {
                        noteScanTargetTimestamp = null
                        scope.launch {
                            repository.addNoteToJimCateringScan(targetTs, code)
                            currentScreen = Screen.NOTE_TAKEN
                        }
                        return@QRScannerScreen
                    }
                    val result = repository.verifyAccess(code)
                    when (result) {
                        is VerificationResult.Success -> {
                            val matchedName = result.matchedName.removeSuffix(" (BONUS)")
                            val isBonus = result.matchedName.endsWith("(BONUS)")
                            val company = repository.getWhitelistRawList()
                                .firstOrNull { it.name.equals(matchedName, ignoreCase = true) }
                                ?.company ?: ""
                            firebaseRepo.pushScan(
                                name = matchedName,
                                company = company,
                                result = if (isBonus) "BONUS" else "SUCCESS",
                                timestamp = result.timestamp,
                                deviceId = deviceId,
                                area = currentArea
                            )
                        }
                        is VerificationResult.Failure -> {
                            firebaseRepo.pushScan(
                                name = result.scannedName,
                                company = result.company ?: "",
                                result = "DENIED",
                                timestamp = result.timestamp,
                                deviceId = deviceId,
                                area = currentArea
                            )
                        }
                    }
                    lastResult = result
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
                scanCount = todayCloudAdmittedCount
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
                            currentScreen = Screen.SCANNER
                        }
                    },
                    onSkip = {
                        lastResult = null
                        currentScreen = Screen.SCANNER
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
        Screen.NOTE_TAKEN -> {
            NoteTakenScreen(
                onNextClick = { currentScreen = Screen.SCANNER },
                onHomeClick = { currentScreen = Screen.HOME }
            )
        }
        Screen.STATS -> {
            val stats = repository.getStats()
            StatsScreen(
                stats = stats,
                expectedDay = expectedDay,
                expectedNight = expectedNight,
                repository = repository,
                onBackClick = { currentScreen = Screen.HOME }
            )
        }
        Screen.TODAY_USERS -> {
            TodayUsersScreen(
                localScans = todayLocalScans,
                cloudScans = areaCloudScans,
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
        Screen.AREA2_MANAGER -> {
            Area2ManagerScreen(
                employees = area2Employees,
                onAddEmployee = { name, company ->
                    firebaseRepo.addArea2Employee(name, company)
                },
                onRemoveEmployee = { key ->
                    firebaseRepo.removeArea2Employee(key)
                },
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
    isArea2Mode: Boolean,
    onPinChange: (String) -> Unit,
    onPinSubmit: () -> Unit,
    onToggleApp: (Boolean) -> Unit,
    onToggleArea2Mode: (Boolean) -> Unit,
    onOpenWhitelist: () -> Unit,
    onOpenCompanyRules: () -> Unit,
    onOpenArea2List: () -> Unit,
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
                        Text("\uD83D\uDC64  Manage Manual Whitelist")
                    }

                    // Company Rules button
                    Button(
                        onClick = onOpenCompanyRules,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                    ) {
                        Text("\uD83C\uDFE2  Manage Company Rules")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ── Area 2 section ──────────────────────────────────────
                    // Area 2 Mode toggle button
                    Button(
                        onClick = { onToggleArea2Mode(!isArea2Mode) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isArea2Mode) Color(0xFF7C3AED) else Color(0xFF6B7280)
                        )
                    ) {
                        Text(
                            if (isArea2Mode) "🏠  Exit Area 2 Mode" else "🏠  Area 2 Mode"
                        )
                    }
                    Text(
                        text = if (isArea2Mode)
                            "⚠️ Questo dispositivo è in modalità Area 2. Tap per uscire."
                        else
                            "Attiva solo su questo dispositivo. Persiste anche dopo il riavvio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isArea2Mode) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    // Area 2 List button
                    Button(
                        onClick = onOpenArea2List,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("📋  Area 2 List")
                    }
                    Text(
                        text = "Manage the people authorised for the secondary canteen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Test Email button
                    Button(
                        onClick = onSendTestEmail,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF10B981))
                    ) {
                        Text("\u2709\uFE0F  Send Test Email")
                    }
                    Text(
                        text = "Send today's report immediately to verify it works.",
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
