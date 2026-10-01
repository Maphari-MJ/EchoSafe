package com.echosafe.ui.calibration

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.echosafe.databinding.ActivityCalibrationBinding
import com.echosafe.service.AudioClassifierEngine
import com.echosafe.service.SpeechRecognitionManager
import com.echosafe.ui.dashboard.MainActivity

class CalibrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalibrationBinding
    private val viewModel: CalibrationViewModel by viewModels()

    private var normalRmsBaseline = 45.0
    private var screamPitchThreshold = 550.0
    private var recordedKeyphrase: String = ""
    private var recordedSafeWord: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private var isVisualizing = true

    // AudioRecord for voice calibration
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private val recordedSamples = mutableListOf<Short>()

    // SpeechRecognition for keyphrase / safe-word
    private lateinit var speechManager: SpeechRecognitionManager
    private var isRecordingKeyphrase = false
    private var isRecordingSafeWord = false

    private val classifier = AudioClassifierEngine()

    private val waveRunnable = object : Runnable {
        override fun run() {
            if (isVisualizing) {
                val amp = if (isRecording || isRecordingKeyphrase || isRecordingSafeWord) {
                    0.5f + (0..10).random() / 10f * 0.5f
                } else {
                    0.2f + (0..10).random() / 10f * 0.2f
                }
                binding.waveformVisualizer.setAmplitude(amp)
                handler.postDelayed(this, 120)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Microphone permission is required for voice calibration", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        speechManager = SpeechRecognitionManager(this)

        setupListeners()
        observeViewModel()

        viewModel.loadProfile()
        handler.post(waveRunnable)
    }

    private fun setupListeners() {
        // 1. Normal voice calibration
        binding.btnRecordNormal.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startVoiceRecording("normal")
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopVoiceRecording()
                    true
                }
                else -> false
            }
        }

        // 2. Excited voice calibration
        binding.btnRecordExcited.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startVoiceRecording("excited")
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopVoiceRecording()
                    true
                }
                else -> false
            }
        }

        // 3. Scream calibration
        binding.btnRecordScream.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startVoiceRecording("scream")
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopVoiceRecording()
                    true
                }
                else -> false
            }
        }

        // Keyphrase recording
        binding.btnRecordKeyphrase.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startKeyphraseRecording()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopKeyphraseRecording()
                    true
                }
                else -> false
            }
        }

        // Safe-word recording
        binding.btnRecordSafeWord.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startSafeWordRecording()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopSafeWordRecording()
                    true
                }
                else -> false
            }
        }

        // Save button
        binding.btnSaveCalibration.setOnClickListener {
            val keyphraseToSave = recordedKeyphrase.ifEmpty { "Echo emergency" }
            val safeWordToSave = recordedSafeWord.ifEmpty { "Blue sky" }

            viewModel.saveCalibration(
                normalRms = normalRmsBaseline,
                screamThreshold = screamPitchThreshold,
                secretKeyphrase = keyphraseToSave,
                safeWord = safeWordToSave
            )

            Toast.makeText(this, "Acoustic Profile calibrated & saved with SHA-256 integrity", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // ── Voice Calibration (AudioRecord) ──────────────────────────────────────

    private var activeRecordingType: String = "normal"

    private fun startVoiceRecording(type: String) {
        if (isRecording) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        activeRecordingType = type
        recordedSamples.clear()
        isRecording = true

        val sampleRate = AudioClassifierEngine.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = minBufferSize.coerceAtLeast(AudioClassifierEngine.SAMPLES_PER_CHUNK * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(this, "Microphone initialization failed", Toast.LENGTH_SHORT).show()
                isRecording = false
                return
            }

            audioRecord?.startRecording()

            recordingThread = Thread {
                val buffer = ShortArray(AudioClassifierEngine.SAMPLES_PER_CHUNK)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        for (i in 0 until read) {
                            recordedSamples.add(buffer[i])
                        }
                    }
                }
            }
            recordingThread?.start()

            // Update UI
            when (type) {
                "normal" -> binding.tvNormalStatus.text = "Recording... speak normally"
                "excited" -> binding.tvExcitedStatus.text = "Recording... speak loudly"
                "scream" -> binding.tvScreamStatus.text = "Recording... scream!"
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
            isRecording = false
        }
    }

    private fun stopVoiceRecording() {
        if (!isRecording) return

        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread?.join(1000)
        } catch (e: Exception) {
            // Ignore
        }

        // Analyze recorded sound audio
        if (recordedSamples.isNotEmpty()) {
            val buffer = recordedSamples.toShortArray()
            val result = classifier.processBuffer(buffer, normalRmsBaseline, screamPitchThreshold)

            when (activeRecordingType) {
                "normal" -> {
                    normalRmsBaseline = result.currentRms
                    binding.tvNormalStatus.text = "Calibrated ✓ (Ambient RMS: %.1f dB)".format(result.currentRms)
                }
                "excited" -> {
                    binding.tvExcitedStatus.text = "Calibrated ✓ (Pitch strain: %.0f Hz, RMS: %.1f dB)".format(result.estimatedPitchHz, result.currentRms)
                }
                "scream" -> {
                    screamPitchThreshold = result.estimatedPitchHz.coerceAtLeast(350.0)
                    binding.tvScreamStatus.text = "Calibrated ✓ (Pitch: %.0f Hz, Peak: %.1f dB)".format(result.estimatedPitchHz, result.currentRms)
                }
            }

            Toast.makeText(this, "Voice sound calibration complete for $activeRecordingType audio", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Keyphrase Recording (SpeechRecognizer) ───────────────────────────────

    private fun startKeyphraseRecording() {
        if (isRecordingKeyphrase) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (!speechManager.isRecognitionAvailable()) {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show()
            return
        }

        isRecordingKeyphrase = true
        binding.tvKeyphraseStatus.text = "Listening... speak your keyphrase"
        binding.btnRecordKeyphrase.text = "Recording..."

        speechManager.startListening(object : SpeechRecognitionManager.RecognitionCallback {
            override fun onResult(text: String) {
                runOnUiThread {
                    recordedKeyphrase = text
                    binding.tvKeyphraseStatus.text = "Keyphrase: \"$text\""
                    binding.btnRecordKeyphrase.text = "Hold to Record Keyphrase"
                    isRecordingKeyphrase = false
                    Toast.makeText(this@CalibrationActivity, "Keyphrase recorded: $text", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    binding.tvKeyphraseStatus.text = "Error: $error. Try again."
                    binding.btnRecordKeyphrase.text = "Hold to Record Keyphrase"
                    isRecordingKeyphrase = false
                }
            }

            override fun onPartialResult(text: String) {
                runOnUiThread {
                    binding.tvKeyphraseStatus.text = "Hearing: \"$text\""
                }
            }

            override fun onReadyForSpeech() {}
            override fun onBeginningOfSpeech() {
                runOnUiThread {
                    binding.tvKeyphraseStatus.text = "Speak now..."
                }
            }
            override fun onEndOfSpeech() {}
        })
    }

    private fun stopKeyphraseRecording() {
        if (!isRecordingKeyphrase) return
        speechManager.stopListening()
        isRecordingKeyphrase = false
    }

    // ── Safe-Word Recording (SpeechRecognizer) ───────────────────────────────

    private fun startSafeWordRecording() {
        if (isRecordingSafeWord) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (!speechManager.isRecognitionAvailable()) {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show()
            return
        }

        isRecordingSafeWord = true
        binding.tvSafeWordStatus.text = "Listening... speak your safe-word"
        binding.btnRecordSafeWord.text = "Recording..."

        speechManager.startListening(object : SpeechRecognitionManager.RecognitionCallback {
            override fun onResult(text: String) {
                runOnUiThread {
                    recordedSafeWord = text
                    binding.tvSafeWordStatus.text = "Safe-Word: \"$text\""
                    binding.btnRecordSafeWord.text = "Hold to Record Safe-Word"
                    isRecordingSafeWord = false
                    Toast.makeText(this@CalibrationActivity, "Safe-Word recorded: $text", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    binding.tvSafeWordStatus.text = "Error: $error. Try again."
                    binding.btnRecordSafeWord.text = "Hold to Record Safe-Word"
                    isRecordingSafeWord = false
                }
            }

            override fun onPartialResult(text: String) {
                runOnUiThread {
                    binding.tvSafeWordStatus.text = "Hearing: \"$text\""
                }
            }

            override fun onReadyForSpeech() {}
            override fun onBeginningOfSpeech() {
                runOnUiThread {
                    binding.tvSafeWordStatus.text = "Speak now..."
                }
            }
            override fun onEndOfSpeech() {}
        })
    }

    private fun stopSafeWordRecording() {
        if (!isRecordingSafeWord) return
        speechManager.stopListening()
        isRecordingSafeWord = false
    }

    // ── ViewModel Observation ────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.profile.observe(this) { profile ->
            if (profile != null) {
                normalRmsBaseline = profile.baselineRMS
                screamPitchThreshold = profile.distressPitchThreshold
                binding.tvNormalStatus.text = "Baseline: %.1f dB".format(profile.baselineRMS)
                binding.tvScreamStatus.text = "Threshold: %.0f Hz".format(profile.distressPitchThreshold)
            }
        }
    }

    override fun onDestroy() {
        isVisualizing = false
        handler.removeCallbacks(waveRunnable)
        speechManager.destroy()
        if (isRecording) {
            audioRecord?.stop()
            audioRecord?.release()
        }
        super.onDestroy()
    }
}
