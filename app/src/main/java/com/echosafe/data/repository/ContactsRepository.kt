package com.echosafe.data.repository

import android.content.Context
import android.util.Log
import com.echosafe.data.local.ContactEntity
import com.echosafe.data.local.EchoSafeDatabase
import com.echosafe.data.model.EmergencyContact
import com.echosafe.data.remote.BackendConfig
import com.echosafe.data.remote.dto.AddContactRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ContactsRepository(private val context: Context) {

    private val db = EchoSafeDatabase.getInstance(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getAllContacts(): List<EmergencyContact> {
        val local = db.contactDao().getAllContacts()
        if (local.isNotEmpty()) {
            return local.map {
                EmergencyContact(
                    contactId = it.contactId,
                    contactName = it.contactName,
                    phoneNumber = it.phoneNumber,
                    relationship = it.relationship,
                    fcmToken = it.fcmToken,
                    hasDndPermission = it.hasDndPermission
                )
            }
        }

        // 1. Try fetching from Node.js Express Backend via Retrofit (Ngrok)
        try {
            val response = BackendConfig.apiService.getContacts()
            if (response.isSuccessful && response.body()?.success == true) {
                val apiContacts = response.body()?.data ?: emptyList()
                val list = apiContacts.map {
                    EmergencyContact(
                        contactId = it.id,
                        contactName = it.contactName,
                        phoneNumber = it.phoneNumber,
                        relationship = it.relationship,
                        hasDndPermission = it.hasDndPermission
                    )
                }
                if (list.isNotEmpty()) {
                    db.contactDao().insertContacts(list.map {
                        ContactEntity(
                            contactId = it.contactId,
                            contactName = it.contactName,
                            phoneNumber = it.phoneNumber,
                            relationship = it.relationship,
                            hasDndPermission = it.hasDndPermission
                        )
                    })
                    return list
                }
            }
        } catch (e: Exception) {
            Log.w("ContactsRepository", "Express Backend getContacts call skipped/failed: ${e.message}")
        }

        // 2. Fetch from Firestore if available
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("contacts")
                    .get()
                    .await()

                val contacts = snapshot.toObjects(EmergencyContact::class.java)
                val entities = contacts.map {
                    ContactEntity(
                        contactId = it.contactId,
                        contactName = it.contactName,
                        phoneNumber = it.phoneNumber,
                        relationship = it.relationship,
                        fcmToken = it.fcmToken,
                        hasDndPermission = it.hasDndPermission
                    )
                }
                db.contactDao().insertContacts(entities)
                return contacts
            } catch (e: Exception) {
                // Fallback to local
            }
        }
        return emptyList()
    }

    suspend fun saveContact(contact: EmergencyContact) {
        val entity = ContactEntity(
            contactId = contact.contactId,
            contactName = contact.contactName,
            phoneNumber = contact.phoneNumber,
            relationship = contact.relationship,
            fcmToken = contact.fcmToken,
            hasDndPermission = contact.hasDndPermission
        )
        db.contactDao().insertContact(entity)

        // 1. Sync to Express Backend via Retrofit (Ngrok)
        try {
            val req = AddContactRequest(
                contactName = contact.contactName,
                phoneNumber = contact.phoneNumber,
                relationship = contact.relationship,
                hasDndPermission = contact.hasDndPermission
            )
            BackendConfig.apiService.addContact(req)
        } catch (e: Exception) {
            Log.w("ContactsRepository", "Express Backend addContact call skipped/failed: ${e.message}")
        }

        // 2. Sync to Firestore
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("contacts")
                    .document(contact.contactId)
                    .set(contact)
                    .await()
            } catch (e: Exception) {
                // Queued locally
            }
        }
    }

    suspend fun deleteContact(contactId: String) {
        db.contactDao().deleteById(contactId)

        // 1. Delete on Express Backend via Retrofit (Ngrok)
        try {
            BackendConfig.apiService.deleteContact(contactId)
        } catch (e: Exception) {
            Log.w("ContactsRepository", "Express Backend deleteContact call skipped/failed: ${e.message}")
        }

        // 2. Delete on Firestore
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("contacts")
                    .document(contactId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // Handled locally
            }
        }
    }

    suspend fun getContactCount(): Int {
        return db.contactDao().getCount()
    }

    suspend fun hasMinimumRequiredContacts(): Boolean {
        return getContactCount() >= 3
    }
}
