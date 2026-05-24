package com.example.canteen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.canteen.data.VerificationResult
import com.example.canteen.ui.theme.Black
import com.example.canteen.ui.theme.ErrorBackground
import com.example.canteen.ui.theme.SuccessBackground
import com.example.canteen.ui.theme.White

@Composable
fun ResultScreen(
    result: VerificationResult,
    onNextClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val backgroundColor = when (result) {
        is VerificationResult.Success -> SuccessBackground
        is VerificationResult.Failure -> ErrorBackground
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val titleText = when (result) {
                is VerificationResult.Success -> "ENJOY YOUR MEAL"
                is VerificationResult.Failure -> "DENIED"
            }

            val detailsText = when (result) {
                is VerificationResult.Success -> {
                    if (result.isFuzzyMatch) {
                        result.matchedName.uppercase()
                    } else {
                        result.normalizedName.uppercase()
                    }
                }
                is VerificationResult.Failure -> {
                    when (result.reason) {
                        VerificationResult.Failure.Reason.LIMIT_REACHED  -> "Limit Reached"
                        VerificationResult.Failure.Reason.UNKNOWN_USER   -> "Unknown User"
                        VerificationResult.Failure.Reason.BLACK_LISTED   -> result.company ?: "Not Allowed"
                    }
                }
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.displayLarge,
                color = White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = detailsText,
                style = MaterialTheme.typography.headlineSmall,
                color = White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            if (result is VerificationResult.Failure && result.reason == VerificationResult.Failure.Reason.UNKNOWN_USER) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.scannedName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(72.dp))

            Button(
                onClick = onNextClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = White,
                    contentColor = Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    text = "NEXT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onHomeClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = White),
                border = androidx.compose.foundation.BorderStroke(1.dp, White.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "HOME",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}
