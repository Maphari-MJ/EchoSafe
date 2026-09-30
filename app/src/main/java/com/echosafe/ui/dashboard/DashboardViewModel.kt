package com.echosafe.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.echosafe.data.model.Hotspot
import com.echosafe.data.repository.HotspotsRepository
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val hotspotsRepository = HotspotsRepository()

    private val _hotspots = MutableLiveData<List<Hotspot>>()
    val hotspots: LiveData<List<Hotspot>> = _hotspots

    private val _isGuardActive = MutableLiveData<Boolean>(true)
    val isGuardActive: LiveData<Boolean> = _isGuardActive

    fun loadHotspots() {
        viewModelScope.launch {
            val list = hotspotsRepository.getHotspots()
            _hotspots.value = list
        }
    }

    fun setGuardActive(active: Boolean) {
        _isGuardActive.value = active
    }
}
