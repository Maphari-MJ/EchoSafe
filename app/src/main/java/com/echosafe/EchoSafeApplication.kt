package com.echosafe

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class EchoSafeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebaseSafely()
        createGlobalNotificationChannels()
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(this)
                } catch (_: Exception) {}

                if (FirebaseApp.getApps(this).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:100000000000:android:com.echosafe")
                        .setApiKey("AIzaSyEchoSafeFallbackKey0000000000")
                        .setProjectId("echosafe-app")
                        .setGcmSenderId("100000000000")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createGlobalNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dndChannel = NotificationChannel(
                "emergency_dnd_override",
                "Emergency High-Priority Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for emergency safety dispatch"
                enableVibration(true)
            }

            val guardChannel = NotificationChannel(
                "echosafe_acoustic_guard",
                "EchoSafe Acoustic Guard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous Acoustic Engine status"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(dndChannel)
            manager?.createNotificationChannel(guardChannel)
        }
    }
}
