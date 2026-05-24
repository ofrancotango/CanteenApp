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

class FirebaseSyncRepository {

    private val database = FirebaseDatabase.getInstance("https://app-cant-default-rtdb.firebaseio.com/")
    private val configRef = database.getReference("config")
    private val scansRef = database.getReference("scans")

    private val _isAppEnabled = MutableStateFlow(true)
    val isAppEnabled: StateFlow<Boolean> = _isAppEnabled

    private val _todayCloudScans = MutableStateFlow<List<CloudScan>>(emptyList())
    val todayCloudScans: StateFlow<List<CloudScan>> = _todayCloudScans

    private var killSwitchListener: ValueEventListener? = null
    private var todayScansListener: ValueEventListener? = null
    private var currentTodayDate: String = ""

    fun startListening() {
        listenToKillSwitch()
        listenToTodayScans()
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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                _todayCloudScans.value = scans.sortedByDescending { it.timestamp }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        scansRef.child(today).addValueEventListener(todayScansListener!!)
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
        killSwitchListener = null
        todayScansListener = null
    }

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

    private fun getTodayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
