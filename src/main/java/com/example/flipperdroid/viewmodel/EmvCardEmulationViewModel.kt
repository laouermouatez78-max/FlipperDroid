package com.example.flipperdroid.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EmvCardEmulationViewModel : ViewModel() {
    private val _isEmulationActive = MutableStateFlow(false)
    val isEmulationActive: StateFlow<Boolean> = _isEmulationActive

    private val _selectedCardType = MutableStateFlow("Visa")
    val selectedCardType: StateFlow<String> = _selectedCardType

    fun setEmulationActive(active: Boolean) {
        _isEmulationActive.value = active
    }

} 