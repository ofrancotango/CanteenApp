package com.example.canteen.data

sealed class VerificationResult {
    data class Success(
        val originalName: String, 
        val normalizedName: String,
        val matchedName: String,
        val isFuzzyMatch: Boolean = false
    ) : VerificationResult()
    data class Failure(
        val reason: Reason, 
        val scannedName: String,
        val company: String? = null
    ) : VerificationResult() {
        enum class Reason {
            LIMIT_REACHED,
            UNKNOWN_USER,
            BLACK_LISTED
        }
    }
}
