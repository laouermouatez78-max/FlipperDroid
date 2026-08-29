package com.example.flipperdroid.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * V3 EMV lab simulator. It never registers a HostApduService and never presents real
 * payment credentials. The module models a small APDU transcript using synthetic data.
 */
class EmvCardEmulationViewModel : ViewModel() {

    private val _isEmulationActive = MutableStateFlow(false)
    val isEmulationActive: StateFlow<Boolean> = _isEmulationActive

    private val _selectedCardType = MutableStateFlow("Synthetic Visa-like test profile")
    val selectedCardType: StateFlow<String> = _selectedCardType

    private val _transcript = MutableStateFlow<List<String>>(emptyList())
    val transcript: StateFlow<List<String>> = _transcript

    val profiles = listOf(
        "Synthetic Visa-like test profile",
        "Synthetic Mastercard-like test profile",
        "Generic ISO-DEP test profile"
    )

    fun selectProfile(profile: String) {
        if (profile in profiles) {
            _selectedCardType.value = profile
            append("Profile selected: $profile")
        }
    }

    fun setEmulationActive(active: Boolean) {
        _isEmulationActive.value = active
        if (active) {
            _transcript.value = emptyList()
            append("LAB session started")
            append("RX 00 A4 04 00 07 F0 01 02 03 04 05 06 00")
            append("TX 6F 0A 84 07 F0 01 02 03 04 05 06 90 00")
            append("RX 80 A8 00 00 02 83 00 00")
            append("TX 77 06 82 02 00 00 94 00 90 00")
            append("Synthetic APDU exchange complete")
        } else {
            append("LAB session stopped")
        }
    }

    fun clearTranscript() {
        _transcript.value = emptyList()
    }

    private fun append(line: String) {
        _transcript.value = (_transcript.value + line).takeLast(100)
    }
}
