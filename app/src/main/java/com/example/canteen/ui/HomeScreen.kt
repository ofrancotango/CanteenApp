package com.example.canteen.ui

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.canteen.ui.theme.AppAccent
import com.example.canteen.ui.theme.AppAccentLight
import com.example.canteen.ui.theme.AppBackground
import com.example.canteen.ui.theme.AppBorder
import com.example.canteen.ui.theme.AppMuted
import com.example.canteen.ui.theme.AppSurface
import com.example.canteen.ui.theme.AppText
import com.example.canteen.ui.theme.AppWhite
import com.example.canteen.ui.theme.SuccessGreen
import com.example.canteen.ui.theme.ErrorRed

@Composable
fun HomeScreen(
    scansToday: Int,
    scanStatus: String,
    expectedAttendance: Int,
    onScanClick: () -> Unit,
    onStatsClick: () -> Unit,
    onTodayUsersClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    var showDebugDialog by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    if (showDebugDialog) {
        DebugBottomSheet(
            scanStatus = scanStatus,
            onCopy = {
                clipboardManager.setText(AnnotatedString(scanStatus))
                copied = true
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            },
            onRefresh = onRefreshClick,
            onDismiss = { showDebugDialog = false; copied = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAdminClick) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin",
                    tint = AppMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = { showDebugDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Debug",
                    tint = AppMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Text(
            text = "Streicher Group",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.5.sp,
            color = AppMuted
        )
        Text(
            text = "Canteen",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
            fontWeight = FontWeight.Bold,
            color = AppText
        )
        Text(
            text = "Access Control System",
            style = MaterialTheme.typography.bodyMedium,
            color = AppMuted,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Counter card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "TODAY'S ENTRIES",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    color = AppMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$scansToday",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = AppText,
                        lineHeight = 52.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "/ $expectedAttendance expected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AppBorder)
                ) {
                    val progress = if (expectedAttendance > 0) scansToday.toFloat() / expectedAttendance else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.horizontalGradient(listOf(AppAccent, AppAccentLight)))
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "\u2713 admitted", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                    Text(text = "\u2715 denied", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main scan button
        androidx.compose.material3.FilledIconButton(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Transparent
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(AppAccent, AppAccentLight))),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = AppWhite
                    )
                    Text(
                        text = "SCAN QR CODE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppWhite,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation rows
        NavRow(
            icon = Icons.Default.People,
            label = "Today's Users",
            sub = "$scansToday scans",
            onClick = onTodayUsersClick
        )

        NavRow(
            icon = Icons.Default.BarChart,
            label = "Statistics",
            sub = "Last 30 days",
            onClick = onStatsClick
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun NavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sub: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppText,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppMuted,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DebugBottomSheet(
    scanStatus: String,
    onCopy: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Debug Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppText
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(AppBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("\u00d7", color = AppMuted, fontSize = 16.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppBackground)
                            .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = scanStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppMuted.copy(alpha = 0.7f),
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
                        Text("Copy to Clipboard", color = AppAccent)
                    }
                    TextButton(onClick = { onRefresh(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Force Refresh", color = AppMuted)
                    }
                }
            }
        }
    }
}
