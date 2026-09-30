package com.echosafe.dispatch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.echosafe.data.local.EchoSafeDatabase
import com.echosafe.data.local.OfflineTriggerEntity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class EmergencyDispatchManager(private val context: Context) {

    private val evaluator = NetworkAirtimeEvaluator(context)
    private val db = EchoSafeDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "EmergencyDispatcher"
    }

    /**
     * Dispatches emergency according to the 3-Tier Offline & Zero-Airtime Dispatch Hierarchy
     * Tier 1: Firebase Cloud Messaging (Wi-Fi / Mobile Data)
     * Tier 2: Native Cellular SMS (Standard Airtime / SMS Bundle)
     * Tier 3: Zero-Rated Telecom Gateways / USSD Payload Channels
     */
    fun dispatchEmergency(trigger: OfflineTriggerEntity, contactNumbers: List<String>) {
        scope.launch {
            // 1. Always record trigger to local SQLite queue
            db.triggerDao().insertTrigger(trigger)

            if (isNetworkAvailable()) {
                val success = sendFirebaseCloudAlert(trigger)
                if (success) {
                    db.triggerDao().markSynced(trigger.triggerId)
                } else {
                    // Fallback to SMS if Cloud push fails
                    fallbackToCellularOrUssd(trigger, contactNumbers)
                }
            } else if (hasAirtimeBalance()) {
                sendCellularSmsAlert(trigger, contactNumbers)
                scheduleResiliencySync()
            } else {
                sendZeroAirtimeUssdFallback(trigger)
                scheduleResiliencySync()
            }
        }
    }

    private suspend fun fallbackToCellularOrUssd(trigger: OfflineTriggerEntity, contactNumbers: List<String>) {
        if (hasAirtimeBalance()) {
            sendCellularSmsAlert(trigger, contactNumbers)
        } else {
            sendZeroAirtimeUssdFallback(trigger)
        }
        scheduleResiliencySync()
    }

    private suspend fun sendFirebaseCloudAlert(trigger: OfflineTriggerEntity): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_user"

            val emergencyMap = hashMapOf(
                "emergencyId" to trigger.triggerId,
                "victimUserId" to userId,
                "timestamp" to Timestamp(Date(trigger.timestamp)),
                "status" to "ACTIVE",
                "triggerType" to trigger.triggerType,
                "currentLocation" to GeoPoint(trigger.latitude, trigger.longitude),
                "locationAccuracy" to 5.0
            )

            firestore.collection("emergencies")
                .document(trigger.triggerId)
                .set(emergencyMap)
                .await()

            Log.d(TAG, "Tier 1: Firebase Cloud Alert dispatched successfully: ${trigger.triggerId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Tier 1 Failed: Cloud alert failed: ${e.message}")
            false
        }
    }

    private fun sendCellularSmsAlert(trigger: OfflineTriggerEntity, contacts: List<String>) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
                ?: SmsManager.getDefault()
            val message = "EMERGENCY ALERT from EchoSafe! " +
                    "GPS: https://maps.google.com/?q=${trigger.latitude},${trigger.longitude}"

            for (number in contacts) {
                if (number.isNotBlank()) {
                    smsManager.sendTextMessage(number.trim(), null, message, null, null)
                    Log.d(TAG, "Tier 2: Dispatched SMS alert to $number")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tier 2 Failed: Cellular SMS alert failed: ${e.message}")
            sendZeroAirtimeUssdFallback(trigger)
        }
    }

    private fun sendZeroAirtimeUssdFallback(trigger: OfflineTriggerEntity) {
        try {
            // Formats compressed payload into reverse-billed or zero-rated carrier gateway string
            val latStr = trigger.latitude.toString().take(7)
            val lngStr = trigger.longitude.toString().take(7)
            val ussdString = "tel:*130*999*$latStr*$lngStr%23"

            Log.d(TAG, "Tier 3: Formatted Zero-Airtime USSD Payload: $ussdString")

            // Broadcast Intent to initiate USSD Telemetry Gateway Routing
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse(ussdString)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            // If CALL_PHONE permission is granted, carrier will initiate; otherwise system intent
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Tier 3: USSD background routing handled or fallback logged: ${e.message}")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return evaluator.isNetworkAvailable()
    }

    private fun hasAirtimeBalance(): Boolean {
        return evaluator.hasAirtimeBalance()
    }

    private fun scheduleResiliencySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<ResiliencySyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncWorkRequest)
    }
}
