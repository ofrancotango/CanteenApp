package com.example.canteen.data

sealed class VerificationResult {
    data class Success(
        val originalName: String,
        val normalizedName: String,
        val matchedName: String,
        val isFuzzyMatch: Boolean = false,
        val requiresNote: Boolean = false,
        val timestamp: Long = 0L
    ) : VerificationResult()
    data class Failure(
        val reason: Reason,
        val scannedName: String,
        val company: String? = null,
        val timestamp: Long = 0L
    ) : VerificationResult() {
        enum class Reason {
            LIMIT_REACHED,
            UNKNOWN_USER,
            BLACK_LISTED
        }
    }
}
