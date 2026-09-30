package com.echosafe.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Emergency(
    val emergencyId: String = "",
    val victimUserId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = "ACTIVE", // ACTIVE, RESOLVED, CANCELLED
    val triggerType: String = "PROLONGED_SCREAM", // PROLONGED_SCREAM, SECRET_KEYPHRASE, HARDWARE_DOUBLE_TAP, MANUAL_SOS
    val currentLocation: GeoPoint = GeoPoint(0.0, 0.0),
    val locationAccuracy: Double = 0.0
)
