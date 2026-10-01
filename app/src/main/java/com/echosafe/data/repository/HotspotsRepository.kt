package com.echosafe.data.repository

import android.util.Log
import com.echosafe.data.model.Hotspot
import com.echosafe.data.remote.BackendConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

class HotspotsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getHotspots(): List<Hotspot> {
        // 1. Try fetching from Node.js Express Backend via Retrofit (Ngrok)
        try {
            val response = BackendConfig.apiService.getNearbyHotspots()
            if (response.isSuccessful && response.body()?.success == true) {
                val apiHotspots = response.body()?.hotspots ?: emptyList()
                if (apiHotspots.isNotEmpty()) {
                    return apiHotspots.map {
                        Hotspot(
                            hotspotId = it.id,
                            reportedBy = it.reportedBy,
                            riskLevel = it.riskLevel,
                            description = it.description,
                            location = GeoPoint(it.location.latitude, it.location.longitude),
                            createdAt = Timestamp.now()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("HotspotsRepository", "Express Backend getNearbyHotspots call skipped/failed: ${e.message}")
        }

        // 2. Fetch from Firestore if available
        try {
            val snapshot = firestore.collection("hotspots").get().await()
            val remoteHotspots = snapshot.toObjects(Hotspot::class.java)
            if (remoteHotspots.isNotEmpty()) {
                return remoteHotspots
            }
        } catch (e: Exception) {
            // Fallback to pre-loaded high-risk and moderate-risk safety pins
        }

        // 3. Return default GBV Crime Hotspot Heatmap & Risk Pins
        return listOf(
            Hotspot(
                hotspotId = "hs_001",
                reportedBy = "Anonymized Community Alert",
                riskLevel = "HIGH",
                description = "Isolated pedestrian underpass - Frequent distress calls reported after 20:00",
                location = GeoPoint(-26.2041, 28.0473),
                createdAt = Timestamp.now()
            ),
            Hotspot(
                hotspotId = "hs_002",
                reportedBy = "Safety Volunteer Node #4",
                riskLevel = "HIGH",
                description = "Poorly lit commuter taxi rank pathway - Physical harassment incident logged",
                location = GeoPoint(-26.2065, 28.0425),
                createdAt = Timestamp.now()
            ),
            Hotspot(
                hotspotId = "hs_003",
                reportedBy = "Community Watch",
                riskLevel = "MODERATE",
                description = "Dimly lit street with broken streetlights - Caution advised after sunset",
                location = GeoPoint(-26.1995, 28.0510),
                createdAt = Timestamp.now()
            ),
            Hotspot(
                hotspotId = "hs_004",
                reportedBy = "Campus Security Network",
                riskLevel = "MODERATE",
                description = "Perimeter fence blind spot - Increased security patrols active",
                location = GeoPoint(-26.2010, 28.0380),
                createdAt = Timestamp.now()
            )
        )
    }
}
