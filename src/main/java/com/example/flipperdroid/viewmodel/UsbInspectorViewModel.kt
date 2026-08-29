package com.example.flipperdroid.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
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

    private val _devices = MutableStateFlow<List<UsbDeviceInfo>>(emptyList())
    val devices: StateFlow<List<UsbDeviceInfo>> = _devices

    private val _status = MutableStateFlow("Connect a USB device with an OTG adapter, then refresh.")
    val status: StateFlow<String> = _status

    init { refresh() }

    fun refresh() {
        val list = manager.deviceList.values.map { device -> device.toInfo(manager.hasPermission(device)) }
            .sortedBy { it.deviceId }
        _devices.value = list
        _status.value = if (list.isEmpty()) {
            "No USB device detected. Use an OTG adapter/cable and connect your peripheral."
        } else {
            "${list.size} USB device(s) detected"
        }
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
            manufacturer = runCatching { manufacturerName }.getOrNull(),
            productName = runCatching { productName }.getOrNull(),
            serialNumber = if (hasPermission) runCatching { serialNumber }.getOrNull() else null,
            permissionGranted = hasPermission,
            interfaces = interfaceList
        )
    }
}
