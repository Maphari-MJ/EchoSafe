package com.echosafe.service

import android.content.Context
import android.util.Log
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class AudioClassifierEngine(private val context: Context) {

    companion object {
        private const val TAG = "AudioClassifierEngine"
        const val SAMPLE_RATE = 16000 // 16 kHz
        const val BUFFER_CHUNK_MS = 100 // 100ms sliding buffer window
        const val SAMPLES_PER_CHUNK = (SAMPLE_RATE * BUFFER_CHUNK_MS) / 1000 // 1600 samples
    }

    data class ClassificationResult(
        val isDangerDetected: Boolean,
        val triggerType: String,
        val confidence: Float,
        val currentRms: Double,
        val estimatedPitchHz: Double
    )

    /**
     * Processes a 16-bit Mono PCM buffer chunk (100ms)
     * Evaluates RMS energy, pitch spectral centroid, and scream characteristics
     */
    fun processBuffer(
        pcmBuffer: ShortArray,
        baselineRms: Double,
        screamPitchThreshold: Double
    ): ClassificationResult {
        if (pcmBuffer.isEmpty()) {
            return ClassificationResult(false, "", 0f, 0.0, 0.0)
        }

        // 1. Calculate RMS Energy (dB)
        var sumSquares = 0.0
        for (sample in pcmBuffer) {
            sumSquares += sample * sample
        }
        val meanSquare = sumSquares / pcmBuffer.size
        val rmsRaw = sqrt(meanSquare)
        val rmsDb = if (rmsRaw > 0) 20 * log10(rmsRaw / 32768.0) + 90 else 0.0

        // 2. Pitch / Dominant Frequency Estimation via Zero-Crossing & Autocorrelation
        val estimatedPitchHz = estimatePitch(pcmBuffer, SAMPLE_RATE)

        // 3. Dynamic Threshold Classification (Scream, Vocal Strain, Pitch Spike)
        val isRmsElevated = rmsDb > (baselineRms + 18.0) || rmsDb > 75.0
        val isPitchExceeded = estimatedPitchHz > screamPitchThreshold && estimatedPitchHz in 400.0..3500.0

        var isDanger = false
        var triggerType = ""
        var confidence = 0.0f

        if (isRmsElevated && isPitchExceeded) {
            isDanger = true
            triggerType = "PROLONGED_SCREAM"
            confidence = 0.92f
        } else if (isPitchExceeded && rmsDb > 65.0) {
            isDanger = true
            triggerType = "VOCAL_STRAIN_PITCH_SPIKE"
            confidence = 0.85f
        }

        return ClassificationResult(
            isDangerDetected = isDanger,
            triggerType = triggerType,
            confidence = confidence,
            currentRms = rmsDb,
            estimatedPitchHz = estimatedPitchHz
        )
    }

    private fun estimatePitch(buffer: ShortArray, sampleRate: Int): Double {
        // Zero-crossing rate estimate
        var zeroCrossings = 0
        for (i in 1 until buffer.size) {
            if ((buffer[i] >= 0 && buffer[i - 1] < 0) || (buffer[i] < 0 && buffer[i - 1] >= 0)) {
                zeroCrossings++
            }
        }
        val zcrPitch = (zeroCrossings.toDouble() * sampleRate) / (2.0 * buffer.size)

        // Refined Autocorrelation for pitch estimation
        val maxLag = (sampleRate / 80).coerceAtMost(buffer.size / 2) // Min 80Hz
        val minLag = (sampleRate / 3000).coerceAtLeast(1) // Max 3000Hz
        var bestLag = -1
        var maxCorr = 0.0

        for (lag in minLag..maxLag) {
            var corr = 0.0
            for (i in 0 until (buffer.size - lag)) {
                corr += buffer[i] * buffer[i + lag]
            }
            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        val autoPitch = if (bestLag > 0) sampleRate.toDouble() / bestLag else zcrPitch
        return if (autoPitch in 50.0..4000.0) (autoPitch * 0.7 + zcrPitch * 0.3) else zcrPitch
    }
}
