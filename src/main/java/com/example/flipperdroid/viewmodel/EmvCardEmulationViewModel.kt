package com.example.flipperdroid.viewmodel

import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flipperdroid.nfc.ApduLabState
import com.example.flipperdroid.nfc.FlipperDroidHceService
import com.example.flipperdroid.nfc.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EmvCardEmulationViewModel : ViewModel() {

    private val _isEmulationActive = MutableStateFlow(ApduLabState.enabled)
    val isEmulationActive: StateFlow<Boolean> = _isEmulationActive

    private val _selectedCardType = MutableStateFlow("FlipperDroid HCE test card")
    val selectedCardType: StateFlow<String> = _selectedCardType

    val transcript: StateFlow<List<String>> = ApduLabState.transcript

    private val _readerStatus = MutableStateFlow("Tap another FlipperDroid HCE phone, then run the APDU test")
    val readerStatus: StateFlow<String> = _readerStatus

    val profiles = listOf("FlipperDroid HCE test card")

    fun selectProfile(profile: String) {
        if (profile in profiles) _selectedCardType.value = profile
    }

    fun setEmulationActive(active: Boolean) {
        ApduLabState.enabled = active
        _isEmulationActive.value = active
        ApduLabState.append(if (active) "CARD HCE enabled · AID ${FlipperDroidHceService.AID_HEX}" else "CARD HCE disabled")
    }

    fun runReaderTest(tag: Tag?) {
        if (tag == null) {
            _readerStatus.value = "No NFC tag/device captured yet"
            return
        }
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            _readerStatus.value = "Detected NFC target is not ISO-DEP/HCE"
            ApduLabState.append("READER target is not ISO-DEP")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _readerStatus.value = "Running real APDU exchange…"
            try {
                isoDep.timeout = 3500
                isoDep.connect()
                ApduLabState.append("READER connected · maxTransceive=${isoDep.maxTransceiveLength}")

                val select = FlipperDroidHceService.SELECT_APDU
                ApduLabState.append("READER TX ${select.toHex()}")
                val selectResponse = isoDep.transceive(select)
                ApduLabState.append("READER RX ${selectResponse.toHex()}")
                require(selectResponse.endsWith9000()) { "SELECT failed" }

                val ping = FlipperDroidHceService.PING_APDU
                ApduLabState.append("READER TX ${ping.toHex()}")
                val pingResponse = isoDep.transceive(ping)
                ApduLabState.append("READER RX ${pingResponse.toHex()}")
                require(pingResponse.endsWith9000()) { "PING failed" }

                val echoData = "HELLO-V4".toByteArray(Charsets.US_ASCII)
                val echo = byteArrayOf(0x80.toByte(), 0x20, 0x00, 0x00, echoData.size.toByte()) + echoData
                ApduLabState.append("READER TX ${echo.toHex()}")
                val echoResponse = isoDep.transceive(echo)
                ApduLabState.append("READER RX ${echoResponse.toHex()}")
                require(echoResponse.endsWith9000()) { "ECHO failed" }

                val returned = echoResponse.copyOfRange(0, echoResponse.size - 2).toString(Charsets.US_ASCII)
                _readerStatus.value = if (returned == "HELLO-V4") {
                    "REAL APDU TEST PASSED · SELECT + PING + ECHO"
                } else {
                    "APDU link worked, but ECHO payload differed"
                }
            } catch (error: Exception) {
                _readerStatus.value = "APDU test failed: ${error.message ?: error.javaClass.simpleName}"
                ApduLabState.append("READER error · ${error.message ?: error.javaClass.simpleName}")
            } finally {
                runCatching { isoDep.close() }
            }
        }
    }

    fun clearTranscript() {
        ApduLabState.clear()
    }

    private fun ByteArray.endsWith9000(): Boolean =
        size >= 2 && this[size - 2] == 0x90.toByte() && this[size - 1] == 0x00.toByte()
}
