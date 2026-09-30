package com.echosafe.data.repository

import android.content.Context
import com.echosafe.data.local.ContactEntity
import com.echosafe.data.local.EchoSafeDatabase
import com.echosafe.data.model.EmergencyContact
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

        // Fetch from Firestore if available
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
