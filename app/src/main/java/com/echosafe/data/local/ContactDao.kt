package com.echosafe.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM emergency_contacts WHERE contactId = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM emergency_contacts ORDER BY contactName ASC")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun getCount(): Int

    @Query("DELETE FROM emergency_contacts")
    suspend fun clearAll()
}
