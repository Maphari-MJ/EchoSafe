package com.echosafe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EvidenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: PendingEvidenceUploadEntity)

    @Query("SELECT * FROM pending_evidence_uploads WHERE isUploaded = 0")
    suspend fun getPendingUploads(): List<PendingEvidenceUploadEntity>

    @Query("UPDATE pending_evidence_uploads SET isUploaded = 1 WHERE evidenceId = :id")
    suspend fun markUploaded(id: String)

    @Query("SELECT * FROM pending_evidence_uploads ORDER BY timestamp DESC")
    suspend fun getAllEvidence(): List<PendingEvidenceUploadEntity>
}
