package com.echosafe.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class ContactEntity(
    @PrimaryKey val contactId: String,
    val contactName: String,
    val phoneNumber: String,
    val relationship: String,
    val fcmToken: String = "",
    val hasDndPermission: Boolean = true
)
