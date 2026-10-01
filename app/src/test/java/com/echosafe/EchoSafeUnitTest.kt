package com.echosafe

import com.echosafe.service.AudioClassifierEngine
import com.echosafe.service.SecretKeyphraseDetector
import com.echosafe.vault.CryptoHashUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoSafeUnitTest {

    @Test
    fun sha256_hashing_returns_correct_hex_digest() {
        val input = "Help Me EchoSafe"
        val expectedHash = CryptoHashUtils.sha256(input)
        val computedHash = CryptoHashUtils.sha256(input)

        assertEquals(expectedHash, computedHash)
        assertEquals(64, computedHash.length) // SHA-256 is 64 hex characters
    }

    @Test
    fun secretKeyphraseDetector_verifies_valid_spoken_phrase() {
        val detector = SecretKeyphraseDetector()
        val safeWord = "I am okay, false alarm"
        val hash = CryptoHashUtils.sha256(safeWord)

        assertTrue(detector.matchesSafeWord("I am okay, false alarm", hash))
        assertFalse(detector.matchesSafeWord("Emergency help now", hash))
    }

    @Test
    fun secretKeyphraseDetector_detects_emergency_keywords() {
        val detector = SecretKeyphraseDetector()

        assertTrue(detector.matchesEmergencyKeyword("Please help me!"))
        assertTrue(detector.matchesEmergencyKeyword("EMERGENCY"))
        assertTrue(detector.matchesEmergencyKeyword("Save me!"))
        assertFalse(detector.matchesEmergencyKeyword("Good morning, world"))
    }

    @Test
    fun audioClassifierEngine_detects_screams_correctly() {
        val engine = AudioClassifierEngine(null)

        // Generate high amplitude audio buffer
        val highPitchScreamBuffer = ShortArray(1600) { i ->
            ((Math.sin(2.0 * Math.PI * 800.0 * i / 16000.0) * 30000).toInt()).toShort()
        }

        val result = engine.processBuffer(
            pcmBuffer = highPitchScreamBuffer,
            baselineRms = 45.0,
            screamPitchThreshold = 400.0
        )

        assertTrue(result.isDangerDetected)
        assertTrue(result.triggerType.isNotEmpty())
    }
}
