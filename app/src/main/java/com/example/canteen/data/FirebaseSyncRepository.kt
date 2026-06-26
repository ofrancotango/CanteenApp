package com.example.canteen.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FirebaseEmployee(
    val key: String,
    val name: String,
    val company: String
)

class FirebaseSyncRepository {

    private val database = FirebaseDatabase.getInstance("https://app-cant-default-rtdb.firebaseio.com/")
    private val configRef = database.getReference("config")
    private val scansRef = database.getReference("scans")

    private val _isAppEnabled = MutableStateFlow(true)
    val isAppEnabled: StateFlow<Boolean> = _isAppEnabled

    private val _todayCloudScans = MutableStateFlow<List<CloudScan>>(emptyList())
    val todayCloudScans: StateFlow<List<CloudScan>> = _todayCloudScans

    // Firebase-managed company rules
    private val _allowedCompanies = MutableStateFlow<Set<String>>(emptySet())
    val allowedCompanies: StateFlow<Set<String>> = _allowedCompanies

    private val _forbiddenCompanies = MutableStateFlow<Set<String>>(emptySet())
    val forbiddenCompanies: StateFlow<Set<String>> = _forbiddenCompanies

    private val _forbiddenEmployees = MutableStateFlow<Set<String>>(emptySet())
    val forbiddenEmployees: StateFlow<Set<String>> = _forbiddenEmployees

    // Manual whitelist employees added via app
    private val _manualEmployees = MutableStateFlow<List<FirebaseEmployee>>(emptyList())
    val manualEmployees: StateFlow<List<FirebaseEmployee>> = _manualEmployees

    private var killSwitchListener: ValueEventListener? = null
    private var todayScansListener: ValueEventListener? = null
    private var allowedCompaniesListener: ValueEventListener? = null
    private var forbiddenCompaniesListener: ValueEventListener? = null
    private var forbiddenEmployeesListener: ValueEventListener? = null
    private var manualEmployeesListener: ValueEventListener? = null
    private var currentTodayDate: String = ""

    companion object {
        val DEFAULT_ALLOWED_COMPANIES = setOf(
            "EOS", "Max Streicher", "PMM", "Admar", "Adotech",
            "Cargomet", "Kotloinwest", "Liliana", "OMV", "RMLI", "Strong", "Ado Tech", "Delta"
        )
        val DEFAULT_FORBIDDEN_COMPANIES = setOf(
            "Cakici", "DK build", "Galiv", "gts", "ms management", "polprep", "workers4u"
        )
        val DEFAULT_FORBIDDEN_EMPLOYEES = setOf(
            "Eugene jansen", "Guilliano pahawakan", "Kevin santiago", "Zaza arabuli", "Aleksandre khanjaladze"
        )
    }

    fun startListening() {
        listenToKillSwitch()
        listenToTodayScans()
        listenToAllowedCompanies()
        listenToForbiddenCompanies()
        listenToForbiddenEmployees()
        listenToManualEmployees()
    }

    private fun listenToKillSwitch() {
        killSwitchListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isAppEnabled.value = snapshot.getValue(Boolean::class.java) ?: true
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        configRef.child("appEnabled").addValueEventListener(killSwitchListener!!)
    }

    private fun listenToTodayScans() {
        val today = getTodayString()
        currentTodayDate = today
        todayScansListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val scans = mutableListOf<CloudScan>()
                for (child in snapshot.children) {
                    try {
                        val name = child.child("name").getValue(String::class.java) ?: ""
                        val company = child.child("company").getValue(String::class.java) ?: ""
                        val result = child.child("result").getValue(String::class.java) ?: ""
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        val deviceId = child.child("deviceId").getValue(String::class.java) ?: ""
                        scans.add(CloudScan(name, company, result, timestamp, deviceId))
                    } catch (e: Exception) { e.printStackTrace() }
                }
                _todayCloudScans.value = scans.sortedByDescending { it.timestamp }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        scansRef.child(today).addValueEventListener(todayScansListener!!)
    }

    private fun listenToAllowedCompanies() {
        allowedCompaniesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // First run: initialize with defaults
                    initializeDefaultAllowedCompanies()
                    _allowedCompanies.value = DEFAULT_ALLOWED_COMPANIES
                    return
                }
                val set = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val enabled = child.getValue(Boolean::class.java) ?: true
                    if (enabled) set.add(child.key ?: continue)
                }
                _allowedCompanies.value = if (set.isEmpty()) DEFAULT_ALLOWED_COMPANIES else set
            }
            override fun onCancelled(error: DatabaseError) {
                _allowedCompanies.value = DEFAULT_ALLOWED_COMPANIES
            }
        }
        configRef.child("allowedCompanies").addValueEventListener(allowedCompaniesListener!!)
    }

    private fun listenToForbiddenCompanies() {
        forbiddenCompaniesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    initializeDefaultForbiddenCompanies()
                    _forbiddenCompanies.value = DEFAULT_FORBIDDEN_COMPANIES
                    return
                }
                val set = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val enabled = child.getValue(Boolean::class.java) ?: true
                    if (enabled) set.add(child.key ?: continue)
                }
                _forbiddenCompanies.value = set
            }
            override fun onCancelled(error: DatabaseError) {
                _forbiddenCompanies.value = DEFAULT_FORBIDDEN_COMPANIES
            }
        }
        configRef.child("forbiddenCompanies").addValueEventListener(forbiddenCompaniesListener!!)
    }

    private fun listenToForbiddenEmployees() {
        forbiddenEmployeesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    initializeDefaultForbiddenEmployees()
                    _forbiddenEmployees.value = DEFAULT_FORBIDDEN_EMPLOYEES
                    return
                }
                val set = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val enabled = child.getValue(Boolean::class.java) ?: true
                    if (enabled) set.add(child.key?.replace("_", " ") ?: continue)
                }
                _forbiddenEmployees.value = set
            }
            override fun onCancelled(error: DatabaseError) {
                _forbiddenEmployees.value = DEFAULT_FORBIDDEN_EMPLOYEES
            }
        }
        configRef.child("forbiddenEmployees").addValueEventListener(forbiddenEmployeesListener!!)
    }

    private fun listenToManualEmployees() {
        manualEmployeesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FirebaseEmployee>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    val company = child.child("company").getValue(String::class.java) ?: "ManualWhitelist"
                    list.add(FirebaseEmployee(key, name, company))
                }
                _manualEmployees.value = list.sortedBy { it.name }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        configRef.child("manualEmployees").addValueEventListener(manualEmployeesListener!!)
    }

    // ── Company Rules Write Methods ──────────────────────────────────────────

    fun addAllowedCompany(company: String) {
        val safeKey = company.replace(".", "_")
        configRef.child("allowedCompanies").child(safeKey).setValue(true)
        // If it was forbidden, remove from forbidden
        configRef.child("forbiddenCompanies").child(safeKey).removeValue()
    }

    fun removeAllowedCompany(company: String) {
        val safeKey = company.replace(".", "_")
        configRef.child("allowedCompanies").child(safeKey).setValue(false)
    }

    fun addForbiddenCompany(company: String) {
        val safeKey = company.replace(".", "_")
        configRef.child("forbiddenCompanies").child(safeKey).setValue(true)
        // If it was allowed, remove from allowed
        configRef.child("allowedCompanies").child(safeKey).setValue(false)
    }

    fun removeForbiddenCompany(company: String) {
        val safeKey = company.replace(".", "_")
        configRef.child("forbiddenCompanies").child(safeKey).setValue(false)
    }

    // ── Manual Employee Write Methods ────────────────────────────────────────

    fun addManualEmployee(name: String, company: String) {
        val data = mapOf("name" to name, "company" to company)
        configRef.child("manualEmployees").push().setValue(data)
    }

    fun removeManualEmployee(key: String) {
        configRef.child("manualEmployees").child(key).removeValue()
    }

    // ── Forbidden Employees Write Methods ────────────────────────────────────

    fun addForbiddenEmployee(name: String) {
        val safeKey = name.replace(" ", "_")
        configRef.child("forbiddenEmployees").child(safeKey).setValue(true)
    }

    fun removeForbiddenEmployee(name: String) {
        val safeKey = name.replace(" ", "_")
        configRef.child("forbiddenEmployees").child(safeKey).setValue(false)
    }

    // ── Default Initializers (first-run only) ────────────────────────────────

    private fun initializeDefaultAllowedCompanies() {
        val data = DEFAULT_ALLOWED_COMPANIES.associate { it.replace(".", "_") to true }
        configRef.child("allowedCompanies").setValue(data)
    }

    private fun initializeDefaultForbiddenCompanies() {
        val data = DEFAULT_FORBIDDEN_COMPANIES.associate { it.replace(".", "_") to true }
        configRef.child("forbiddenCompanies").setValue(data)
    }

    private fun initializeDefaultForbiddenEmployees() {
        val data = DEFAULT_FORBIDDEN_EMPLOYEES.associate { it.replace(" ", "_") to true }
        configRef.child("forbiddenEmployees").setValue(data)
    }

    // ── Misc ─────────────────────────────────────────────────────────────────

    fun setAppEnabled(enabled: Boolean) {
        configRef.child("appEnabled").setValue(enabled)
    }

    fun pushScan(name: String, company: String, result: String, timestamp: Long, deviceId: String) {
        val today = getTodayString()
        val scan = mapOf(
            "name" to name,
            "company" to company,
            "result" to result,
            "timestamp" to timestamp,
            "deviceId" to deviceId
        )
        scansRef.child(today).push().setValue(scan)
    }

    fun refreshTodayListenerIfNeeded() {
        val today = getTodayString()
        if (today != currentTodayDate) {
            todayScansListener?.let { scansRef.child(currentTodayDate).removeEventListener(it) }
            _todayCloudScans.value = emptyList()
            listenToTodayScans()
        }
    }

    fun stopListening() {
        killSwitchListener?.let { configRef.child("appEnabled").removeEventListener(it) }
        todayScansListener?.let { scansRef.child(currentTodayDate).removeEventListener(it) }
        allowedCompaniesListener?.let { configRef.child("allowedCompanies").removeEventListener(it) }
        forbiddenCompaniesListener?.let { configRef.child("forbiddenCompanies").removeEventListener(it) }
        forbiddenEmployeesListener?.let { configRef.child("forbiddenEmployees").removeEventListener(it) }
        manualEmployeesListener?.let { configRef.child("manualEmployees").removeEventListener(it) }
    }

    private fun getTodayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
