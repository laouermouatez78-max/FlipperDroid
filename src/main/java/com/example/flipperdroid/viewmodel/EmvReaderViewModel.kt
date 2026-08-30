package com.example.flipperdroid.viewmodel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flipperdroid.nfc.EmvCardData
import com.example.flipperdroid.nfc.EmvCardReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EmvReaderViewModel : ViewModel() {
    private val emvCardReader = EmvCardReader()

    private val _cardData = MutableStateFlow<EmvCardData?>(null)
    val cardData: StateFlow<EmvCardData?> = _cardData

    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun readCard(tag: Tag) {
        if (_isReading.value) return
        viewModelScope.launch {
            _isReading.value = true
            _error.value = null
            _cardData.value = null
            try {
                val data = emvCardReader.readCard(tag)
                if (data?.isValid() == true && data.privacyMode) {
                    _cardData.value = data
                } else {
                    _error.value = "No supported payment application metadata was identified. The card may be unsupported, moved too soon, or not expose one of the V5 known AIDs."
                }
            } catch (e: Exception) {
                _error.value = "EMV metadata read failed: ${e.message?.take(160) ?: e.javaClass.simpleName}"
            } finally {
                _isReading.value = false
            }
        }
    }

    fun clearData() {
        _cardData.value = null
        _error.value = null
    }
}
