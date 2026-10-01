package com.echosafe.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat

class PowerButtonDetector(
    private val context: Context,
    private val onPowerButtonDoubleClicked: () -> Unit
) {

    private var lastPowerPressTime: Long = 0
    private var isListening = false

    companion object {
        private const val DOUBLE_CLICK_TIME_DELTA = 1500L // 1.5 seconds window for double click
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_SCREEN_OFF || action == Intent.ACTION_SCREEN_ON || action == Intent.ACTION_USER_PRESENT) {
                registerPowerClick()
            }
        }
    }

    fun registerPowerClick() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPowerPressTime <= DOUBLE_CLICK_TIME_DELTA) {
            lastPowerPressTime = 0
            onPowerButtonDoubleClicked()
        } else {
            lastPowerPressTime = currentTime
        }
    }

    fun startListening() {
        if (isListening) return
        isListening = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(context, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        try {
            context.unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
            // Unregister safety
        }
    }
}
