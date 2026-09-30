package com.echosafe.data.model

import com.google.firebase.Timestamp

data class EvidenceItem(
    val evidenceId: String = "",
    val mediaType: String = "AUDIO_CLIP", // AUDIO_CLIP, CAMERA_SNAPSHOT, GPS_BREADCRUMB
    val storageUrl: String = "",
    val sha256Hash: String = "",
    val capturedAt: Timestamp = Timestamp.now(),
    val localFilePath: String = "",
    val isUploaded: Boolean = false
)
