package com.echosafe.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_triggers")
data class OfflineTriggerEntity(
    @PrimaryKey val triggerId: String,
    val timestamp: Long,
    val triggerType: String,
    val latitude: Double,
    val longitude: Double,
    val dispatchChannel: String, // "FCM", "SMS", "USSD"
    val isSynced: Boolean = false
)
