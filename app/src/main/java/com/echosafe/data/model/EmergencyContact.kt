package com.echosafe.data.model

data class EmergencyContact(
    val contactId: String = "",
    val contactName: String = "",
    val phoneNumber: String = "",
    val relationship: String = "Friend", // Parent, Partner, Friend, etc.
    val fcmToken: String = "",
    val hasDndPermission: Boolean = true
)
