package com.example.flipperdroid.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * V5 USB/HID training lab.
 *
 * HID-style scripts are parsed and replayed only into an on-screen preview log.
 * No keyboard interface is claimed and no keystrokes are injected into another computer.
 */
class BadUsbViewModel : ViewModel() {

    private val _isUsbHostAvailable = mutableStateOf(false)
    val isUsbHostAvailable: State<Boolean> = _isUsbHostAvailable

    private val _isConnected = mutableStateOf(false)
    val isConnected: State<Boolean> = _isConnected

    private val _currentScript = MutableStateFlow("")
    val currentScript: StateFlow<String> = _currentScript

    private val _status = MutableStateFlow("USB HID Lab not initialized")
    val status: StateFlow<String> = _status

    private val _previewLog = MutableStateFlow<List<String>>(emptyList())
    val previewLog: StateFlow<List<String>> = _previewLog

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating

    private var simulationJob: Job? = null

    fun initialize(context: Context) {
        _isUsbHostAvailable.value = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        _status.value = if (_isUsbHostAvailable.value) {
            "USB Host detected · local HID parser ready"
        } else {
            "USB Host is not supported on this phone · local parser still available"
        }
    }

    /** Retained for compatibility with USB inspection flows; no interface or endpoint is claimed. */
    fun connectUsb(device: UsbDevice, connection: UsbDeviceConnection) {
        connection.close()
        _isConnected.value = true
        _status.value = "USB device %04X:%04X observed · safe local lab session".format(device.vendorId, device.productId)
        appendLog("USB device observed; HID injection remains disabled in V5")
    }

    fun startLabSession() {
        _isConnected.value = true
        _status.value = "USB HID local simulation session active"
        appendLog("V5 LAB session started")
    }

    fun disconnect() {
        stopSimulation()
        _isConnected.value = false
        _status.value = "USB HID lab session stopped"
        appendLog("V5 LAB session stopped")
    }

    fun updateScript(script: String) {
        if (_isSimulating.value) return
        _currentScript.value = script.take(20_000)
    }

    fun executeScript() {
        val script = _currentScript.value
        if (script.isBlank()) {
            _status.value = "Enter a script to validate"
            return
        }
        if (_isSimulating.value) {
            _status.value = "A local script preview is already running"
            return
        }
        if (!_isConnected.value) startLabSession()

        val lines = script.lines().filter { it.isNotBlank() }.take(200)
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            _isSimulating.value = true
            _status.value = "Previewing ${if (lines.isEmpty()) 1 else lines.size} local step(s)…"
            _previewLog.value = emptyList()
            try {
                if (lines.isEmpty()) {
                    appendLog("TEXT ${sanitize(script)}")
                } else {
                    lines.forEachIndexed { index, line ->
                        appendLog("STEP ${index + 1}/${lines.size}: ${sanitize(line)}")
                        delay(35)
                    }
                }
                _status.value = "Preview complete · no USB keystrokes were sent"
            } finally {
                _isSimulating.value = false
                simulationJob = null
            }
        }
    }

    fun stopSimulation() {
        if (!_isSimulating.value && simulationJob == null) return
        simulationJob?.cancel()
        simulationJob = null
        _isSimulating.value = false
        _status.value = "Local script preview stopped · no USB keystrokes were sent"
        appendLog("Preview stopped")
    }

    fun clearPreview() {
        _previewLog.value = emptyList()
        _status.value = "Preview log cleared"
    }

    private fun sanitize(value: String): String =
        value.replace("\t", "⇥").replace("\r", "").take(180)

    private fun appendLog(message: String) {
        _previewLog.value = (_previewLog.value + message).takeLast(160)
    }

    override fun onCleared() {
        simulationJob?.cancel()
        super.onCleared()
    }
}
