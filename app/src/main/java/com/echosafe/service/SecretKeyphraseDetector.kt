package com.echosafe.service

import com.echosafe.vault.CryptoHashUtils

class SecretKeyphraseDetector {

    companion object {
        val EMERGENCY_KEYWORDS = listOf(
            "help",
            "help me",
            "emergency",
            "call 911",
            "call police",
            "save me",
            "stop",
            "danger",
            "attack",
            "echosafe alert",
            "somebody help",
            "please help",
            "call security",
            "assistance"
        )
    }

    /**
     * Checks if spoken text matches the calibrated secret keyphrase hash
     */
    fun matchesSecretKeyphrase(spokenText: String, expectedHash: String): Boolean {
        if (spokenText.isBlank() || expectedHash.isBlank()) return false
        val cleanSpoken = spokenText.trim().lowercase()
        val computed = CryptoHashUtils.sha256(cleanSpoken)
        if (computed.equals(expectedHash, ignoreCase = true)) return true

        // Also check un-normalized
        val rawComputed = CryptoHashUtils.sha256(spokenText)
        return rawComputed.equals(expectedHash, ignoreCase = true)
    }

    /**
     * Checks if spoken text contains universal emergency keywords
     */
    fun matchesEmergencyKeyword(spokenText: String): Boolean {
        if (spokenText.isBlank()) return false
        val clean = spokenText.trim().lowercase()
        return EMERGENCY_KEYWORDS.any { keyword ->
            clean == keyword || clean.contains(keyword)
        }
    }

    /**
     * Checks if spoken text matches the safe word disarm hash
     */
    fun matchesSafeWord(spokenText: String, expectedHash: String): Boolean {
        if (spokenText.isBlank() || expectedHash.isBlank()) return false
        val cleanSpoken = spokenText.trim().lowercase()
        val computed = CryptoHashUtils.sha256(cleanSpoken)
        if (computed.equals(expectedHash, ignoreCase = true)) return true

        val rawComputed = CryptoHashUtils.sha256(spokenText)
        return rawComputed.equals(expectedHash, ignoreCase = true)
    }
}
