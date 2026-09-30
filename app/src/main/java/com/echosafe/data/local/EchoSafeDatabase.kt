package com.echosafe.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OfflineTriggerEntity::class,
        VoiceCalibrationProfileEntity::class,
        PendingEvidenceUploadEntity::class,
        ContactEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EchoSafeDatabase : RoomDatabase() {
    abstract fun triggerDao(): TriggerDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: EchoSafeDatabase? = null

        fun getInstance(context: Context): EchoSafeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EchoSafeDatabase::class.java,
                    "echosafe_database.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
