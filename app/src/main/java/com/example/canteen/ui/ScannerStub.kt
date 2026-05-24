package com.example.canteen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A stub Scanner screen for simulation.
 * In a real device implementation, this would use CameraX to scan QR codes.
 * Here it allows manual entry to simulate a scan.
 */
@Composable
fun ScannerStub(
    onScanCode: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Simulate QR Scan", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter Name (e.g. Mario Giuseppe.Rossi)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { onScanCode(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Simulate Scan")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onCancel) {
                Text("Cancel")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Try inputs like:\n- Mario Giuseppe.Rossi\n- èxample user\n- Unknown Person",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
