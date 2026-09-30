package com.echosafe.data.repository

import android.content.Context
import com.echosafe.data.local.EchoSafeDatabase
import com.echosafe.data.local.VoiceCalibrationProfileEntity
import com.echosafe.data.model.VoiceCalibrationProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CalibrationRepository(private val context: Context) {

    private val db = EchoSafeDatabase.getInstance(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getProfile(): VoiceCalibrationProfile {
        val entity = db.calibrationDao().getProfile()
        if (entity != null) {
            return VoiceCalibrationProfile(
                baselineRMS = entity.normalPitchBaseline,
                distressPitchThreshold = entity.screamPitchThreshold,
                secretKeyphraseHash = entity.secretKeyphraseHash,
                safeWordHash = entity.safeWordHash,
                lastCalibratedAt = Timestamp.now()
            )
        }

        // Try fetching from Firestore
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                val doc = firestore.collection("users")
                    .document(userId)
                    .collection("calibration")
                    .document("profile")
                    .get()
                    .await()

                val profile = doc.toObject(VoiceCalibrationProfile::class.java)
                if (profile != null) {
                    db.calibrationDao().saveProfile(
                        VoiceCalibrationProfileEntity(
                            id = 1,
                            normalPitchBaseline = profile.baselineRMS,
                            screamPitchThreshold = profile.distressPitchThreshold,
                            secretKeyphraseHash = profile.secretKeyphraseHash,
                            safeWordHash = profile.safeWordHash
                        )
                    )
                    return profile
                }
            } catch (e: Exception) {
                // Fallback to default
            }
        }

        return VoiceCalibrationProfile(
            baselineRMS = 45.0,
            distressPitchThreshold = 550.0,
            secretKeyphraseHash = "",
            safeWordHash = ""
        )
    }

    suspend fun saveProfile(profile: VoiceCalibrationProfile) {
        val entity = VoiceCalibrationProfileEntity(
            id = 1,
            normalPitchBaseline = profile.baselineRMS,
            screamPitchThreshold = profile.distressPitchThreshold,
            secretKeyphraseHash = profile.secretKeyphraseHash,
            safeWordHash = profile.safeWordHash
        )
        db.calibrationDao().saveProfile(entity)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("calibration")
                    .document("profile")
                    .set(profile)
                    .await()
            } catch (e: Exception) {
                // Handled locally
            }
        }
    }
}
