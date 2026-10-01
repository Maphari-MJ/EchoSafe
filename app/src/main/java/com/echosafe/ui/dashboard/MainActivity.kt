package com.echosafe.ui.dashboard

import android.Manifest
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.echosafe.R
import com.echosafe.databinding.ActivityMainBinding
import com.echosafe.service.AcousticDetectionService
import com.echosafe.ui.calibration.CalibrationActivity
import com.echosafe.ui.contacts.ContactsActivity
import com.echosafe.ui.disarm.DisarmActivity
import com.echosafe.ui.vault.VaultActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: DashboardViewModel by viewModels()

    private val handler = Handler(Looper.getMainLooper())
    private var holdStartTime = 0L
    private var isHolding = false

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            startGuardService()
        }
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isActive = intent?.getBooleanExtra(AcousticDetectionService.EXTRA_IS_ACTIVE, false) ?: false
            updateGuardUi(isActive)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNecessaryPermissions()
        setupListeners()
        setupBottomNavigation()
        observeViewModel()

        viewModel.loadHotspots()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(AcousticDetectionService.ACTION_STATUS_CHANGED)
        ContextCompat.registerReceiver(this, statusReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        updateGuardUi(AcousticDetectionService.isGuardActive)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            // Not registered
        }
    }

    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(missing.toTypedArray())
        } else {
            startGuardService()
        }
    }

    private fun startGuardService() {
        val intent = Intent(this, AcousticDetectionService::class.java).apply {
            action = AcousticDetectionService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
        viewModel.setGuardActive(true)
        updateGuardUi(true)
    }

    private fun stopGuardService() {
        val intent = Intent(this, AcousticDetectionService::class.java).apply {
            action = AcousticDetectionService.ACTION_STOP
        }
        startService(intent)
        viewModel.setGuardActive(false)
        updateGuardUi(false)
    }

    private fun updateGuardUi(isActive: Boolean) {
        if (isActive) {
            binding.bannerGuardStatus.setBackgroundResource(R.drawable.bg_banner_active)
            binding.viewIndicatorDot.setBackgroundColor(getColor(R.color.guard_active_green))
            binding.tvGuardState.text = "[Guard: ACTIVE ]"
            binding.tvGuardState.setTextColor(getColor(R.color.guard_active_green))
            binding.tvListeningStatus.text = getString(R.string.acoustic_engine_listening)
            binding.tvListeningStatus.setTextColor(getColor(R.color.secondary_teal))
        } else {
            binding.bannerGuardStatus.setBackgroundResource(R.drawable.bg_banner_inactive)
            binding.viewIndicatorDot.setBackgroundColor(getColor(R.color.warning_yellow))
            binding.tvGuardState.text = "[Guard: PAUSED ]"
            binding.tvGuardState.setTextColor(getColor(R.color.warning_yellow))
            binding.tvListeningStatus.text = getString(R.string.acoustic_engine_paused)
            binding.tvListeningStatus.setTextColor(getColor(R.color.text_secondary))
        }
    }

    private fun setupListeners() {
        // Toggle Guard on clicking header badge
        binding.bannerGuardStatus.setOnClickListener {
            if (AcousticDetectionService.isGuardActive) {
                stopGuardService()
                Toast.makeText(this, "Guard Paused", Toast.LENGTH_SHORT).show()
            } else {
                startGuardService()
                Toast.makeText(this, "Guard Activated", Toast.LENGTH_SHORT).show()
            }
        }

        // Hotspot selection from Map
        binding.hotspotMapView.onHotspotSelected = { hotspot ->
            AlertDialog.Builder(this)
                .setTitle("Hotspot Incident Details (${hotspot.riskLevel})")
                .setMessage("Reported By: ${hotspot.reportedBy}\n\nDescription: ${hotspot.description}")
                .setPositiveButton("Acknowledge", null)
                .show()
        }

        // Hold for Manual SOS touch logic (1.5 seconds hold)
        binding.btnSos.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isHolding = true
                    holdStartTime = System.currentTimeMillis()
                    handler.post(holdProgressRunnable)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isHolding = false
                    binding.pbSosHold.progress = 0
                    handler.removeCallbacks(holdProgressRunnable)
                    true
                }
                else -> false
            }
        }
    }

    private val holdProgressRunnable = object : Runnable {
        override fun run() {
            if (isHolding) {
                val elapsed = System.currentTimeMillis() - holdStartTime
                val progress = ((elapsed / 1500.0) * 100).toInt().coerceIn(0, 100)
                binding.pbSosHold.progress = progress

                if (progress >= 100) {
                    isHolding = false
                    binding.pbSosHold.progress = 0
                    triggerManualSos()
                } else {
                    handler.postDelayed(this, 30)
                }
            }
        }
    }

    private fun triggerManualSos() {
        Toast.makeText(this, "Manual SOS Triggered! Opening Safeguards...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, DisarmActivity::class.java).apply {
            putExtra(DisarmActivity.EXTRA_TRIGGER_TYPE, "MANUAL_SOS")
        }
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        binding.tabHome.setOnClickListener {
            // Already on home
        }
        binding.tabContacts.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }
        binding.tabCalibration.setOnClickListener {
            startActivity(Intent(this, CalibrationActivity::class.java))
        }
        binding.tabVault.setOnClickListener {
            startActivity(Intent(this, VaultActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.hotspots.observe(this) { list ->
            binding.hotspotMapView.setHotspots(list)
        }
    }
}
