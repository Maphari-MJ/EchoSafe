package com.echosafe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CalibrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: VoiceCalibrationProfileEntity)

    @Query("SELECT * FROM voice_calibration_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): VoiceCalibrationProfileEntity?
}
