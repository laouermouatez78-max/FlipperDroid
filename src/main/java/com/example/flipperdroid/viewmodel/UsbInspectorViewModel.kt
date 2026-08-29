package com.example.flipperdroid.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


data class UsbEndpointInfo(val address: Int, val direction: Int, val type: Int, val maxPacketSize: Int)
data class UsbInterfaceInfo(val id: Int, val interfaceClass: Int, val subclass: Int, val protocol: Int, val endpoints: List<UsbEndpointInfo>)
data class UsbDeviceInfo(
    val deviceId: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int,
    val manufacturer: String?,
    val productName: String?,
    val serialNumber: String?,
    val permissionGranted: Boolean,
    val interfaces: List<UsbInterfaceInfo>
)

class UsbInspectorViewModel(app: Application) : AndroidViewModel(app) {
    private val manager = app.getSystemService(Context.USB_SERVICE) as UsbManager
    private val actionUsbPermission = "${app.packageName}.USB_PERMISSION_V4"
    private var receiverRegistered = false

    private val _usbHostSupported = MutableStateFlow(app.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST))
    val usbHostSupported: StateFlow<Boolean> = _usbHostSupported

    private val _devices = MutableStateFlow<List<UsbDeviceInfo>>(emptyList())
    val devices: StateFlow<List<UsbDeviceInfo>> = _devices

    private val _status = MutableStateFlow("Connect a USB device with an OTG adapter, then refresh.")
    val status: StateFlow<String> = _status

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != actionUsbPermission) return
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            }
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            _status.value = when {
                device == null -> "USB permission response received"
                granted -> "USB permission granted for %04X:%04X".format(device.vendorId, device.productId)
                else -> "USB permission denied for %04X:%04X".format(device.vendorId, device.productId)
            }
            refresh()
        }
    }

    init {
        registerPermissionReceiver()
        refresh()
    }

    fun refresh() {
        _usbHostSupported.value = getApplication<Application>().packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        if (!_usbHostSupported.value) {
            _devices.value = emptyList()
            _status.value = "This phone does not report Android USB Host support."
            return
        }

        val list = runCatching {
            manager.deviceList.values.map { device -> device.toInfo(manager.hasPermission(device)) }
                .sortedBy { it.deviceId }
        }.getOrElse {
            _status.value = "USB refresh error: ${it.message?.take(160) ?: it.javaClass.simpleName}"
            emptyList()
        }

        _devices.value = list
        if (list.isEmpty()) {
            _status.value = "No USB device detected. Connect a peripheral through USB OTG, then refresh."
        } else if (list.none { it.permissionGranted }) {
            _status.value = "${list.size} USB device(s) detected · tap Grant access to inspect protected details"
        } else {
            _status.value = "${list.size} USB device(s) detected"
        }
    }

    fun requestPermission(deviceId: Int) {
        val app = getApplication<Application>()
        val device = manager.deviceList.values.firstOrNull { it.deviceId == deviceId } ?: run {
            _status.value = "USB device is no longer connected"
            refresh()
            return
        }
        if (manager.hasPermission(device)) {
            _status.value = "USB permission is already granted"
            refresh()
            return
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val intent = Intent(actionUsbPermission).setPackage(app.packageName)
        val pendingIntent = PendingIntent.getBroadcast(app, deviceId, intent, flags)
        runCatching { manager.requestPermission(device, pendingIntent) }
            .onSuccess { _status.value = "Waiting for Android USB permission…" }
            .onFailure { _status.value = "Could not request USB permission: ${it.message?.take(160) ?: it.javaClass.simpleName}" }
    }

    private fun UsbDevice.toInfo(hasPermission: Boolean): UsbDeviceInfo {
        val interfaceList = (0 until interfaceCount).map { index ->
            val intf = getInterface(index)
            UsbInterfaceInfo(
                id = intf.id,
                interfaceClass = intf.interfaceClass,
                subclass = intf.interfaceSubclass,
                protocol = intf.interfaceProtocol,
                endpoints = (0 until intf.endpointCount).map { epIndex ->
                    val ep = intf.getEndpoint(epIndex)
                    UsbEndpointInfo(ep.address, ep.direction, ep.type, ep.maxPacketSize)
                }
            )
        }

        return UsbDeviceInfo(
            deviceId = deviceId,
            name = deviceName,
            vendorId = vendorId,
            productId = productId,
            deviceClass = deviceClass,
            manufacturer = if (hasPermission) runCatching { manufacturerName }.getOrNull() else null,
            productName = if (hasPermission) runCatching { productName }.getOrNull() else null,
            serialNumber = if (hasPermission) runCatching { serialNumber }.getOrNull() else null,
            permissionGranted = hasPermission,
            interfaces = interfaceList
        )
    }

    private fun registerPermissionReceiver() {
        if (receiverRegistered) return
        val app = getApplication<Application>()
        val filter = IntentFilter(actionUsbPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            app.registerReceiver(permissionReceiver, filter)
        }
        receiverRegistered = true
    }

    override fun onCleared() {
        if (receiverRegistered) {
            runCatching { getApplication<Application>().unregisterReceiver(permissionReceiver) }
            receiverRegistered = false
        }
        super.onCleared()
    }
}
