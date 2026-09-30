package com.echosafe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TriggerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrigger(trigger: OfflineTriggerEntity)

    @Query("SELECT * FROM offline_triggers WHERE isSynced = 0")
    suspend fun getUnsyncedTriggers(): List<OfflineTriggerEntity>

    @Query("UPDATE offline_triggers SET isSynced = 1 WHERE triggerId = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT * FROM offline_triggers ORDER BY timestamp DESC")
    suspend fun getAllTriggers(): List<OfflineTriggerEntity>
}
