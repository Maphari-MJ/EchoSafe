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
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.echosafe.R
import com.echosafe.data.local.EchoSafeDatabase
import com.echosafe.ui.dashboard.MainActivity
import com.echosafe.ui.disarm.DisarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

        // Start Speech Recognition for keyphrase detection
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

        if (keyphraseHash.isEmpty()) {
            Log.w(TAG, "No keyphrase hash available, skipping speech recognition")
            return
        }

        speechManager.startListening(object : SpeechRecognitionManager.RecognitionCallback {
            override fun onResult(text: String) {
                Log.d(TAG, "Speech recognized: $text")

                // Check if the recognized text matches the keyphrase
                if (SecretKeyphraseDetector().matchesSecretKeyphrase(text, keyphraseHash)) {
                    Log.w(TAG, "KEYPHRASE DETECTED: $text")
                    onAcousticThreatDetected("KEYPHRASE_DETECTED")
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
                // Partial results available if needed
            }

            override fun onReadyForSpeech() {
                Log.d(TAG, "Speech recognition ready")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech recognition: beginning of speech")
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech recognition: end of speech")
            }
        })
    }

    private fun onAcousticThreatDetected(triggerType: String) {
        Log.w(TAG, "Acoustic Threat Detected: $triggerType. Launching Disarm Window...")
        val disarmIntent = Intent(this, DisarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(DisarmActivity.EXTRA_TRIGGER_TYPE, triggerType)
        }
        startActivity(disarmIntent)
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
