package com.example.flipperdroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flipperdroid.model.`class`.AdvertisementSet
import com.example.flipperdroid.model.`object`.ContinuityNewDevicePopUpAdvertisementSetGenerator
import com.example.flipperdroid.model.`object`.EasySetupBudsAdvertisementSetGenerator
import com.example.flipperdroid.model.`object`.EasySetupWatchAdvertisementSetGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * V4 BLE payload training controller.
 *
 * Legacy payload profiles are kept for inspection and training, but rotation is
 * simulated locally. No repeated BLE advertisements are transmitted.
 */
class BleSpamViewModel(app: Application) : AndroidViewModel(app) {

    enum class BleSpamBrand { APPLE, SAMSUNG, ALL }

    private val _advertisementSets = MutableStateFlow<List<AdvertisementSet>>(emptyList())
    val advertisementSets: StateFlow<List<AdvertisementSet>> = _advertisementSets

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val _brand = MutableStateFlow(BleSpamBrand.APPLE)
    val brand: StateFlow<BleSpamBrand> = _brand

    private val _spamLogs = MutableStateFlow<List<String>>(emptyList())
    val spamLogs: StateFlow<List<String>> = _spamLogs

    private var allAdvertisementSets: List<AdvertisementSet> = emptyList()
    private var selectedSets: List<AdvertisementSet> = emptyList()
    private var simulationJob: Job? = null

    init {
        loadAdvertisementSets()
        appendLog("V4 BLE Payload Lab ready · local simulation")
    }

    fun setBrand(brand: BleSpamBrand) {
        if (_isActive.value) return
        _brand.value = brand
        loadAdvertisementSets()
        appendLog("Profile changed to ${brand.name}")
    }

    fun startSimulation() {
        if (_isActive.value) return
        val queue = selectedSets.ifEmpty { _advertisementSets.value }
        if (queue.isEmpty()) {
            appendLog("No BLE lab payload selected")
            return
        }

        _isActive.value = true
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            appendLog("Local rotation started with ${queue.size} payload(s)")
            var index = 0
            while (_isActive.value) {
                val set = queue[index % queue.size]
                val title = if (set.title.isNotBlank()) set.title else set.type.toString()
                appendLog("PREVIEW #${index + 1}: $title (${set.type})")
                index++
                delay(650)
            }
        }
    }

    fun stopSimulation() {
        if (!_isActive.value && simulationJob == null) return
        _isActive.value = false
        simulationJob?.cancel()
        simulationJob = null
        appendLog("Local rotation stopped")
    }

    private fun loadAdvertisementSets() {
        val sets = when (_brand.value) {
            BleSpamBrand.APPLE -> ContinuityNewDevicePopUpAdvertisementSetGenerator.getAdvertisementSets()
            BleSpamBrand.SAMSUNG ->
                EasySetupWatchAdvertisementSetGenerator.getAdvertisementSets() +
                    EasySetupBudsAdvertisementSetGenerator.getAdvertisementSets()
            BleSpamBrand.ALL ->
                ContinuityNewDevicePopUpAdvertisementSetGenerator.getAdvertisementSets() +
                    EasySetupWatchAdvertisementSetGenerator.getAdvertisementSets() +
                    EasySetupBudsAdvertisementSetGenerator.getAdvertisementSets()
        }
        allAdvertisementSets = sets
        selectedSets = sets
        _advertisementSets.value = sets
    }

    fun setCheckedPayloads(checkedStates: List<Boolean>) {
        if (_isActive.value) return
        selectedSets = allAdvertisementSets.filterIndexed { index, _ ->
            checkedStates.getOrNull(index) == true
        }
        _advertisementSets.value = allAdvertisementSets
    }

    private fun appendLog(message: String) {
        val stamp = System.currentTimeMillis() % 100000
        _spamLogs.value = (_spamLogs.value + "[$stamp] $message").takeLast(120)
    }

    override fun onCleared() {
        stopSimulation()
        super.onCleared()
    }
}
