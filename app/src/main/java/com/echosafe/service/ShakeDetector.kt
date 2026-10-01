package com.echosafe.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Evaluates device accelerometer (Sensor.TYPE_ACCELEROMETER) for horizontal
 * left-to-right phone shaking to disarm safety alerts.
 */
class ShakeDetector(
    context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isListening = false
    private var lastShakeTime = 0L
    private var shakeCount = 0

    companion object {
        private const val TAG = "ShakeDetector"
        private const val SHAKE_THRESHOLD_GRAVITY = 2.2f // Accelerometer G-force threshold
        private const val SHAKE_SLOP_TIME_MS = 350L // Minimum time between shakes
        private const val SHAKE_TIMEOUT_MS = 1500L // Time window for 3 consecutive horizontal shakes
    }

    fun startListening() {
        if (isListening || accelerometer == null) return
        isListening = true
        shakeCount = 0
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        Log.d(TAG, "ShakeDetector accelerometer listener started.")
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        sensorManager.unregisterListener(this)
        Log.d(TAG, "ShakeDetector accelerometer listener stopped.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isListening) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            // Calculate overall G-force
            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            // Horizontal (Left-to-Right x-axis) dominance evaluation
            val isHorizontalShake = Math.abs(x) > 11.0f || gForce > SHAKE_THRESHOLD_GRAVITY

            if (isHorizontalShake) {
                val now = System.currentTimeMillis()
                // Ignore shake events that are too close together
                if (lastShakeTime + SHAKE_SLOP_TIME_MS > now) {
                    return
                }

                // Reset shake count if last shake was too long ago
                if (lastShakeTime + SHAKE_TIMEOUT_MS < now) {
                    shakeCount = 0
                }

                lastShakeTime = now
                shakeCount++
                Log.d(TAG, "Horizontal shake detected! Count: $shakeCount")

                if (shakeCount >= 2) { // 2 rapid horizontal shakes
                    shakeCount = 0
                    onShakeDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
