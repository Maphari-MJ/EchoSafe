package com.echosafe.service

import com.echosafe.vault.CryptoHashUtils

class SecretKeyphraseDetector {

    /**
     * Checks if spoken text matches the calibrated secret keyphrase hash
     */
    fun matchesSecretKeyphrase(spokenText: String, expectedHash: String): Boolean {
        if (spokenText.isBlank() || expectedHash.isBlank()) return false
        val computed = CryptoHashUtils.sha256(spokenText)
        return computed.equals(expectedHash, ignoreCase = true)
    }

    /**
     * Checks if spoken text matches the safe word disarm hash
     */
    fun matchesSafeWord(spokenText: String, expectedHash: String): Boolean {
        if (spokenText.isBlank() || expectedHash.isBlank()) return false
        val computed = CryptoHashUtils.sha256(spokenText)
        return computed.equals(expectedHash, ignoreCase = true)
    }
}
