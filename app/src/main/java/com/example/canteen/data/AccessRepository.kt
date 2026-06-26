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
    private val scanEventDao = db.scanEventDao() // New DAO
    
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

    // Real-time list of ALL today's scans (SUCCESS, BONUS, DENIED) — resets at midnight
    val todayScans: Flow<List<ScanEvent>> = scanEventDao.getTodayAllScans(getStartOfDayTimestamp())

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
    }
    
    // Bonus config
    private val DAILY_BONUS_THRESHOLD = 25

    init {
        checkDailyReset()
        loadFromCache()
        loadCsvData() // Load CSV on startup (merging with cache)
        // Sync stats from prefs to DB on startup to ensure persistence
        GlobalScope.launch(Dispatchers.IO) {
            syncStatsToDb()
            updateScanCountFlow()
        }
    }

    private fun checkDailyReset() {
        val lastDate = prefs.getString("last_run_date", "")
        val todayDate = getTodayDateString()

        if (lastDate != todayDate) {
            // New Day: Archive yesterday's stats if not done? (Already done via sync usually)
            // Ideally we wipe daily counters but keep history.
            
            // Clear daily counters
            prefs.edit().clear().putString("last_run_date", todayDate).apply()
            _currentScanCount.value = 0
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    
    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Load CSV Data from Assets (technipqrlist/MAX_BADGES00.csv)
    private fun loadCsvData() {
        try {
            // Use Assets instead of InputStream argument for simplicity in this context
            val assetPath = "technipqrlist/MAX_BADGES00.csv"
            context.assets.open(assetPath).use { stream ->
                val reader = stream.bufferedReader()
                val lines = reader.readLines()
                
                // Helper map for merging to avoid overwriting Web-fetched users (Precedence to First System)
                // We clone the current whitelist to a mutable map
                val currentWhitelist = whitelist.toMutableMap()
                var addedCount = 0

                lines.drop(1).forEach { line -> // Skip Header
                    // Format: First name;Last name;Company Contractor;Company Sub Contractor
                    val parts = line.split(";")
                    if (parts.size >= 3) {
                        val fName = parts[0].trim()
                        val lName = parts[1].trim()
                        val companyMain = parts[2].trim()
                        val companySub = if (parts.size > 3) parts[3].trim() else ""
                        
                        // Rule: If Sub Contractor exists, use it (likely the actual employer for blacklist check)
                        var company = if (companySub.isNotBlank()) companySub else companyMain
                        
                        // FIX: Ignore "." or single char noise as company
                        if (company == "." || company.length < 2) {
                             // Fallback to Main if Sub was invalid, or just empty if both bad
                             company = if (companyMain.length > 1) companyMain else ""
                        }
                        
                        val fullName = "$fName $lName"

                        val emp = Employee(
                            name = fullName,
                            company = company,
                            firstName = fName,
                            lastName = lName
                        )

                        // Key Generation
                        // We add permutations just like Web entries
                        val combos = listOf(
                            "$fName $lName",
                            "$lName $fName"
                        )

                        for (combo in combos) {
                            val norm = StringNormalizer.normalize(combo)
                            if (norm.isNotBlank()) {
                                // ONLY ADD IF NOT EXISTS (Web Fetch takes precedence)
                                if (!currentWhitelist.containsKey(norm)) {
                                    currentWhitelist[norm] = emp
                                    addedCount++
                                }
                            }
                        }
                    }
                }
                
                if (addedCount > 0) {
                    whitelist = currentWhitelist
                    // Don't update totalEmployees just yet or do we? 
                    // totalEmployees usually tracks "Fetched" count. 
                    // Let's just update the map.
                }
                // Log/Debug could go here
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        injectManualUsers()
    }
    
    private fun injectManualUsers() {
        // Manually add users who are missing from CSV/API but need access
        val manualUsers = listOf(
            Employee("Tudor Marian", "ManualWhitelist", "Tudor", "Marian"),
            Employee("Despa Constantin", "ManualWhitelist", "Despa", "Constantin"),
            Employee("Adrian Valeriu", "ManualWhitelist", "Adrian", "Valeriu"),
            Employee("Daniel Ionut Papatoiiu", "ManualWhitelist", "Daniel Ionut", "Papatoiiu"),
            Employee("Andrei Alexandru Ionut Dima", "ManualWhitelist", "Andrei Alexandru Ionut", "Dima"),
            Employee("Andre Fernandes Da sousa Nunes", "ManualWhitelist", "Andre Fernandes", "Da sousa Nunes"),
            Employee("Marius Gabriel Nica", "ManualWhitelist", "Marius Gabriel", "Nica"),
            Employee("Schiano Hugo", "ManualWhitelist", "Schiano", "Hugo"),
            Employee("Sebastian Tomaszkowicz", "ManualWhitelist", "Sebastian", "Tomaszkowicz")
        )
        
        val currentWhitelist = whitelist.toMutableMap()
        manualUsers.forEach { emp ->
            // Map variations
            val rawName = emp.name
            val dotName = "${emp.firstName}.${emp.lastName}".lowercase() // tudor.marian
            
            val normOriginal = StringNormalizer.normalize(rawName)
            if (normOriginal.isNotBlank()) currentWhitelist[normOriginal] = emp
            
            // Also map the specific "dot" format they might get scanned as
            if (dotName.isNotBlank()) currentWhitelist[dotName] = emp
        }
        whitelist = currentWhitelist
    }
    
    // Legacy loadData stub - removed/replaced
    fun loadData(csvInputStream: InputStream) { 
        // No-op or redirect to loadCsvData if needed, but we used internal asset loading
    }

    fun verifyAccess(scannedInput: String): VerificationResult {
        checkDailyReset()
        
        val timestamp = System.currentTimeMillis()
        val cleanedInput = if (scannedInput.contains("_")) scannedInput.substringAfter("_") else scannedInput
        val normalizedInput = StringNormalizer.normalize(cleanedInput)
        
        // 2. Find Employee
        // Try exact match first
        var employee = whitelist[normalizedInput]
        var isFuzzy = false
        var matchedKey = normalizedInput
        
        // If not found, try Smart Token Match
        if (employee == null) {
            // New "Smart Token" Strategy
            // Iterate over UNIQUE employees (values)
            val uniqueEmployees = whitelist.values.distinct()
            
            for (emp in uniqueEmployees) {
                // Check match against original name (or reconstructed full name)
                // We pass the Employee's Name to the matcher
                // But wait, emp.name from CSV might be "First Last". 
                // We should probably rely on the normalized key map?
                // No, Smart Token Match needs raw-ish or tokenized strings. 
                // StringNormalizer.smartTokenMatch handles normalization internally.
                
                // Use emp.name
                if (StringNormalizer.smartTokenMatch(scannedInput, emp.name)) {
                    employee = emp
                    isFuzzy = true
                    matchedKey = StringNormalizer.normalize(emp.name)
                    break // Stop on first match
                }
            }
        }
        
        // --- 1. UNKNOWN USER ---
        if (employee == null) {
            // Not found at all
            logEvent(timestamp, scannedInput, null, null, "DENIED", "UNKNOWN_USER")
            return VerificationResult.Failure(
                VerificationResult.Failure.Reason.UNKNOWN_USER,
                scannedInput
            )
        }
        
        // 3. Check Companies
        val company = employee.company.trim()
        val lowerCompany = company.lowercase()
        
        // Bypass for specific users if needed (e.g. correct company data issues)
        val SPECIAL_WHITELIST = setOf(
            "Cristian De Domenico",
            "Tudor Marian",
            "Carlos Ferreira Palhau",
            "Giovanni Giarrizzo",
            "Chukwudi Joshua Agim",
            "Despa Constantin",
            "Mohammed Fezaad Khan",
            "Adrian Valeriu",
            "Daniel Ionut Papatoiiu",
            "Johan Weesie",
            "Andrei Alexandru Ionut Dima",
            "Carlos Vilela",
            "Patrick Flohil",
            "Andre Fernandes Da sousa Nunes",
            "Marius Gabriel Nica",
            "Schiano Hugo",
            "Sebastian Tomaszkowicz"
        )
        val isSpecialWhitelisted = SPECIAL_WHITELIST.any { it.equals(employee.name.trim(), ignoreCase = true) }

        // --- 2. BLACKLIST ---
        val isCompanyBlacklisted = FORBIDDEN_COMPANIES.any { it.equals(company, ignoreCase = true) }
        
        // Also check if employee is specifically blacklisted
        // Use StringNormalizer to ensure we catch variations in spacing/lowercase and name ordering
        val isEmployeeBlacklisted = FORBIDDEN_EMPLOYEES.any { 
            it.equals(employee.name.trim(), ignoreCase = true) ||
            StringNormalizer.normalize(it) == StringNormalizer.normalize(employee.name) ||
            StringNormalizer.smartTokenMatch(it, employee.name)
        }
        
        val isBlacklisted = isCompanyBlacklisted || isEmployeeBlacklisted
        
        if (isBlacklisted && !isSpecialWhitelisted) {
             logEvent(timestamp, scannedInput, employee.name, company, "DENIED", "BLACKLISTED")
             return VerificationResult.Failure(
                VerificationResult.Failure.Reason.BLACK_LISTED,
                scannedInput,
                company
            )
        }
        
        // --- 3. WHITELIST CHECK ---
        val isWhitelisted = ALLOWED_COMPANIES.any { it.equals(company, ignoreCase = true) }
        
        if (!isWhitelisted && !isSpecialWhitelisted) {
             // Treat as Blacklisted/Unauthorized but with Company Name
             logEvent(timestamp, scannedInput, employee.name, company, "DENIED", "NOT_WHITELISTED")
             return VerificationResult.Failure(
                VerificationResult.Failure.Reason.BLACK_LISTED, 
                scannedInput,
                company
            )
        }

        // --- 4. DAILY LIMITS + BONUS ---
        val countKey = "count_$matchedKey"
        val currentUsage = prefs.getInt(countKey, 0)
        val allowance = 1 
        
        if (currentUsage < allowance) {
            // SUCCESS (Standard)
            val newCount = currentUsage + 1
            prefs.edit().putInt(countKey, newCount).apply()
            
            // Async Persistence to DB
            GlobalScope.launch(Dispatchers.IO) {
                syncStatsToDb()
                updateScanCountFlow()
                scanEventDao.insert(ScanEvent(
                    timestamp = timestamp,
                    scannedCode = scannedInput,
                    matchedName = employee!!.name,
                    company = company,
                    result = "SUCCESS",
                    reason = null
                ))
            }
            
            return VerificationResult.Success(
                originalName = scannedInput, 
                normalizedName = normalizedInput,
                matchedName = employee.name, 
                isFuzzyMatch = isFuzzy
            )
        } else {
            // LIMIT REACHED - CHECK BONUS
            // We need to check GLOBAL bonus usage for today
            val usedBonus = prefs.getInt("daily_bonus_used", 0)
            
            if (usedBonus < DAILY_BONUS_THRESHOLD) {
                // GRANT BONUS ACCESS
                val newBonus = usedBonus + 1
                prefs.edit().putInt("daily_bonus_used", newBonus).apply()
                
                // We DON'T increment their personal count effectively (or we do? User said "first 25 limit reached pass").
                // Implies they get a pass. We should probably log it as BONUS.
                
                GlobalScope.launch(Dispatchers.IO) {
                    syncStatsToDb()
                    updateScanCountFlow() // Bonus counts as a scan
                    scanEventDao.insert(ScanEvent(
                        timestamp = timestamp,
                        scannedCode = scannedInput,
                        matchedName = employee!!.name,
                        company = company,
                        result = "BONUS", // Distinction
                        reason = "LIMIT_REACHED_BONUS($newBonus/$DAILY_BONUS_THRESHOLD)"
                    ))
                }
                
                return VerificationResult.Success(
                    originalName = scannedInput,
                    normalizedName = normalizedInput,
                    matchedName = employee.name + " (BONUS)",
                    isFuzzyMatch = isFuzzy
                )
            } else {
                // HARD DENY
                logEvent(timestamp, scannedInput, employee.name, company, "DENIED", "LIMIT_REACHED")
                return VerificationResult.Failure(
                    VerificationResult.Failure.Reason.LIMIT_REACHED,
                    scannedInput
                )
            }
        }
    }
    
    private fun logEvent(ts: Long, code: String, name: String?, company: String?, res: String, reason: String?) {
        GlobalScope.launch(Dispatchers.IO) {
            scanEventDao.insert(ScanEvent(
                timestamp = ts,
                scannedCode = code,
                matchedName = name,
                company = company,
                result = res,
                reason = reason
            ))
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
        }
        // Bonus counts are NOT stored in "count_" keys usually? 
        // Wait, if we grant access, we usually increment count. 
        // But for bonus, we incremented "daily_bonus_used".
        // Should bonus scans count towards TOTAL SCANS? Yes.
        // So we should add daily_bonus_used to totalScans?
        // Or did we increment "count_" for them? In code above I did NOT increment personal count for Bonus.
        // So we must add bonus explicitly.
        
        val bonus = prefs.getInt("daily_bonus_used", 0)
        totalScans += bonus
        
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
    
    // Expected attendance = Count of Whitelisted Employees
    fun getExpectedAttendance(): Int {
        // Filter current whitelist by Allowed Companies
        return whitelist.values.distinct().count { emp -> 
            ALLOWED_COMPANIES.any { it.equals(emp.company, ignoreCase = true) }
        }
    }

    suspend fun refreshWhitelist(): Boolean {
        _lastFetchStatus.value = "Fetching..."
        return withContext(Dispatchers.IO) {
            try {
                val employees = fetcher.fetchEmployees() // Returns List<Employee>
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
                    
                    // Merge CSV Data after refresh
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
                            
                            // Reconstruct whitelist keys
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

    suspend fun exportLogs(): String {
        return withContext(Dispatchers.IO) {
            val events = scanEventDao.getAll()
            val sb = StringBuilder()
            sb.append("ID;Timestamp;Time;Code;MatchedName;Company;Result;Reason\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            events.forEach { e ->
                val timeStr = sdf.format(Date(e.timestamp))
                sb.append("${e.id};${e.timestamp};$timeStr;${e.scannedCode};${e.matchedName ?: ""};${e.company ?: ""};${e.result};${e.reason ?: ""}\n")
            }
            sb.toString()
        }
    }
}
