package com.echosafe.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Hotspot(
    val hotspotId: String = "",
    val reportedBy: String = "Anonymized User",
    val riskLevel: String = "HIGH", // HIGH (Red), MODERATE (Yellow)
    val description: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val createdAt: Timestamp = Timestamp.now()
)
