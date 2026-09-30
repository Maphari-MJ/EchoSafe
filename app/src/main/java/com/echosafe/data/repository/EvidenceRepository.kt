package com.echosafe.data.repository

import android.content.Context
import com.echosafe.data.local.EchoSafeDatabase
import com.echosafe.data.model.EvidenceItem
import com.google.firebase.Timestamp
import java.util.Date

class EvidenceRepository(private val context: Context) {

    private val db = EchoSafeDatabase.getInstance(context)

    suspend fun getAllEvidenceItems(): List<EvidenceItem> {
        val list = db.evidenceDao().getAllEvidence()
        return list.map {
            EvidenceItem(
                evidenceId = it.evidenceId,
                mediaType = it.mediaType,
                storageUrl = "",
                sha256Hash = it.sha256Hash,
                capturedAt = Timestamp(Date(it.timestamp)),
                localFilePath = it.localFilePath,
                isUploaded = it.isUploaded
            )
        }
    }
}
