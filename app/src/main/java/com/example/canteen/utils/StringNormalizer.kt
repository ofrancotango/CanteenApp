package com.example.canteen.utils

import java.text.Normalizer
import java.util.Locale

object StringNormalizer {

    /**
     * Ultra-Normalization:
     * 1. Convert to lowercase.
     * 2. Remove accents/diacritics (NFKD).
     * 3. Replace dots '.' and hyphens '-' with spaces.
     * 4. Trim whitespace from ends.
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""

        // 1. Replace dots, hyphens, and commas with spaces
        // "Francesco-Pio.Tonelli" -> "Francesco Pio Tonelli"
        var processed = input.replace(".", " ").replace("-", " ").replace(",", " ")

        // 2. Convert to lowercase
        processed = processed.lowercase(Locale.getDefault())

        // 3. Remove accents (NFKD normalization)
        val normalized = Normalizer.normalize(processed, Normalizer.Form.NFD)
        val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        var withoutAccents = regex.replace(normalized, "")

        // Manual replacements for some common edge cases if needed (optional based on "ASCII" requirement)
        // For strictly ASCII, we might want to be more aggressive, but NFD covers most European names.
        withoutAccents = withoutAccents.replace("ł", "l")
                                       .replace("ø", "o")
                                       .replace("đ", "d")

        // 4. Trim and normalize internal whitespace (single spaces)
        // User requested: "Trim all whitespace from both ends". 
        // Also "matches francesco pio tonelli" implies single spaces between words.
        // RE-ENABLE SORTING: This solves "Surname Name" vs "Name Surname" AND Composite names issues 
        // by creating a canonical alphabetical order (e.g. "Kizhakedath Kuttappan Satheesan" -> "kizhakedath kuttappan satheesan").
        return withoutAccents.trim().split("\\s+".toRegex()).sorted().joinToString(" ")
    }

    /**
     * Jaro-Winkler Similarity
     * Returns a score between 0.0 (no match) and 1.0 (perfect match).
     */
    fun jaroWinkler(s1: String, s2: String): Double {
        val jaro = jaroDistance(s1, s2)
        
        // If Jaro score is high, boost it with Winkler prefix scale
        // Standard prefix scale is 0.1, max prefix length is 4
        if (jaro > 0.7) {
            val prefixLength = s1.commonPrefixWith(s2).length.coerceAtMost(4)
            return jaro + (prefixLength * 0.1 * (1.0 - jaro))
        }
        
        return jaro
    }

    fun jaroDistance(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0

        val len1 = s1.length
        val len2 = s2.length

        if (len1 == 0 || len2 == 0) return 0.0

        val matchDistance = (kotlin.math.max(len1, len2) / 2) - 1
        
        val s1Matches = BooleanArray(len1)
        val s2Matches = BooleanArray(len2)
        
        var matches = 0.0
        var transpositions = 0.0

        // Find matches
        for (i in 0 until len1) {
            val start = kotlin.math.max(0, i - matchDistance)
            val end = kotlin.math.min(i + matchDistance + 1, len2)
            
            for (j in start until end) {
                if (s2Matches[j]) continue
                if (s1[i] != s2[j]) continue
                
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0.0) return 0.0

        // Count transpositions
        var k = 0
        for (i in 0 until len1) {
            if (!s1Matches[i]) continue
            
            while (!s2Matches[k]) k++
            
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        return ((matches / len1) + (matches / len2) + ((matches - transpositions / 2.0) / matches)) / 3.0
    }


    /**
     * Token-Based Matcher
     * Splits both strings by space and checks if ALL tokens in [name1] (Key) 
     * find a match in [name2] (Input) with score >= [threshold].
     * Returns the average score of the best matches.
     */
    fun tokenBasedMatch(name1: String, name2: String, threshold: Double): Double {
        val tokens1 = name1.split(" ").filter { it.isNotBlank() }
        val tokens2 = name2.split(" ").filter { it.isNotBlank() }

        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0

        var totalScore = 0.0

        // We require that every token in the Key (tokens1) matches something in the Input (tokens2)
        for (token1 in tokens1) {
            var bestSubScore = 0.0
            
            for (token2 in tokens2) {
                val score = jaroWinkler(token1, token2)
                if (score > bestSubScore) {
                    bestSubScore = score
                }
            }

            if (bestSubScore < threshold) {
                return 0.0 // Fail: A required name part was missing or didn't match well enough
            }
            totalScore += bestSubScore
        }

        return totalScore / tokens1.size
    }

    /**
     * Smart Token Intersection (Solving Unknown Users)
     * 
     * Rules:
     * 1. Tokenize: Split QR and DB Name into word lists. Remove tokens < 3 letters.
     * 2. Intersection Count: Count how many QR tokens have a Fuzzy Match (>0.90) with ANY DB token.
     * 3. Merged Check: If a QR token doesn't match, check if it matches DB_Token_A + DB_Token_B.
     * 4. Golden Rule: If (Matched_Tokens / Total_QR_Tokens) >= 0.66 -> GRANT ACCESS.
     */
    fun smartTokenMatch(qrName: String, dbName: String): Boolean {
        // 1. Tokenize & Filter
        // We use the normalized versions to ensure case/accent insensitivity
        val cleanQr = normalize(qrName) // uses our normalize which removes accents etc
        val cleanDb = normalize(dbName)

        val qrTokens = cleanQr.split(" ").filter { it.length >= 3 }
        val dbTokens = cleanDb.split(" ").filter { it.length >= 3 }

        if (qrTokens.isEmpty()) return false
        
        // If DB has no significant tokens, we can't match unless QR is also empty (handled above)
        if (dbTokens.isEmpty()) return false

        var matchedTokensCount = 0.0

        for (qrToken in qrTokens) {
            var isMatched = false

            // 2. Exact/Fuzzy Match with any DB Token
            for (dbToken in dbTokens) {
                if (jaroWinkler(qrToken, dbToken) > 0.90) {
                    isMatched = true
                    break
                }
            }

            // 3. Merged Check (if not already matched)
            if (!isMatched && dbTokens.size >= 2) {
                // Check combinations of any 2 DB tokens
                // "Molajan" vs "Mola" + "Jan"
                for (i in dbTokens.indices) {
                    if (isMatched) break
                    for (j in dbTokens.indices) {
                        if (i == j) continue
                        val mergedDb = dbTokens[i] + dbTokens[j]
                        if (jaroWinkler(qrToken, mergedDb) > 0.90) {
                            isMatched = true
                            break
                        }
                    }
                }
            }

            if (isMatched) {
                matchedTokensCount++
            }
        }

        // 4. Golden Rule
        val matchRatio = matchedTokensCount / qrTokens.size.toDouble()
        return matchRatio >= 0.66
    }
}
