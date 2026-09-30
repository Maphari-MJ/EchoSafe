package com.echosafe.ui.vault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.echosafe.data.model.EvidenceItem
import com.echosafe.data.repository.EvidenceRepository
import com.echosafe.vault.CryptoHashUtils
import com.echosafe.vault.EvidentiaryVaultManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EvidenceRepository(application)
    private val vaultManager = EvidentiaryVaultManager(application)

    private val _evidenceList = MutableLiveData<List<EvidenceItem>>()
    val evidenceList: LiveData<List<EvidenceItem>> = _evidenceList

    fun loadEvidence() {
        viewModelScope.launch {
            val list = repository.getAllEvidenceItems()
            _evidenceList.value = list
        }
    }

    fun captureNewEvidencePackage() {
        vaultManager.captureEmergencyEvidencePackage(
            emergencyId = UUID.randomUUID().toString(),
            currentLocation = null
        )
        loadEvidence()
    }

    fun syncNow() {
        vaultManager.scheduleResiliencySync()
        loadEvidence()
    }

    fun verifyEvidenceIntegrity(item: EvidenceItem): Boolean {
        if (item.localFilePath.isNotBlank()) {
            val file = File(item.localFilePath)
            if (file.exists()) {
                return CryptoHashUtils.verifyIntegrity(file, item.sha256Hash)
            }
        }
        return item.sha256Hash.isNotBlank()
    }
}
