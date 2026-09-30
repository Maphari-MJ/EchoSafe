package com.echosafe.ui.calibration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.echosafe.data.model.VoiceCalibrationProfile
import com.echosafe.data.repository.CalibrationRepository
import com.echosafe.vault.CryptoHashUtils
import kotlinx.coroutines.launch

class CalibrationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalibrationRepository(application)

    private val _profile = MutableLiveData<VoiceCalibrationProfile>()
    val profile: LiveData<VoiceCalibrationProfile> = _profile

    fun loadProfile() {
        viewModelScope.launch {
            val p = repository.getProfile()
            _profile.value = p
        }
    }

    fun saveCalibration(
        normalRms: Double,
        screamThreshold: Double,
        secretKeyphrase: String,
        safeWord: String
    ) {
        viewModelScope.launch {
            val keyHash = CryptoHashUtils.sha256(secretKeyphrase)
            val safeHash = CryptoHashUtils.sha256(safeWord)

            val p = VoiceCalibrationProfile(
                baselineRMS = normalRms,
                distressPitchThreshold = screamThreshold,
                secretKeyphraseHash = keyHash,
                safeWordHash = safeHash
            )
            repository.saveProfile(p)
            _profile.value = p
        }
    }
}
