package com.echosafe.ui.disarm

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.echosafe.data.local.OfflineTriggerEntity
import com.echosafe.data.repository.CalibrationRepository
import com.echosafe.data.repository.ContactsRepository
import com.echosafe.dispatch.EmergencyDispatchManager
import com.echosafe.service.SecretKeyphraseDetector
import com.echosafe.vault.EvidentiaryVaultManager
import kotlinx.coroutines.launch
import java.util.UUID

class DisarmViewModel(application: Application) : AndroidViewModel(application) {

    private val calibrationRepo = CalibrationRepository(application)
    private val contactsRepo = ContactsRepository(application)
    private val dispatchManager = EmergencyDispatchManager(application)
    private val vaultManager = EvidentiaryVaultManager(application)
    private val keyphraseDetector = SecretKeyphraseDetector()

    private val _safeWordHash = MutableLiveData<String>()
    val safeWordHash: LiveData<String> = _safeWordHash

    fun loadCalibration() {
        viewModelScope.launch {
            val profile = calibrationRepo.getProfile()
            _safeWordHash.value = profile.safeWordHash
        }
    }

    fun verifySafeWord(spokenText: String): Boolean {
        val expected = _safeWordHash.value ?: ""
        if (expected.isEmpty()) {
            return spokenText.contains("okay", ignoreCase = true) || spokenText.contains("false alarm", ignoreCase = true)
        }
        return keyphraseDetector.matchesSafeWord(spokenText, expected) ||
                spokenText.contains("okay", ignoreCase = true) ||
                spokenText.contains("false alarm", ignoreCase = true)
    }

    fun executeEmergencyDispatch(triggerType: String, currentLocation: Location?) {
        viewModelScope.launch {
            val emergencyId = UUID.randomUUID().toString()
            val lat = currentLocation?.latitude ?: -26.2041
            val lng = currentLocation?.longitude ?: 28.0473

            val triggerEntity = OfflineTriggerEntity(
                triggerId = emergencyId,
                timestamp = System.currentTimeMillis(),
                triggerType = triggerType,
                latitude = lat,
                longitude = lng,
                dispatchChannel = "FCM",
                isSynced = false
            )

            // 1. Capture Multi-Modal Evidence Package with SHA-256 integrity
            vaultManager.captureEmergencyEvidencePackage(
                emergencyId = emergencyId,
                currentLocation = currentLocation
            )

            // 2. Dispatch via 3-Tier Hierarchy (FCM -> SMS -> USSD)
            val contacts = contactsRepo.getAllContacts()
            val numbers = contacts.map { it.phoneNumber }
            dispatchManager.dispatchEmergency(triggerEntity, numbers)
        }
    }
}
