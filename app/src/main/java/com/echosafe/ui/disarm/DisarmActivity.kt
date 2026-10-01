package com.echosafe.ui.disarm

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.echosafe.databinding.ActivityDisarmBinding
import com.echosafe.service.PowerButtonDetector
import com.echosafe.service.ShakeDetector
import com.echosafe.ui.vault.VaultActivity

class DisarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDisarmBinding
    private val viewModel: DisarmViewModel by viewModels()

    private var countdownSeconds = 10
    private var isDisarmed = false
    private var isAuthenticationInProgress = false
    private val handler = Handler(Looper.getMainLooper())
    private var vibrator: Vibrator? = null
    private var powerButtonDetector: PowerButtonDetector? = null
    private var shakeDetector: ShakeDetector? = null
    private var toneGenerator: ToneGenerator? = null
    private var biometricPrompt: BiometricPrompt? = null
    private var promptInfo: BiometricPrompt.PromptInfo? = null

    companion object {
        const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
    }

    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (isDisarmed || isAuthenticationInProgress) return

            countdownSeconds--
            binding.tvCountdownSeconds.text = countdownSeconds.toString()

            if (countdownSeconds > 0) {
                if (countdownSeconds >= 7) { // 3-bing audio cue over first 3 seconds
                    playBingTone()
                }
                triggerHapticPulse()
                handler.postDelayed(this, 1000)
            } else {
                onCountdownExpired()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            // Audio stream handled
        }

        setupPowerButtonDetector()
        setupShakeDetector()
        setupBiometrics()
        setupListeners()
        viewModel.loadCalibration()

        // Start 10-second alert countdown sequence
        startAlertSequence()
    }

    private fun setupPowerButtonDetector() {
        powerButtonDetector = PowerButtonDetector(this) {
            runOnUiThread {
                triggerBiometricAuthentication()
            }
        }
    }

    private fun setupShakeDetector() {
        shakeDetector = ShakeDetector(this) {
            runOnUiThread {
                disarmSafetyAlarm("Disarmed via Horizontal Shake Gesture")
            }
        }
    }

    private fun setupBiometrics() {
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isAuthenticationInProgress = false
                disarmSafetyAlarm("Disarmed via Biometric / Device Credential Authentication")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isAuthenticationInProgress = false
                Toast.makeText(this@DisarmActivity, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                if (!isDisarmed) {
                    handler.postDelayed(countdownRunnable, 1000)
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                isAuthenticationInProgress = false
                Toast.makeText(this@DisarmActivity, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
                if (!isDisarmed) {
                    handler.postDelayed(countdownRunnable, 1000)
                }
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Disarm EchoSafe Safeguards")
            .setSubtitle("Authenticate using Fingerprint, Face ID, or PIN/Password to cancel alarm")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }

    private fun triggerBiometricAuthentication() {
        if (isDisarmed || isAuthenticationInProgress) return
        isAuthenticationInProgress = true
        handler.removeCallbacks(countdownRunnable) // Pause countdown while authenticating

        promptInfo?.let { info ->
            try {
                biometricPrompt?.authenticate(info)
            } catch (e: Exception) {
                isAuthenticationInProgress = false
                handler.postDelayed(countdownRunnable, 1000)
                Toast.makeText(this, "Biometric Prompt unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startAlertSequence() {
        countdownSeconds = 10
        binding.tvCountdownSeconds.text = "10"
        play3BingSequence()
        triggerHapticPulse()
        handler.postDelayed(countdownRunnable, 1000)
    }

    private fun play3BingSequence() {
        playBingTone()
        handler.postDelayed({ playBingTone() }, 1000)
        handler.postDelayed({ playBingTone() }, 2000)
    }

    private fun playBingTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        } catch (e: Exception) {
            // Audio cue handled
        }
    }

    private fun triggerHapticPulse() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                v.vibrate(180)
            }
        }
    }

    private fun setupListeners() {
        binding.btnBiometricAuthenticate.setOnClickListener {
            triggerBiometricAuthentication()
        }

        binding.btnSubmitSafeWord.setOnClickListener {
            val entered = binding.etSpokenSafeWord.text.toString().trim()
            if (viewModel.verifySafeWord(entered)) {
                disarmSafetyAlarm("Disarmed via Acoustic Safe-Word Verification")
            } else {
                Toast.makeText(this, "Safe-Word incorrect. Shake device, double power click, or authenticate.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnManualDisarm.setOnClickListener {
            triggerBiometricAuthentication()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_POWER) {
            powerButtonDetector?.registerPowerClick()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun disarmSafetyAlarm(reason: String) {
        if (isDisarmed) return
        isDisarmed = true
        isAuthenticationInProgress = false
        handler.removeCallbacks(countdownRunnable)
        powerButtonDetector?.stopListening()
        shakeDetector?.stopListening()

        Toast.makeText(this, "SAFEGUARDS SHUT OFF: $reason", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun onCountdownExpired() {
        if (isDisarmed) return
        isDisarmed = true
        isAuthenticationInProgress = false
        powerButtonDetector?.stopListening()
        shakeDetector?.stopListening()

        Toast.makeText(this, "COUNTDOWN EXPIRED: Multi-channel Emergency Dispatch Initiated!", Toast.LENGTH_LONG).show()

        val triggerType = intent.getStringExtra(EXTRA_TRIGGER_TYPE) ?: "PROLONGED_SCREAM"
        viewModel.executeEmergencyDispatch(triggerType, null)

        val intent = Intent(this, VaultActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        powerButtonDetector?.startListening()
        shakeDetector?.startListening()
    }

    override fun onPause() {
        super.onPause()
        powerButtonDetector?.stopListening()
        shakeDetector?.stopListening()
    }

    override fun onDestroy() {
        isDisarmed = true
        isAuthenticationInProgress = false
        handler.removeCallbacks(countdownRunnable)
        powerButtonDetector?.stopListening()
        shakeDetector?.stopListening()
        toneGenerator?.release()
        super.onDestroy()
    }
}
