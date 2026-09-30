package com.echosafe.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.echosafe.data.model.EmergencyContact
import com.echosafe.data.repository.ContactsRepository
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactsRepository(application)

    private val _contacts = MutableLiveData<List<EmergencyContact>>()
    val contacts: LiveData<List<EmergencyContact>> = _contacts

    private val _hasMinContacts = MutableLiveData<Boolean>()
    val hasMinContacts: LiveData<Boolean> = _hasMinContacts

    fun loadContacts() {
        viewModelScope.launch {
            val list = repository.getAllContacts()
            _contacts.value = list
            _hasMinContacts.value = list.size >= 3
        }
    }

    fun addContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.saveContact(contact)
            loadContacts()
        }
    }

    fun deleteContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.deleteContact(contact.contactId)
            loadContacts()
        }
    }
}
