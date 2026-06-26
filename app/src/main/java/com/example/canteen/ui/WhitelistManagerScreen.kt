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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canteen.data.FirebaseEmployee
import com.example.canteen.ui.theme.AppAccent
import com.example.canteen.ui.theme.AppBackground
import com.example.canteen.ui.theme.AppBorder
import com.example.canteen.ui.theme.AppMuted
import com.example.canteen.ui.theme.AppSurface
import com.example.canteen.ui.theme.AppText
import com.example.canteen.ui.theme.ErrorRed
import com.example.canteen.ui.theme.SuccessGreen

@Composable
fun WhitelistManagerScreen(
    employees: List<FirebaseEmployee>,
    onAddEmployee: (name: String, company: String) -> Unit,
    onRemoveEmployee: (key: String) -> Unit,
    onBackClick: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDeleteKey by remember { mutableStateOf<String?>(null) }
    var pendingDeleteName by remember { mutableStateOf("") }

    if (showAddDialog) {
        AddEmployeeDialog(
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
            title = { Text("Remove employee?", color = AppText) },
            text = { Text("Remove $pendingDeleteName from the manual whitelist?", color = AppMuted) },
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

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppSurface)
                        .border(1.dp, AppBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("\u2190", color = AppMuted, fontSize = 18.sp) }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Whitelist Manuale",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppText
                )
                Text(
                    text = "${employees.size} dipendenti aggiunti manualmente",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppMuted
                )
            }
            IconButton(onClick = { showAddDialog = true }) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Info banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppAccent.copy(alpha = 0.08f))
                .border(1.dp, AppAccent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "\uD83D\uDCF1 Le modifiche si sincronizzano automaticamente su tutti i dispositivi.",
                style = MaterialTheme.typography.labelSmall,
                color = AppAccent.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (employees.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDC64", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nessun dipendente manuale", style = MaterialTheme.typography.titleMedium, color = AppMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Usa + per aggiungerne uno", style = MaterialTheme.typography.bodySmall, color = AppMuted.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(horizontal = 24.dp)) {
                items(employees, key = { it.key }) { emp ->
                    EmployeeCard(
                        employee = emp,
                        onDelete = {
                            pendingDeleteKey = emp.key
                            pendingDeleteName = emp.name
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun EmployeeCard(employee: FirebaseEmployee, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.1f))
                    .border(1.dp, SuccessGreen.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelLarge,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppText
                )
                Text(
                    text = employee.company,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppMuted
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = ErrorRed.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddEmployeeDialog(
    onConfirm: (name: String, company: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    val isValid = name.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Aggiungi dipendente", color = AppText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Il dipendente sarà aggiunto alla whitelist su tutti i dispositivi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppMuted
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome completo *") },
                    placeholder = { Text("es. Mario Rossi") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = AppBorder,
                        focusedLabelColor = AppAccent,
                        unfocusedLabelColor = AppMuted,
                        focusedTextColor = AppText,
                        unfocusedTextColor = AppText,
                        cursorColor = AppAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Azienda") },
                    placeholder = { Text("es. Max Streicher") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = AppBorder,
                        focusedLabelColor = AppAccent,
                        unfocusedLabelColor = AppMuted,
                        focusedTextColor = AppText,
                        unfocusedTextColor = AppText,
                        cursorColor = AppAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name.trim(), company.trim().ifEmpty { "ManualWhitelist" })
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
            ) { Text("Aggiungi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", color = AppMuted) }
        }
    )
}
