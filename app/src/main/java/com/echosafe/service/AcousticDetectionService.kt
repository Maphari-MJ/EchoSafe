package com.echosafe.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.echosafe.R
import com.echosafe.data.local.EchoSafeDatabase
import com.echosafe.data.local.OfflineTriggerEntity
import com.echosafe.data.repository.ContactsRepository
import com.echosafe.dispatch.EmergencyDispatchManager
import com.echosafe.ui.dashboard.MainActivity
import com.echosafe.vault.EvidentiaryVaultManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class AcousticDetectionService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isRunning = false
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var classifier: AudioClassifierEngine
    private lateinit var speechManager: SpeechRecognitionManager
    private val handler = Handler(Looper.getMainLooper())

    private var keyphraseHash: String = ""
    private var safeWordHash: String = ""

    companion object {
        private const val TAG = "AcousticService"
        const val CHANNEL_ID = "echosafe_acoustic_guard"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.echosafe.action.START_GUARD"
        const val ACTION_STOP = "com.echosafe.action.STOP_GUARD"
        const val ACTION_STATUS_CHANGED = "com.echosafe.action.STATUS_CHANGED"
        const val EXTRA_IS_ACTIVE = "extra_is_active"

        var isGuardActive = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        classifier = AudioClassifierEngine(this)
        speechManager = SpeechRecognitionManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startGuard()
            ACTION_STOP -> stopGuard()
            else -> {
                if (!isRunning) startGuard()
            }
        }
        return START_STICKY
    }

    private fun startGuard() {
        if (isRunning) return
        isRunning = true
        isGuardActive = true
        broadcastStatus(true)

        // Acquire WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EchoSafe::AcousticWakeLock").apply {
            acquire(24 * 60 * 60 * 1000L) // 24 hours
        }

        // Start Foreground Notification
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Load keyphrase hash from database
        scope.launch {
            val db = EchoSafeDatabase.getInstance(applicationContext)
            val profile = db.calibrationDao().getProfile()
            keyphraseHash = profile?.secretKeyphraseHash ?: ""
            safeWordHash = profile?.safeWordHash ?: ""
        }

        // Start Audio Recording Loop
        startAudioLoop()

        // Start Speech Recognition for keyphrase & keyword detection
        startSpeechRecognition()

        Log.d(TAG, "Continuous Acoustic Engine Guard Started.")
    }

    private fun stopGuard() {
        isRunning = false
        isGuardActive = false
        broadcastStatus(false)

        recordingJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audioRecord", e)
        }

        speechManager.stopListening()
        speechManager.destroy()
        classifier.close()

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Continuous Acoustic Engine Guard Stopped.")
    }

    private fun startAudioLoop() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission missing, stopping guard service")
            stopGuard()
            return
        }

        val sampleRate = AudioClassifierEngine.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val chunkSize = AudioClassifierEngine.SAMPLES_PER_CHUNK
        val bufferSize = minBufferSize.coerceAtLeast(chunkSize * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            audioRecord?.startRecording()

            recordingJob = scope.launch {
                val db = EchoSafeDatabase.getInstance(applicationContext)
                val profile = db.calibrationDao().getProfile()
                val baselineRms = profile?.normalPitchBaseline ?: 45.0
                val screamThreshold = profile?.screamPitchThreshold ?: 500.0

                val buffer = ShortArray(chunkSize)
                var consecutiveDangerCount = 0

                while (isActive && isRunning) {
                    val read = audioRecord?.read(buffer, 0, chunkSize) ?: -1
                    if (read > 0) {
                        val result = classifier.processBuffer(buffer, baselineRms, screamThreshold)
                        if (result.isDangerDetected) {
                            consecutiveDangerCount++
                            if (consecutiveDangerCount >= 2) { // 2 consecutive chunks (200ms) of scream/strain
                                consecutiveDangerCount = 0
                                onAcousticThreatDetected(result.triggerType)
                            }
                        } else {
                            consecutiveDangerCount = (consecutiveDangerCount - 1).coerceAtLeast(0)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in AudioRecord loop", e)
        }
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "Speech recognition not available on this device")
            return
        }

        speechManager.startListening(object : SpeechRecognitionManager.RecognitionCallback {
            override fun onResult(text: String) {
                Log.d(TAG, "Speech recognized: $text")
                val detector = SecretKeyphraseDetector()

                // Check keyphrase OR universal emergency keywords (help, emergency, save me, stop, danger)
                if (detector.matchesSecretKeyphrase(text, keyphraseHash) || detector.matchesEmergencyKeyword(text)) {
                    Log.w(TAG, "KEYPHRASE OR EMERGENCY KEYWORD DETECTED: $text")
                    onAcousticThreatDetected("KEYPHRASE_KEYWORD_DETECTED")
                }

                // Restart listening for continuous detection
                if (isRunning) {
                    handler.postDelayed({
                        if (isRunning) {
                            speechManager.startListening(this)
                        }
                    }, 500)
                }
            }

            override fun onError(error: String) {
                Log.w(TAG, "Speech recognition error: $error")
                // Restart listening after a delay
                if (isRunning) {
                    handler.postDelayed({
                        if (isRunning) {
                            speechManager.startListening(this)
                        }
                    }, 2000)
                }
            }

            override fun onPartialResult(text: String) {
                val detector = SecretKeyphraseDetector()
                if (detector.matchesSecretKeyphrase(text, keyphraseHash) || detector.matchesEmergencyKeyword(text)) {
                    Log.w(TAG, "PARTIAL KEYPHRASE / KEYWORD DETECTED: $text")
                    onAcousticThreatDetected("KEYPHRASE_KEYWORD_DETECTED")
                }
            }

            override fun onReadyForSpeech() {}
            override fun onBeginningOfSpeech() {}
            override fun onEndOfSpeech() {}
        })
    }

    /**
     * Called when screaming, vocal strain, keyphrases, or emergency keywords are detected.
     * STEALTH SECURITY REQUIREMENTS:
     * 1. Does NOT reflect on user's screen (no loud emergency popups or screen wake).
     * 2. Vibrates discrete non-continuous pulses (vibrates only a few times).
     * 3. Plays 3 nicely paced notification bings.
     * 4. Dispatches emergency alert to emergency contacts, cloud, SMS, & USSD gateways.
     */
    private fun onAcousticThreatDetected(triggerType: String) {
        Log.w(TAG, "Acoustic Threat Activated: $triggerType. Executing Stealth Emergency Dispatch...")

        // 1. Discrete Haptic Vibration (vibrates only a few times, NOT continuously)
        triggerDiscreteVibration()

        // 2. Play 3 Nicely Paced Notification Bings
        playThreeNotificationBings()

        // 3. Covert Silent Emergency Dispatch (screen does NOT show emergency activation)
        executeSilentEmergencyDispatch(triggerType)
    }

    private fun triggerDiscreteVibration() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                val discretePattern = longArrayOf(0, 140, 200, 140, 200, 140) // 3 short subtle pulses
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(discretePattern, -1)) // -1 = NO repeat loop
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(discretePattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration feedback error", e)
        }
    }

    private fun playThreeNotificationBings() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 160) // Bing 1
            handler.postDelayed({
                try { toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 160) } catch (e: Exception) {} // Bing 2
            }, 600)
            handler.postDelayed({
                try {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 160) // Bing 3
                    handler.postDelayed({ toneGenerator.release() }, 300)
                } catch (e: Exception) {}
            }, 1200)
        } catch (e: Exception) {
            Log.e(TAG, "Audio bing playback error", e)
        }
    }

    private fun executeSilentEmergencyDispatch(triggerType: String) {
        scope.launch {
            try {
                val emergencyId = UUID.randomUUID().toString()
                val trigger = OfflineTriggerEntity(
                    triggerId = emergencyId,
                    timestamp = System.currentTimeMillis(),
                    triggerType = triggerType,
                    latitude = -26.2041,
                    longitude = 28.0473,
                    dispatchChannel = "FCM"
                )

                val contactsRepo = ContactsRepository(applicationContext)
                val contacts = contactsRepo.getAllContacts()
                val contactPhoneNumbers = contacts.map { it.phoneNumber }.filter { it.isNotBlank() }

                // Multi-Tier Dispatch (Cloud -> SMS -> USSD Zero-Airtime)
                val dispatcher = EmergencyDispatchManager(applicationContext)
                dispatcher.dispatchEmergency(trigger, contactPhoneNumbers)

                // Capture Silent Evidentiary Package
                val vaultManager = EvidentiaryVaultManager(applicationContext)
                vaultManager.captureEmergencyEvidencePackage(emergencyId, null)

                Log.d(TAG, "Stealth Emergency Dispatch completed silently for trigger: $triggerType")
            } catch (e: Exception) {
                Log.e(TAG, "Error in silent emergency dispatch", e)
            }
        }
    }

    private fun broadcastStatus(active: Boolean) {
        val intent = Intent(ACTION_STATUS_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_IS_ACTIVE, active)
        }
        sendBroadcast(intent)
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EchoSafe Guard: ACTIVE")
            .setContentText("Continuous Acoustic Engine listening for danger...")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EchoSafe Acoustic Guard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active background listening status for personal safety"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopGuard()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
