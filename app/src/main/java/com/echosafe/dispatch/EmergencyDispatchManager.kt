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
import com.echosafe.data.remote.BackendConfig
import com.echosafe.data.remote.dto.LocationDto
import com.echosafe.data.remote.dto.TriggerEmergencyRequest
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
     * Tier 1: Express Backend API (Ngrok) / Firebase Cloud Messaging (Wi-Fi / Mobile Data)
     * Tier 2: Native Cellular SMS (Standard Airtime / SMS Bundle)
     * Tier 3: Zero-Rated Telecom Gateways / USSD Payload Channels
     */
    fun dispatchEmergency(trigger: OfflineTriggerEntity, contactNumbers: List<String>) {
        scope.launch {
            // 1. Always record trigger to local SQLite queue
            db.triggerDao().insertTrigger(trigger)

            if (isNetworkAvailable()) {
                val success = sendCloudAlert(trigger)
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

    private suspend fun sendCloudAlert(trigger: OfflineTriggerEntity): Boolean {
        var remoteSuccess = false

        // 1. Dispatch to Node.js Express Backend via Retrofit & Ngrok
        try {
            val req = TriggerEmergencyRequest(
                triggerType = trigger.triggerType,
                currentLocation = LocationDto(trigger.latitude, trigger.longitude),
                locationAccuracy = 5.0f
            )
            val response = BackendConfig.apiService.triggerEmergency(req)
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Tier 1: Dispatched emergency to Node.js Express Backend via Ngrok: ${trigger.triggerId}")
                remoteSuccess = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 1: Node.js Express Backend call failed or offline: ${e.message}")
        }

        // 2. Dispatch to Firebase Cloud Firestore
        try {
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
            remoteSuccess = true
        } catch (e: Exception) {
            Log.e(TAG, "Tier 1: Firebase cloud alert failed: ${e.message}")
        }

        return remoteSuccess
    }

    private fun sendCellularSmsAlert(trigger: OfflineTriggerEntity, contacts: List<String>) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
                ?: @Suppress("DEPRECATION") SmsManager.getDefault()
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

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse(ussdString)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
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
