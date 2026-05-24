package com.example.canteen.utils

fun main() {
    println("=== Testing Smart Token Matcher ===")

    val cases = listOf(
        Triple("Silviu Constantin Truica", "Truica Silviu Laurentiu", true), // Case A: 2/3 matches (66%) -> PASS
        Triple("Joao Rosado", "Fernandes Rosado Joao Carlos", true),         // Case B: Subset (100%) -> PASS
        Triple("Molajan", "Mola Jan", true),                                 // Case C: Merged (100% via merge) -> PASS
        Triple("Mario Rossi", "Mario Verdi", false),                         // Fail: 1/2 (50%) -> FAIL
        Triple("John", "John Doe", true),                                    // Pass: 1/1 (100%) -> PASS
        Triple("Short", "Shorter Name", false),                              // Fail: No match -> FAIL
        Triple("Wahid Kudussi", "Wahid Kudussi", true)                       // Exact -> PASS
    )

    var passed = 0
    var failed = 0

    for ((qr, db, expected) in cases) {
        val result = StringNormalizer.smartTokenMatch(qr, db)
        if (result == expected) {
            println("[PASS] QR='$qr' vs DB='$db' -> $result")
            passed++
        } else {
            println("[FAIL] QR='$qr' vs DB='$db' -> Expected $expected but got $result")
            failed++
        }
    }

    println("\nSummary: $passed Passed, $failed Failed.")
}
