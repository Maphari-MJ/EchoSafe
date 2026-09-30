package com.echosafe.data.model

import com.google.firebase.Timestamp

data class VoiceCalibrationProfile(
    val baselineRMS: Double = 0.0,
    val distressPitchThreshold: Double = 350.0,
    val secretKeyphraseHash: String = "",
    val safeWordHash: String = "",
    val lastCalibratedAt: Timestamp = Timestamp.now()
)
