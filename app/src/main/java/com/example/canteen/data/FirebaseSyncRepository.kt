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

    // ── Area 2 ──────────────────────────────────────────────────────────────
    private val _isArea2Mode = MutableStateFlow(false)
    val isArea2Mode: StateFlow<Boolean> = _isArea2Mode

    private val _area2Employees = MutableStateFlow<List<FirebaseEmployee>>(emptyList())
    val area2Employees: StateFlow<List<FirebaseEmployee>> = _area2Employees

    private var killSwitchListener: ValueEventListener? = null
    private var todayScansListener: ValueEventListener? = null
    private var allowedCompaniesListener: ValueEventListener? = null
    private var forbiddenCompaniesListener: ValueEventListener? = null
    private var forbiddenEmployeesListener: ValueEventListener? = null
    private var manualEmployeesListener: ValueEventListener? = null
    private var area2ModeListener: ValueEventListener? = null
    private var area2EmployeesListener: ValueEventListener? = null
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
        listenToArea2Mode()
        listenToArea2Employees()
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
                    } catch (_: Exception) {}
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
                val companies = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val enabled = child.getValue(Boolean::class.java) ?: false
                    if (enabled) companies.add(child.key?.replace("_", " ") ?: "")
                }
                if (companies.isNotEmpty()) _allowedCompanies.value = companies
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        configRef.child("allowedCompanies").addValueEventListener(allowedCompaniesListener!!)
    }

    private fun listenToForbiddenCompanies() {
        forbiddenCompaniesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val companies = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val enabled = child.getValue(Boolean::class.java) ?: false
                    if (enabled) companies.add(child.key?.replace("_", " ") ?: "")
                }
                if (companies.isNotEmpty()) _forbiddenCompanies.value = companies
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        configRef.child("forbiddenCompanies").addValueEventListener(forbiddenCompaniesListener!!)
    }

    private fun listenToForbiddenEmployees() {
        forbiddenEmployeesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val employees = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val enabled = child.getValue(Boolean::class.java) ?: false
                    if (enabled) employees.add(child.key?.replace("_", " ") ?: "")
                }
                if (employees.isNotEmpty()) _forbiddenEmployees.value = employees
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        configRef.child("forbiddenEmployees").addValueEventListener(forbiddenEmployeesListener!!)
    }

    private fun listenToManualEmployees() {
        manualEmployeesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val employees = mutableListOf<FirebaseEmployee>()
                for (child in snapshot.children) {
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    val company = child.child("company").getValue(String::class.java) ?: "ManualWhitelist"
                    employees.add(FirebaseEmployee(child.key ?: "", name, company))
                }
                _manualEmployees.value = employees
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        configRef.child("manualEmployees").addValueEventListener(manualEmployeesListener!!)
    }

    // ── Area 2 listeners ────────────────────────────────────────────────────

    private fun listenToArea2Mode() {
        area2ModeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isArea2Mode.value = snapshot.getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        configRef.child("area2Mode").addValueEventListener(area2ModeListener!!)
    }

    private fun listenToArea2Employees() {
        area2EmployeesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val employees = mutableListOf<FirebaseEmployee>()
                for (child in snapshot.children) {
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    val company = child.child("company").getValue(String::class.java) ?: "Area2"
                    employees.add(FirebaseEmployee(child.key ?: "", name, company))
                }
                _area2Employees.value = employees
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        configRef.child("area2Employees").addValueEventListener(area2EmployeesListener!!)
    }

    // ── Writes ───────────────────────────────────────────────────────────────

    fun setAppEnabled(enabled: Boolean) {
        configRef.child("appEnabled").setValue(enabled)
    }

    fun setArea2Mode(enabled: Boolean) {
        configRef.child("area2Mode").setValue(enabled)
    }

    fun addArea2Employee(name: String, company: String) {
        val data = mapOf("name" to name, "company" to company)
        configRef.child("area2Employees").push().setValue(data)
    }

    fun removeArea2Employee(key: String) {
        configRef.child("area2Employees").child(key).removeValue()
    }

    fun addManualEmployee(name: String, company: String) {
        val data = mapOf("name" to name, "company" to company)
        configRef.child("manualEmployees").push().setValue(data)
    }

    fun removeManualEmployee(key: String) {
        configRef.child("manualEmployees").child(key).removeValue()
    }

    fun addAllowedCompany(name: String) {
        configRef.child("allowedCompanies").child(name.replace(" ", "_")).setValue(true)
    }

    fun removeAllowedCompany(name: String) {
        configRef.child("allowedCompanies").child(name.replace(" ", "_")).removeValue()
    }

    fun addForbiddenCompany(name: String) {
        configRef.child("forbiddenCompanies").child(name.replace(" ", "_")).setValue(true)
    }

    fun removeForbiddenCompany(name: String) {
        configRef.child("forbiddenCompanies").child(name.replace(" ", "_")).removeValue()
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

    fun initializeDefaultAllowedCompanies() {
        val data = DEFAULT_ALLOWED_COMPANIES.associate { it.replace(" ", "_") to true }
        configRef.child("allowedCompanies").setValue(data)
    }

    fun initializeDefaultForbiddenCompanies() {
        val data = DEFAULT_FORBIDDEN_COMPANIES.associate { it.replace(".", "_") to true }
        configRef.child("forbiddenCompanies").setValue(data)
    }

    fun initializeDefaultForbiddenEmployees() {
        val data = DEFAULT_FORBIDDEN_EMPLOYEES.associate { it.replace(" ", "_") to true }
        configRef.child("forbiddenEmployees").setValue(data)
    }

    fun stopListening() {
        killSwitchListener?.let { configRef.child("appEnabled").removeEventListener(it) }
        todayScansListener?.let { scansRef.child(currentTodayDate).removeEventListener(it) }
        allowedCompaniesListener?.let { configRef.child("allowedCompanies").removeEventListener(it) }
        forbiddenCompaniesListener?.let { configRef.child("forbiddenCompanies").removeEventListener(it) }
        forbiddenEmployeesListener?.let { configRef.child("forbiddenEmployees").removeEventListener(it) }
        manualEmployeesListener?.let { configRef.child("manualEmployees").removeEventListener(it) }
        area2ModeListener?.let { configRef.child("area2Mode").removeEventListener(it) }
        area2EmployeesListener?.let { configRef.child("area2Employees").removeEventListener(it) }
    }

    private fun getTodayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
