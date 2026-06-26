package com.example.canteen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canteen.data.VerificationResult
import com.example.canteen.ui.theme.AppAccent
import com.example.canteen.ui.theme.AppBorder
import com.example.canteen.ui.theme.AppMuted
import com.example.canteen.ui.theme.AppText
import com.example.canteen.ui.theme.AppWhite
import com.example.canteen.ui.theme.ErrorBackground
import com.example.canteen.ui.theme.ErrorRed
import com.example.canteen.ui.theme.SuccessBackground
import com.example.canteen.ui.theme.SuccessGreen

@Composable
fun ResultScreen(
    result: VerificationResult,
    onNextClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val isSuccess = result is VerificationResult.Success
    val bgColor = if (isSuccess) SuccessBackground else ErrorBackground
    val accentColor = if (isSuccess) SuccessGreen else ErrorRed
    val title = if (isSuccess) "ENJOY YOUR MEAL" else "ACCESS DENIED"
    val subtitle = if (isSuccess) "Accesso consentito" else "Accesso negato"

    val detailsText = when (result) {
        is VerificationResult.Success -> {
            val name = if (result.isFuzzyMatch) result.matchedName else result.normalizedName
            name
        }
        is VerificationResult.Failure -> {
            when (result.reason) {
                VerificationResult.Failure.Reason.LIMIT_REACHED -> "Limit reached for today"
                VerificationResult.Failure.Reason.UNKNOWN_USER -> "Not whitelisted"
                VerificationResult.Failure.Reason.BLACK_LISTED -> result.company ?: "Not Allowed"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Big icon circle
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f))
                    .border(2.dp, accentColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSuccess) "\u2713" else "\u2715",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.4.sp,
                color = accentColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                fontWeight = FontWeight.ExtraBold,
                color = AppText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = detailsText,
                style = MaterialTheme.typography.bodyLarge,
                color = AppMuted,
                textAlign = TextAlign.Center
            )

            if (isSuccess && result is VerificationResult.Success) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.08f))
                        .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Entry #\u00b7 ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.7f)
                    )
                }
            }

            if (result is VerificationResult.Failure && result.reason == VerificationResult.Failure.Reason.UNKNOWN_USER) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.scannedName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppMuted.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(40.dp))

            // SCAN NEXT BADGE
            Button(
                onClick = onNextClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = AppWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "SCAN NEXT BADGE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = AppMuted)
            ) {
                Text(
                    text = "\u2190 Back to Home",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

