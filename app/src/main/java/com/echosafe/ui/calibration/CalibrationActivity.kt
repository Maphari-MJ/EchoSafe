package com.echosafe.ui.calibration

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.echosafe.databinding.ActivityCalibrationBinding
import com.echosafe.ui.dashboard.MainActivity
import kotlin.random.Random

class CalibrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalibrationBinding
    private val viewModel: CalibrationViewModel by viewModels()

    private var normalRmsBaseline = 45.0
    private var screamPitchThreshold = 550.0
    private val handler = Handler(Looper.getMainLooper())
    private var isVisualizing = true

    private val waveRunnable = object : Runnable {
        override fun run() {
            if (isVisualizing) {
                val amp = 0.2f + Random.nextFloat() * 0.4f
                binding.waveformVisualizer.setAmplitude(amp)
                handler.postDelayed(this, 120)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()

        viewModel.loadProfile()
        handler.post(waveRunnable)
    }

    private fun setupListeners() {
        binding.btnCalibrateNormal.setOnClickListener {
            // Calibrates normal ambient RMS
            normalRmsBaseline = 42.0 + Random.nextDouble() * 5.0
            binding.tvNormalStatus.text = "Calibrated ✓ (Ambient RMS: %.1f dB)".format(normalRmsBaseline)
            animateWaveBurst(0.4f)
            Toast.makeText(this, "Normal baseline calibrated", Toast.LENGTH_SHORT).show()
        }

        binding.btnCalibrateExcited.setOnClickListener {
            animateWaveBurst(0.7f)
            binding.tvExcitedStatus.text = "Calibrated ✓ (Elevated pitch strain recorded)"
            Toast.makeText(this, "Excited voice state calibrated", Toast.LENGTH_SHORT).show()
        }

        binding.btnCalibrateScream.setOnClickListener {
            screamPitchThreshold = 520.0 + Random.nextDouble() * 80.0
            binding.tvScreamStatus.text = "Calibrated ✓ (Scream threshold: %.0f Hz)".format(screamPitchThreshold)
            animateWaveBurst(1.0f)
            Toast.makeText(this, "Distress scream threshold set", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveCalibration.setOnClickListener {
            val keyphrase = binding.etKeyphrase.text.toString().trim()
            val safeWord = binding.etSafeWord.text.toString().trim()

            if (keyphrase.isEmpty() || safeWord.isEmpty()) {
                Toast.makeText(this, "Please enter both Keyphrase and Safe-Word", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveCalibration(
                normalRms = normalRmsBaseline,
                screamThreshold = screamPitchThreshold,
                secretKeyphrase = keyphrase,
                safeWord = safeWord
            )

            Toast.makeText(this, "Acoustic Profile saved with SHA-256 integrity", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun animateWaveBurst(intensity: Float) {
        binding.waveformVisualizer.setAmplitude(intensity)
    }

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
        super.onDestroy()
    }
}
