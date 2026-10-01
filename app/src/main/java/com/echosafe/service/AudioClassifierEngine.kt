package com.echosafe.service

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * AudioClassifierEngine combines TensorFlow Lite AI classification
 * with real-time acoustic feature extraction (RMS dB & pitch Hz estimation).
 * Uses user-calibrated dynamic thresholds to accurately detect screams,
 * vocal strain, and distress audio events.
 */
class AudioClassifierEngine(private val context: Context? = null) {

    companion object {
        private const val TAG = "AudioClassifierEngine"
        const val SAMPLE_RATE = 16000 // 16 kHz
        const val BUFFER_CHUNK_MS = 100 // 100ms sliding buffer window
        const val SAMPLES_PER_CHUNK = (SAMPLE_RATE * BUFFER_CHUNK_MS) / 1000 // 1600 samples
        private const val MODEL_FILENAME = "sound_classifier.tflite"
    }

    private var tfliteInterpreter: Interpreter? = null

    init {
        initTFLiteInterpreter()
    }

    private fun initTFLiteInterpreter() {
        if (context == null) return
        try {
            val assetFileDescriptor = context.assets.openFd(MODEL_FILENAME)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            tfliteInterpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "TensorFlow Lite AI Interpreter initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "TFLite model asset could not be loaded into Interpreter (${e.message}); using fallback acoustic feature classifier.")
        }
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
     * Evaluates RMS energy, pitch spectral centroid, scream characteristics,
     * and runs TFLite AI classification.
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

        // 3. TensorFlow Lite AI Classifier Inference (if model initialized)
        var tfliteScreamConfidence = 0.0f
        var tfliteStrainConfidence = 0.0f

        tfliteInterpreter?.let { interpreter ->
            try {
                // Convert PCM ShortArray into normalized FloatArray [-1.0, 1.0]
                val floatInput = Array(1) { FloatArray(pcmBuffer.size) }
                for (i in pcmBuffer.indices) {
                    floatInput[0][i] = pcmBuffer[i] / 32768.0f
                }
                // Output logits/probs array: [1, 4] -> [Scream, Vocal Strain, Normal Speech, Ambient]
                val outputProbs = Array(1) { FloatArray(4) }
                interpreter.run(floatInput, outputProbs)

                tfliteScreamConfidence = outputProbs[0][0]
                tfliteStrainConfidence = outputProbs[0][1]
            } catch (e: Exception) {
                // TFLite inference exception handled gracefully
            }
        }

        // 4. Dynamic Calibrated Threshold Classification (Scream, Vocal Strain, Pitch Spike)
        val isRmsElevated = rmsDb > (baselineRms + 16.0) || rmsDb > 72.0
        val isPitchExceeded = estimatedPitchHz > screamPitchThreshold && estimatedPitchHz in 350.0..4000.0

        var isDanger = false
        var triggerType = ""
        var confidence = 0.0f

        if ((isRmsElevated && isPitchExceeded) || tfliteScreamConfidence > 0.65f) {
            isDanger = true
            triggerType = "PROLONGED_SCREAM"
            confidence = max(0.92f, tfliteScreamConfidence)
        } else if ((isPitchExceeded && rmsDb > 62.0) || tfliteStrainConfidence > 0.60f) {
            isDanger = true
            triggerType = "VOCAL_STRAIN_PITCH_SPIKE"
            confidence = max(0.85f, tfliteStrainConfidence)
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

    fun close() {
        try {
            tfliteInterpreter?.close()
            tfliteInterpreter = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
