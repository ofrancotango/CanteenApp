package com.example.canteen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.canteen.ui.theme.AppAccent
import com.example.canteen.ui.theme.AppBackground
import com.example.canteen.ui.theme.AppBorder
import com.example.canteen.ui.theme.AppMuted
import com.example.canteen.ui.theme.AppSurface
import com.example.canteen.ui.theme.AppText
import com.example.canteen.ui.theme.ErrorRed
import com.example.canteen.ui.theme.SuccessGreen

enum class CompanyStatus { ALLOWED, FORBIDDEN, NEUTRAL }

@Composable
fun CompanyRulesScreen(
    allowedCompanies: Set<String>,
    forbiddenCompanies: Set<String>,
    onAddAllowed: (String) -> Unit,
    onRemoveAllowed: (String) -> Unit,
    onAddForbidden: (String) -> Unit,
    onRemoveForbidden: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf<CompanyStatus?>(null) }

    // Merge all companies and determine status
    val allCompanies = (allowedCompanies + forbiddenCompanies).sortedBy { it.lowercase() }

    val statusMap: Map<String, CompanyStatus> = allCompanies.associate { company ->
        company to when {
            allowedCompanies.any { it.equals(company, ignoreCase = true) } -> CompanyStatus.ALLOWED
            forbiddenCompanies.any { it.equals(company, ignoreCase = true) } -> CompanyStatus.FORBIDDEN
            else -> CompanyStatus.NEUTRAL
        }
    }

    if (showAddDialog != null) {
        AddCompanyDialog(
            status = showAddDialog!!,
            onConfirm = { companyName ->
                when (showAddDialog) {
                    CompanyStatus.ALLOWED -> onAddAllowed(companyName)
                    CompanyStatus.FORBIDDEN -> onAddForbidden(companyName)
                    else -> {}
                }
                showAddDialog = null
            },
            onDismiss = { showAddDialog = null }
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
                    text = "Regole Aziende",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppText
                )
                Text(
                    text = "${allowedCompanies.size} consentite · ${forbiddenCompanies.size} vietate",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppMuted
                )
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // ALLOWED section
            item {
                SectionHeader(
                    title = "CONSENTITE",
                    count = allowedCompanies.size,
                    color = SuccessGreen,
                    onAdd = { showAddDialog = CompanyStatus.ALLOWED }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(allowedCompanies.sortedBy { it.lowercase() }) { company ->
                CompanyCard(
                    name = company,
                    status = CompanyStatus.ALLOWED,
                    onToggle = { onRemoveAllowed(company) },
                    toggleLabel = "Rimuovi"
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // FORBIDDEN section
            item {
                SectionHeader(
                    title = "VIETATE",
                    count = forbiddenCompanies.size,
                    color = ErrorRed,
                    onAdd = { showAddDialog = CompanyStatus.FORBIDDEN }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(forbiddenCompanies.sortedBy { it.lowercase() }) { company ->
                CompanyCard(
                    name = company,
                    status = CompanyStatus.FORBIDDEN,
                    onToggle = { onRemoveForbidden(company) },
                    toggleLabel = "Rimuovi"
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    color: Color,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.4.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "($count)",
                style = MaterialTheme.typography.labelSmall,
                color = AppMuted
            )
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f))
                    .border(1.dp, color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = color, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun CompanyCard(
    name: String,
    status: CompanyStatus,
    onToggle: () -> Unit,
    toggleLabel: String
) {
    val color = when (status) {
        CompanyStatus.ALLOWED -> SuccessGreen
        CompanyStatus.FORBIDDEN -> ErrorRed
        CompanyStatus.NEUTRAL -> AppMuted
    }
    val bgColor = color.copy(alpha = 0.05f)
    val borderColor = color.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppText,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ErrorRed.copy(alpha = 0.08f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = toggleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = ErrorRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AddCompanyDialog(
    status: CompanyStatus,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val isValid = name.trim().length >= 2
    val color = if (status == CompanyStatus.ALLOWED) SuccessGreen else ErrorRed
    val label = if (status == CompanyStatus.ALLOWED) "Consentita" else "Vietata"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Aggiungi azienda $label", color = AppText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Questa azienda sarà aggiunta alla lista $label su tutti i dispositivi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppMuted
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome azienda *") },
                    placeholder = { Text("es. Max Streicher") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = color,
                        unfocusedBorderColor = AppBorder,
                        focusedLabelColor = color,
                        unfocusedLabelColor = AppMuted,
                        focusedTextColor = AppText,
                        unfocusedTextColor = AppText,
                        cursorColor = color
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) { Text("Aggiungi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", color = AppMuted) }
        }
    )
}
