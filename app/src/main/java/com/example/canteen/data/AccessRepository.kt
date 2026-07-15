package com.example.canteen.data

import android.content.Context
import android.content.SharedPreferences
import com.example.canteen.data.db.AppDatabase
import com.example.canteen.data.db.DailyStats
import com.example.canteen.data.db.ScanEvent
import com.example.canteen.utils.StringNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class AccessRepository(val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("canteen_prefs", Context.MODE_PRIVATE)
    private val db = AppDatabase.getDatabase(context)
    private val statsDao = db.dailyStatsDao()
    private val scanEventDao = db.scanEventDao()
    
    private val fetcher = EmployeeFetcher()
    private val CACHE_FILE = "cached_whitelist_v2.txt" // Storing "Name|Company|FirstName|LastName"
    
    // Whitelist: Map of NormalizedName -> Employee Object
    private var whitelist: Map<String, Employee> = emptyMap()
    
    // Debug properties
    private val _lastFetchStatus = MutableStateFlow("Idle")
    val lastFetchStatus: StateFlow<String> = _lastFetchStatus
    
    // Live Scan Count for UI
    private val _currentScanCount = MutableStateFlow(0)
    val currentScanCount: StateFlow<Int> = _currentScanCount

    // Names of employees added via Whitelist Manager (Firebase manualEmployees node).
    private var manualWhitelistedNames: Set<String> = emptySet()

    // ── Area 2 ──────────────────────────────────────────────────────────────
    // Sticky: persisted in SharedPreferences so it survives restarts without internet.
    var isArea2Mode: Boolean
        get() = prefs.getBoolean("area2_mode_active", false)
        set(value) { prefs.edit().putBoolean("area2_mode_active", value).apply() }

    // Area 2 employee names (normalized) — set from Firebase via applyArea2Employees()
    private var area2EmployeeNames: Set<String> = emptySet()

    fun applyArea2Employees(employees: List<FirebaseEmployee>) {
        area2EmployeeNames = employees.map { it.name.trim().lowercase() }.toSet()
    }

    // Real-time list of ALL today's scans — auto-refreshed via timer in MainActivity
    private val _todayScans = MutableStateFlow<List<ScanEvent>>(emptyList())
    val todayScans: StateFlow<List<ScanEvent>> = _todayScans

    // Real-time daily counts from database — single source of truth
    private val _todayAdmittedCount = MutableStateFlow(0)
    val todayAdmittedCount: StateFlow<Int> = _todayAdmittedCount
    private val _todayDeniedCount = MutableStateFlow(0)
    val todayDeniedCount: StateFlow<Int> = _todayDeniedCount
    private val _todayTotalCount = MutableStateFlow(0)
    val todayTotalCount: StateFlow<Int> = _todayTotalCount

    init {
        // Start collecting DB flows into StateFlows
        GlobalScope.launch(Dispatchers.IO) {
            scanEventDao.getTodayAllScans().collect { _todayScans.value = it }
        }
        GlobalScope.launch(Dispatchers.IO) {
            scanEventDao.getTodayAdmittedCount().collect { _todayAdmittedCount.value = it }
        }
        GlobalScope.launch(Dispatchers.IO) {
            scanEventDao.getTodayDeniedCount().collect { _todayDeniedCount.value = it }
        }
        GlobalScope.launch(Dispatchers.IO) {
            scanEventDao.getTodayTotalCount().collect { _todayTotalCount.value = it }
        }
    }

    var totalEmployees: Int = 0
    var lastError: String? = null

    // Rules — overrideable from Firebase (set via setFirebaseRules)
    private var ALLOWED_COMPANIES: Set<String> = FirebaseSyncRepository.DEFAULT_ALLOWED_COMPANIES
    private var FORBIDDEN_COMPANIES: Set<String> = FirebaseSyncRepository.DEFAULT_FORBIDDEN_COMPANIES
    private var FORBIDDEN_EMPLOYEES: Set<String> = FirebaseSyncRepository.DEFAULT_FORBIDDEN_EMPLOYEES

    fun setFirebaseRules(
        allowed: Set<String>,
        forbidden: Set<String>,
        forbiddenEmployees: Set<String>
    ) {
        if (allowed.isNotEmpty()) ALLOWED_COMPANIES = allowed
        if (forbidden.isNotEmpty()) FORBIDDEN_COMPANIES = forbidden
        if (forbiddenEmployees.isNotEmpty()) FORBIDDEN_EMPLOYEES = forbiddenEmployees
    }

    fun applyFirebaseManualEmployees(employees: List<FirebaseEmployee>) {
        val current = whitelist.toMutableMap()
        employees.forEach { fe ->
            val emp = Employee(fe.name, fe.company, fe.name.split(" ").firstOrNull(), fe.name.split(" ").lastOrNull())
            val norm = com.example.canteen.utils.StringNormalizer.normalize(fe.name)
            if (norm.isNotBlank()) current[norm] = emp
        }
        whitelist = current
        manualWhitelistedNames = employees.map { it.name.trim().lowercase() }.toSet()
    }
    
    // Bonus config
    private val DAILY_BONUS_THRESHOLD = 25

    init {
        checkDailyReset()
        loadFromCache()
        loadCsvData()
        GlobalScope.launch(Dispatchers.IO) {
            syncStatsToDb()
            updateScanCountFlow()
        }
    }

    private fun checkDailyReset() {
        val lastDate = prefs.getString("last_run_date", "")
        val todayDate = getTodayDateString()

        if (lastDate != todayDate) {
            val edit = prefs.edit()
            prefs.all.forEach { (key, _) ->
                if (key.startsWith("count_")) edit.remove(key)
                if (key.startsWith("daily_bonus_used_")) edit.remove(key)
            }
            edit.putString("last_run_date", todayDate)
            edit.apply()
            _currentScanCount.value = 0
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun refreshTodayStats() {
        checkDailyReset()
        GlobalScope.launch(Dispatchers.IO) {
            syncStatsToDb()
            updateScanCountFlow()
        }
    }

    // Day shift: 06:00-15:00, Night shift: 15:00-21:30
    fun getCurrentShift(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val time = hour * 60 + minute
        val dayEnd = 15 * 60
        return if (time in 360..<dayEnd) "DAY" else "NIGHT"
    }

    private fun isActualNightShift(): Boolean {
        val calendar = Calendar.getInstance()
        val time = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val nightStart = 15 * 60
        val nightEnd   = 21 * 60 + 30
        return time in nightStart..<nightEnd
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun loadCsvData() {
        try {
            val assetPath = "technipqrlist/MAX_BADGES00.csv"
            context.assets.open(assetPath).use { stream ->
                val reader = stream.bufferedReader()
                val lines = reader.readLines()
                val currentWhitelist = whitelist.toMutableMap()
                var addedCount = 0

                lines.drop(1).forEach { line ->
                    val parts = line.split(";")
                    if (parts.size >= 3) {
                        val fName = parts[0].trim()
                        val lName = parts[1].trim()
                        val companyMain = parts[2].trim()
                        val companySub = if (parts.size > 3) parts[3].trim() else ""
                        
                        var company = if (companySub.isNotBlank()) companySub else companyMain
                        if (company == "." || company.length < 2) {
                             company = if (companyMain.length > 1) companyMain else ""
                        }
                        
                        val fullName = "$fName $lName"
                        val emp = Employee(name = fullName, company = company, firstName = fName, lastName = lName)
                        val combos = listOf("$fName $lName", "$lName $fName")

                        for (combo in combos) {
                            val norm = StringNormalizer.normalize(combo)
                            if (norm.isNotBlank() && !currentWhitelist.containsKey(norm)) {
                                currentWhitelist[norm] = emp
                                addedCount++
                            }
                        }
                    }
                }
                
                if (addedCount > 0) whitelist = currentWhitelist
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        injectManualUsers()
    }
    
    private fun injectManualUsers() {
        val manualUsers = listOf(
            Employee("Tudor Marian", "ManualWhitelist", "Tudor", "Marian"),
            Employee("Despa Constantin", "ManualWhitelist", "Despa", "Constantin"),
            Employee("Adrian Valeriu", "ManualWhitelist", "Adrian", "Valeriu"),
            Employee("Daniel Ionut Papatoiiu", "ManualWhitelist", "Daniel Ionut", "Papatoiiu"),
            Employee("Andrei Alexandru Ionut Dima", "ManualWhitelist", "Andrei Alexandru Ionut", "Dima"),
            Employee("Andre Fernandes Da sousa Nunes", "ManualWhitelist", "Andre Fernandes", "Da sousa Nunes"),
            Employee("Marius Gabriel Nica", "ManualWhitelist", "Marius Gabriel", "Nica"),
            Employee("Schiano Hugo", "ManualWhitelist", "Schiano", "Hugo"),
            Employee("Sebastian Tomaszkowicz", "ManualWhitelist", "Sebastian", "Tomaszkowicz"),
            // Jim Catering — golden pass, no limits
            Employee("Jim Catering", "JimCatering", "Jim", "Catering")
        )
        
        val currentWhitelist = whitelist.toMutableMap()
        manualUsers.forEach { emp ->
            val rawName = emp.name
            val dotName = "${emp.firstName}.${emp.lastName}".lowercase()
            val normOriginal = StringNormalizer.normalize(rawName)
            if (normOriginal.isNotBlank()) currentWhitelist[normOriginal] = emp
            if (dotName.isNotBlank()) currentWhitelist[dotName] = emp
        }
        whitelist = currentWhitelist
    }
    
    fun loadData(csvInputStream: InputStream) { }

    fun verifyAccess(scannedInput: String): VerificationResult {
        checkDailyReset()
        
        val timestamp = System.currentTimeMillis()
        val cleanedInput = if (scannedInput.contains("_")) scannedInput.substringAfter("_") else scannedInput
        val normalizedInput = StringNormalizer.normalize(cleanedInput)
        
        var employee = whitelist[normalizedInput]
        var isFuzzy = false
        var matchedKey = normalizedInput
        
        if (employee == null) {
            val uniqueEmployees = whitelist.values.distinct()
            for (emp in uniqueEmployees) {
                if (StringNormalizer.smartTokenMatch(scannedInput, emp.name)) {
                    employee = emp
                    isFuzzy = true
                    matchedKey = StringNormalizer.normalize(emp.name)
                    break
                }
            }
        }
        
        val shift = getCurrentShift()
        val currentArea = if (isArea2Mode) "AREA2" else "MAIN"

        // --- 0. NIGHT SHIFT FREE ACCESS (15:00–21:30) ---
        if (isActualNightShift()) {
            GlobalScope.launch(Dispatchers.IO) {
                syncStatsToDb()
                updateScanCountFlow()
                scanEventDao.insert(ScanEvent(
                    timestamp = timestamp,
                    scannedCode = scannedInput,
                    matchedName = employee?.name ?: scannedInput,
                    company = employee?.company ?: "NIGHT_ACCESS",
                    result = "SUCCESS",
                    reason = "NIGHT_FREE_ACCESS",
                    shift = shift,
                    area = currentArea
                ))
            }
            return VerificationResult.Success(
                originalName = scannedInput,
                normalizedName = normalizedInput,
                matchedName = employee?.name ?: scannedInput,
                isFuzzyMatch = isFuzzy,
                timestamp = timestamp
            )
        }

        // --- 1. UNKNOWN USER ---
        if (employee == null) {
            logEvent(timestamp, scannedInput, null, null, "DENIED", "UNKNOWN_USER", currentArea)
            return VerificationResult.Failure(
                VerificationResult.Failure.Reason.UNKNOWN_USER,
                scannedInput,
                timestamp = timestamp
            )
        }

        val company = employee.company.trim()
        val lowerCompany = company.lowercase()

        // ── Jim Catering golden pass check ──────────────────────────────────
        val isUnlimited = company.equals("JimCatering", ignoreCase = true) ||
            employee.name.trim().equals("Jim Catering", ignoreCase = true)

        // ── Area 2 mode: only Area 2 employees (+ Jim Catering) are allowed ─
        if (isArea2Mode) {
            val isInArea2List = area2EmployeeNames.contains(employee.name.trim().lowercase())

            if (!isInArea2List && !isUnlimited) {
                logEvent(timestamp, scannedInput, employee.name, company, "DENIED", "NOT_IN_AREA2", currentArea)
                return VerificationResult.Failure(
                    VerificationResult.Failure.Reason.BLACK_LISTED,
                    scannedInput,
                    company,
                    timestamp = timestamp
                )
            }

            // Jim Catering in Area 2: golden pass
            if (isUnlimited) {
                GlobalScope.launch(Dispatchers.IO) {
                    scanEventDao.insert(ScanEvent(
                        timestamp = timestamp,
                        scannedCode = scannedInput,
                        matchedName = employee!!.name,
                        company = company,
                        result = "SUCCESS",
                        reason = "UNLIMITED",
                        shift = shift,
                        area = currentArea
                    ))
                }
                return VerificationResult.Success(
                    originalName = scannedInput,
                    normalizedName = normalizedInput,
                    matchedName = employee.name,
                    isFuzzyMatch = isFuzzy,
                    requiresNote = true,
                    timestamp = timestamp
                )
            }

            // Area 2 employee: apply daily limit (1/shift) + bonus
            val countKey = "area2_count_${matchedKey}_$shift"
            val currentUsage = prefs.getInt(countKey, 0)
            val allowance = 1

            if (currentUsage < allowance) {
                val newCount = currentUsage + 1
                prefs.edit().putInt(countKey, newCount).apply()
                GlobalScope.launch(Dispatchers.IO) {
                    syncStatsToDb()
                    updateScanCountFlow()
                    scanEventDao.insert(ScanEvent(
                        timestamp = timestamp,
                        scannedCode = scannedInput,
                        matchedName = employee!!.name,
                        company = company,
                        result = "SUCCESS",
                        reason = null,
                        shift = shift,
                        area = currentArea
                    ))
                }
                return VerificationResult.Success(
                    originalName = scannedInput,
                    normalizedName = normalizedInput,
                    matchedName = employee.name,
                    isFuzzyMatch = isFuzzy,
                    timestamp = timestamp
                )
            } else {
                // Bonus for Area 2
                val bonusKey = "area2_bonus_used_$shift"
                val usedBonus = prefs.getInt(bonusKey, 0)
                if (usedBonus < DAILY_BONUS_THRESHOLD) {
                    val newBonus = usedBonus + 1
                    prefs.edit().putInt(bonusKey, newBonus).apply()
                    GlobalScope.launch(Dispatchers.IO) {
                        syncStatsToDb()
                        updateScanCountFlow()
                        scanEventDao.insert(ScanEvent(
                            timestamp = timestamp,
                            scannedCode = scannedInput,
                            matchedName = employee!!.name,
                            company = company,
                            result = "BONUS",
                            reason = "LIMIT_REACHED_BONUS($newBonus/$DAILY_BONUS_THRESHOLD)",
                            shift = shift,
                            area = currentArea
                        ))
                    }
                    return VerificationResult.Success(
                        originalName = scannedInput,
                        normalizedName = normalizedInput,
                        matchedName = employee.name + " (BONUS)",
                        isFuzzyMatch = isFuzzy,
                        timestamp = timestamp
                    )
                } else {
                    logEvent(timestamp, scannedInput, employee.name, company, "DENIED", "LIMIT_REACHED", currentArea)
                    return VerificationResult.Failure(
                        VerificationResult.Failure.Reason.LIMIT_REACHED,
                        scannedInput,
                        timestamp = timestamp
                    )
                }
            }
        }

        // ── Standard (MAIN) mode logic below ────────────────────────────────

        val isManualWhitelisted = company.equals("ManualWhitelist", ignoreCase = true)
            || manualWhitelistedNames.contains(employee.name.trim().lowercase())
        val SPECIAL_WHITELIST = setOf(
            "Cristian De Domenico", "Tudor Marian", "Carlos Ferreira Palhau",
            "Giovanni Giarrizzo", "Chukwudi Joshua Agim", "Despa Constantin",
            "Mohammed Fezaad Khan", "Adrian Valeriu", "Daniel Ionut Papatoiiu",
            "Johan Weesie", "Andrei Alexandru Ionut Dima", "Carlos Vilela",
            "Patrick Flohil", "Andre Fernandes Da sousa Nunes",
            "Marius Gabriel Nica", "Schiano Hugo", "Sebastian Tomaszkowicz",
            "Jim Catering"
        )
        val isSpecialWhitelisted = isManualWhitelisted ||
            SPECIAL_WHITELIST.any { it.equals(employee.name.trim(), ignoreCase = true) }

        // --- 2. BLACKLIST ---
        val isCompanyBlacklisted = FORBIDDEN_COMPANIES.any { it.equals(company, ignoreCase = true) }
        val isEmployeeBlacklisted = FORBIDDEN_EMPLOYEES.any { 
            it.equals(employee.name.trim(), ignoreCase = true) ||
            StringNormalizer.normalize(it) == StringNormalizer.normalize(employee.name) ||
            StringNormalizer.smartTokenMatch(it, employee.name)
        }
        val isBlacklisted = isCompanyBlacklisted || isEmployeeBlacklisted
        
        if (isBlacklisted && !isSpecialWhitelisted) {
             logEvent(timestamp, scannedInput, employee.name, company, "DENIED", "BLACKLISTED", currentArea)
             return VerificationResult.Failure(
                VerificationResult.Failure.Reason.BLACK_LISTED,
                scannedInput,
                company,
                timestamp = timestamp
            )
        }
        
        // --- 3. WHITELIST CHECK ---
        val isWhitelisted = ALLOWED_COMPANIES.any { it.equals(company, ignoreCase = true) }
        
        if (!isWhitelisted && !isSpecialWhitelisted) {
             logEvent(timestamp, scannedInput, employee.name, company, "DENIED", "NOT_WHITELISTED", currentArea)
             return VerificationResult.Failure(
                VerificationResult.Failure.Reason.BLACK_LISTED, 
                scannedInput,
                company,
                timestamp = timestamp
            )
        }

        // --- 4. DAILY LIMITS + BONUS ---
        val countKey = "count_${matchedKey}_$shift"
        val currentUsage = prefs.getInt(countKey, 0)
        val allowance = 1

        if (isUnlimited) {
            // Jim Catering: no daily limit, always admitted
            GlobalScope.launch(Dispatchers.IO) {
                scanEventDao.insert(ScanEvent(
                    timestamp = timestamp,
                    scannedCode = scannedInput,
                    matchedName = employee!!.name,
                    company = company,
                    result = "SUCCESS",
                    reason = "UNLIMITED",
                    shift = shift,
                    area = currentArea
                ))
            }
            return VerificationResult.Success(
                originalName = scannedInput,
                normalizedName = normalizedInput,
                matchedName = employee.name,
                isFuzzyMatch = isFuzzy,
                requiresNote = true,
                timestamp = timestamp
            )
        }

        if (currentUsage < allowance) {
            val newCount = currentUsage + 1
            prefs.edit().putInt(countKey, newCount).apply()

            GlobalScope.launch(Dispatchers.IO) {
                syncStatsToDb()
                updateScanCountFlow()
                scanEventDao.insert(ScanEvent(
                    timestamp = timestamp,
                    scannedCode = scannedInput,
                    matchedName = employee!!.name,
                    company = company,
                    result = "SUCCESS",
                    reason = null,
                    shift = shift,
                    area = currentArea
                ))
            }

            return VerificationResult.Success(
                originalName = scannedInput,
                normalizedName = normalizedInput,
                matchedName = employee.name,
                isFuzzyMatch = isFuzzy,
                timestamp = timestamp
            )
        } else {
            // LIMIT REACHED - CHECK BONUS (per shift)
            val bonusKey = "daily_bonus_used_$shift"
            val usedBonus = prefs.getInt(bonusKey, 0)

            if (usedBonus < DAILY_BONUS_THRESHOLD) {
                val newBonus = usedBonus + 1
                prefs.edit().putInt(bonusKey, newBonus).apply()

                GlobalScope.launch(Dispatchers.IO) {
                    syncStatsToDb()
                    updateScanCountFlow()
                    scanEventDao.insert(ScanEvent(
                        timestamp = timestamp,
                        scannedCode = scannedInput,
                        matchedName = employee!!.name,
                        company = company,
                        result = "BONUS",
                        reason = "LIMIT_REACHED_BONUS($newBonus/$DAILY_BONUS_THRESHOLD)",
                        shift = shift,
                        area = currentArea
                    ))
                }

                return VerificationResult.Success(
                    originalName = scannedInput,
                    normalizedName = normalizedInput,
                    matchedName = employee.name + " (BONUS)",
                    isFuzzyMatch = isFuzzy,
                    timestamp = timestamp
                )
            } else {
                logEvent(timestamp, scannedInput, employee.name, company, "DENIED", "LIMIT_REACHED", currentArea)
                return VerificationResult.Failure(
                    VerificationResult.Failure.Reason.LIMIT_REACHED,
                    scannedInput,
                    timestamp = timestamp
                )
            }
        }
    }

    private fun logEvent(ts: Long, code: String, name: String?, company: String?, res: String, reason: String?, area: String = "MAIN") {
        val shift = getCurrentShift()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                scanEventDao.insert(ScanEvent(
                    timestamp = ts,
                    scannedCode = code,
                    matchedName = name,
                    company = company,
                    result = res,
                    reason = reason,
                    shift = shift,
                    area = area
                ))
            } catch (e: Exception) {
                android.util.Log.e("AccessRepository", "Failed to insert $res scan: ${e.message}")
            }
        }
    }

    private suspend fun syncStatsToDb() {
        val today = getTodayDateString()
        val stats = getStats()
        val dailyStats = DailyStats(
            date = today,
            totalScans = stats["total_scans_today"] as Int,
            uniqueUsers = stats["unique_users_served"] as Int
        )
        statsDao.insertOrUpdate(dailyStats)
    }
    
    private fun updateScanCountFlow() {
        val stats = getStats()
        _currentScanCount.value = stats["total_scans_today"] as Int
    }

    fun getStats(): Map<String, Any> {
        val allEntries = prefs.all
        var totalScans = 0
        var uniqueUsers = 0
        
        for ((key, value) in allEntries) {
            if (key.startsWith("count_") && value is Int) {
                totalScans += value
                uniqueUsers++
            }
            if (key.startsWith("area2_count_") && value is Int) {
                totalScans += value
                uniqueUsers++
            }
        }
        
        val bonus = prefs.getInt("daily_bonus_used", 0)
        val area2Bonus = prefs.getInt("area2_bonus_used_DAY", 0) + prefs.getInt("area2_bonus_used_NIGHT", 0)
        totalScans += bonus + area2Bonus
        
        return mapOf(
            "total_scans_today" to totalScans,
            "unique_users_served" to uniqueUsers 
        )
    }
    
    suspend fun getDailyHistory(): List<DailyStats> {
        return withContext(Dispatchers.IO) {
            statsDao.getAllStats()
        }
    }

    fun getEventsForDate(dateStr: String): Flow<List<ScanEvent>> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        val start = date.time
        val end = start + 24 * 60 * 60 * 1000L
        return scanEventDao.getEventsByDateFlow(start, end)
    }
    
    // Expected attendance split by shift — average admitted over last 2 workdays.
    // In Area 2 mode, returns counts based on area2 scans only.
    suspend fun getExpectedAttendanceByShift(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val dayCountList   = mutableListOf<Int>()
        val nightCountList = mutableListOf<Int>()
        for (i in 1..14) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                val start = cal.clone() as Calendar
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)
                val end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_YEAR, 1)
                val events = scanEventDao.getEventsByDate(start.timeInMillis, end.timeInMillis)
                val relevant = if (isArea2Mode) events.filter { it.area == "AREA2" } else events
                dayCountList.add(relevant.count   { (it.result == "SUCCESS" || it.result == "BONUS") && it.shift == "DAY"   })
                nightCountList.add(relevant.count { (it.result == "SUCCESS" || it.result == "BONUS") && it.shift == "NIGHT" })
                if (dayCountList.size >= 2) break
            }
        }
        val dayAvg   = if (dayCountList.isEmpty())   0 else dayCountList.sum()   / dayCountList.size
        val nightAvg = if (nightCountList.isEmpty())  0 else nightCountList.sum() / nightCountList.size
        Pair(dayAvg, nightAvg)
    }

    suspend fun refreshWhitelist(): Boolean {
        _lastFetchStatus.value = "Fetching..."
        return withContext(Dispatchers.IO) {
            try {
                val employees = fetcher.fetchEmployees()
                if (employees.isNotEmpty()) {
                    val newMap = mutableMapOf<String, Employee>()
                    employees.forEach { emp ->
                        val fName = emp.firstName ?: ""
                        val lName = emp.lastName ?: ""
                        val combo = "$fName $lName"
                        val norm = StringNormalizer.normalize(combo)
                        val normOriginal = StringNormalizer.normalize(emp.name)
                        if (norm.isNotBlank()) newMap[norm] = emp
                        if (normOriginal.isNotBlank()) newMap[normOriginal] = emp
                    }
                    whitelist = newMap
                    totalEmployees = employees.size
                    saveToCache(employees)
                    loadCsvData()
                    _lastFetchStatus.value = "Success: ${employees.size} fetched + CSV merged."
                    lastError = null
                    true
                } else {
                    _lastFetchStatus.value = "Failed: No employees found."
                    lastError = "Empty list returned."
                    false
                }
            } catch (e: Exception) {
                _lastFetchStatus.value = "Error: ${e.message}"
                lastError = e.stackTraceToString()
                false
            }
        }
    }

    private fun saveToCache(employees: List<Employee>) {
        try {
            context.openFileOutput(CACHE_FILE, Context.MODE_PRIVATE).use { output ->
                val data = employees.joinToString("\n") { 
                    "${it.name}|${it.company}|${it.firstName ?: ""}|${it.lastName ?: ""}" 
                }
                output.write(data.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFromCache() {
        try {
            val file = context.getFileStreamPath(CACHE_FILE)
            if (file.exists()) {
                context.openFileInput(CACHE_FILE).use { input ->
                    val lines = input.bufferedReader().readLines()
                    val newMap = mutableMapOf<String, Employee>()
                    
                    lines.forEach { line ->
                        if (line.contains("|")) {
                            val parts = line.split("|")
                            val name = parts[0]
                            val company = parts.getOrElse(1) { "" }
                            val fName = parts.getOrElse(2) { "" }
                            val lName = parts.getOrElse(3) { "" }
                            
                            val emp = Employee(name, company, fName, lName)
                            val combo1 = "$fName $lName"
                            val combo2 = "$lName $fName"
                            val norm1 = StringNormalizer.normalize(combo1)
                            val norm2 = StringNormalizer.normalize(combo2)
                            val normOriginal = StringNormalizer.normalize(name)
                            
                            if (norm1.isNotBlank()) newMap[norm1] = emp
                            if (norm2.isNotBlank()) newMap[norm2] = emp
                            if (normOriginal.isNotBlank()) newMap[normOriginal] = emp
                        }
                    }
                    if (newMap.isNotEmpty()) {
                        whitelist = newMap
                        totalEmployees = newMap.size
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getWhitelistRawList(): List<Employee> {
        return whitelist.values.distinct().sortedBy { it.name }
    }

    suspend fun addNoteToJimCateringScan(timestamp: Long, note: String) {
        scanEventDao.updateNoteByTimestamp(note, timestamp)
    }

    suspend fun exportLogs(): String {
        return withContext(Dispatchers.IO) {
            val events = scanEventDao.getAll()
            val sb = StringBuilder()
            sb.append("ID;Time;Code;MatchedName;Company;Result;Reason;Shift;Area;Note\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            fun csvField(value: String?): String {
                val raw = (value ?: "")
                    .trim()
                    .replace("\r\n", " ")
                    .replace("\n", " ")
                    .replace("\r", " ")
                    .replace("\t", " ")
                    .replace(Regex("\\s+"), " ")
                return if (raw.contains(";") || raw.contains("\"")) {
                    "\"" + raw.replace("\"", "\"\"") + "\""
                } else raw
            }

            events.forEach { e ->
                val timeStr = sdf.format(Date(e.timestamp))
                sb.append("${e.id};$timeStr;${csvField(e.scannedCode)};${csvField(e.matchedName)};${csvField(e.company)};${csvField(e.result)};${csvField(e.reason)};${csvField(e.shift)};${csvField(e.area)};${csvField(e.note)}\n")
            }
            sb.toString()
        }
    }
}
