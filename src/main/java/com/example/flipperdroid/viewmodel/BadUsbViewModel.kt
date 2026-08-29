package com.example.flipperdroid.viewmodel

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * V3 USB/HID lab.
 *
 * Legacy versions wrote HID keyboard reports to a USB endpoint. V3 keeps the same
 * authoring workflow but validates and simulates scripts locally. No keystrokes are
 * injected into a connected computer.
 */
class BadUsbViewModel : ViewModel() {

    private val _isUsbHostAvailable = mutableStateOf(false)
    val isUsbHostAvailable: State<Boolean> = _isUsbHostAvailable

    private val _isConnected = mutableStateOf(false)
    val isConnected: State<Boolean> = _isConnected

    private val _currentScript = MutableStateFlow("")
    val currentScript: StateFlow<String> = _currentScript

    private val _status = MutableStateFlow("USB Lab not initialized")
    val status: StateFlow<String> = _status

    private val _previewLog = MutableStateFlow<List<String>>(emptyList())
    val previewLog: StateFlow<List<String>> = _previewLog

    private var usbManager: UsbManager? = null

    fun initialize(context: Context) {
        usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        _isUsbHostAvailable.value = usbManager != null
        _status.value = if (_isUsbHostAvailable.value) {
            "USB Host available — simulation ready"
        } else {
            "USB Host not supported on this device"
        }
    }

    /** Retained for compatibility with legacy activity code; no endpoint is claimed. */
    fun connectUsb(device: UsbDevice, connection: UsbDeviceConnection) {
        connection.close()
        _isConnected.value = true
        _status.value = "Detected USB device ${device.vendorId}:${device.productId} — safe lab session"
        appendLog("USB device detected; HID injection disabled in V3")
    }

    fun startLabSession() {
        _isConnected.value = true
        _status.value = "USB HID simulation session active"
        appendLog("LAB session started")
    }

    fun disconnect() {
        _isConnected.value = false
        _status.value = "USB lab session stopped"
        appendLog("LAB session stopped")
    }

    fun updateScript(script: String) {
        _currentScript.value = script.take(20_000)
    }

    fun executeScript() {
        val script = _currentScript.value
        if (script.isBlank()) {
            _status.value = "Enter a script to simulate"
            return
        }
        if (!_isConnected.value) startLabSession()

        viewModelScope.launch {
            _status.value = "Simulating ${script.length} character(s)…"
            _previewLog.value = emptyList()
            val lines = script.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                appendLog("TYPE ${sanitize(script)}")
            } else {
                lines.take(200).forEachIndexed { index, line ->
                    appendLog("STEP ${index + 1}: ${sanitize(line)}")
                    delay(35)
                }
            }
            _status.value = "Simulation complete — no USB keystrokes were sent"
        }
    }

    private fun sanitize(value: String): String =
        value.replace("\t", "⇥").replace("\r", "").take(180)

    private fun appendLog(message: String) {
        _previewLog.value = (_previewLog.value + message).takeLast(160)
    }
}
