package com.echosafe.dispatch

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.echosafe.data.local.EchoSafeDatabase
import com.echosafe.vault.EvidentiaryVaultManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date

class ResiliencySyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = EchoSafeDatabase.getInstance(context)
    private val vaultManager = EvidentiaryVaultManager(context)

    companion object {
        private const val TAG = "ResiliencySyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting WorkManager Resiliency Sync for offline queue...")

        var allSuccessful = true

        try {
            // 1. Sync pending offline triggers
            val unsyncedTriggers = db.triggerDao().getUnsyncedTriggers()
            val firestore = FirebaseFirestore.getInstance()
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_user"

            for (trigger in unsyncedTriggers) {
                try {
                    val map = hashMapOf(
                        "emergencyId" to trigger.triggerId,
                        "victimUserId" to currentUserId,
                        "timestamp" to Timestamp(Date(trigger.timestamp)),
                        "status" to "ACTIVE",
                        "triggerType" to trigger.triggerType,
                        "currentLocation" to GeoPoint(trigger.latitude, trigger.longitude),
                        "locationAccuracy" to 5.0
                    )

                    firestore.collection("emergencies")
                        .document(trigger.triggerId)
                        .set(map)
                        .await()

                    db.triggerDao().markSynced(trigger.triggerId)
                    Log.d(TAG, "Synced offline trigger: ${trigger.triggerId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed syncing trigger ${trigger.triggerId}: ${e.message}")
                    allSuccessful = false
                }
            }

            // 2. Sync pending evidence files
            val pendingUploads = db.evidenceDao().getPendingUploads()
            for (evidence in pendingUploads) {
                try {
                    val file = File(evidence.localFilePath)
                    if (file.exists()) {
                        val uploaded = vaultManager.uploadEvidenceToFirebase(
                            emergencyId = evidence.evidenceId,
                            entity = evidence,
                            file = file
                        )
                        if (!uploaded) allSuccessful = false
                    } else {
                        // File not found, mark uploaded to avoid infinite retry
                        db.evidenceDao().markUploaded(evidence.evidenceId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed syncing evidence ${evidence.evidenceId}: ${e.message}")
                    allSuccessful = false
                }
            }

            return if (allSuccessful) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "ResiliencySyncWorker unexpected error", e)
            return Result.retry()
        }
    }
}
