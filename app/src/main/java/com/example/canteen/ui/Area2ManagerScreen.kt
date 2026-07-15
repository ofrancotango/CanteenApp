package com.example.canteen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canteen.data.FirebaseEmployee
import com.example.canteen.ui.theme.AppBackground
import com.example.canteen.ui.theme.AppBorder
import com.example.canteen.ui.theme.AppMuted
import com.example.canteen.ui.theme.AppSurface
import com.example.canteen.ui.theme.AppText
import com.example.canteen.ui.theme.ErrorRed

// Area 2 accent colour — purple to distinguish from the main canteen
private val Area2Accent = Color(0xFF7C3AED)
private val Area2AccentLight = Color(0xFFEDE9FE)

@Composable
fun Area2ManagerScreen(
    employees: List<FirebaseEmployee>,
    onAddEmployee: (name: String, company: String) -> Unit,
    onRemoveEmployee: (key: String) -> Unit,
    onBackClick: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDeleteKey by remember { mutableStateOf<String?>(null) }
    var pendingDeleteName by remember { mutableStateOf("") }

    if (showAddDialog) {
        Area2AddEmployeeDialog(
            onConfirm = { name, company ->
                onAddEmployee(name, company)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (pendingDeleteKey != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteKey = null },
            containerColor = AppSurface,
            title = { Text("Remove from Area 2?", color = AppText) },
            text = { Text("Remove $pendingDeleteName from the Area 2 list?", color = AppMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteKey?.let { onRemoveEmployee(it) }
                        pendingDeleteKey = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteKey = null }) {
                    Text("Cancel", color = AppMuted)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Area2Accent)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBackClick) {
                        Text("← Back", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                }
                Text(
                    text = "🏠 Area 2 — People List",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${employees.size} people authorised for Area 2",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
            }
        }

        // Info banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Area2AccentLight)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Only people on this list can access the secondary canteen when Area 2 Mode is active. Jim Catering always passes regardless.",
                color = Area2Accent,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        // Add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Area2Accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Person")
            }
        }

        // List
        if (employees.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏠", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No people in Area 2 yet",
                        color = AppMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap "Add Person" to add someone to the secondary canteen.",
                        color = AppMuted.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(employees, key = { it.key }) { emp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppSurface)
                            .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Area2AccentLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emp.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = Area2Accent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = emp.name,
                                color = AppText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (emp.company.isNotBlank() && emp.company != "Area2") {
                                Text(
                                    text = emp.company,
                                    color = AppMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                pendingDeleteKey = emp.key
                                pendingDeleteName = emp.name
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = ErrorRed.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun Area2AddEmployeeDialog(
    onConfirm: (name: String, company: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    val isValid = name.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Add to Area 2", color = AppText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "This person will be allowed into the secondary canteen when Area 2 Mode is active.",
                    color = AppMuted,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full name *") },
                    placeholder = { Text("e.g. Mario Rossi") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Area2Accent,
                        unfocusedBorderColor = AppBorder,
                        focusedLabelColor = Area2Accent,
                        unfocusedLabelColor = AppMuted,
                        focusedTextColor = AppText,
                        unfocusedTextColor = AppText,
                        cursorColor = Area2Accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company (optional)") },
                    placeholder = { Text("e.g. Max Streicher") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Area2Accent,
                        unfocusedBorderColor = AppBorder,
                        focusedLabelColor = Area2Accent,
                        unfocusedLabelColor = AppMuted,
                        focusedTextColor = AppText,
                        unfocusedTextColor = AppText,
                        cursorColor = Area2Accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name.trim(), company.trim().ifEmpty { "Area2" })
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Area2Accent)
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AppMuted) }
        }
    )
}
