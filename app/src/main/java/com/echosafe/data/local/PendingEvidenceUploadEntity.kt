package com.echosafe.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_evidence_uploads")
data class PendingEvidenceUploadEntity(
    @PrimaryKey val evidenceId: String,
    val localFilePath: String,
    val mediaType: String, // "AUDIO_CLIP", "CAMERA_SNAPSHOT", "GPS_BREADCRUMB"
    val sha256Hash: String,
    val timestamp: Long,
    val isUploaded: Boolean = false
)
