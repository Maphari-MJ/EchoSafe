package com.echosafe.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_calibration_profile")
data class VoiceCalibrationProfileEntity(
    @PrimaryKey val id: Int = 1,
    val normalPitchBaseline: Double,
    val screamPitchThreshold: Double,
    val secretKeyphraseHash: String,
    val safeWordHash: String
)
