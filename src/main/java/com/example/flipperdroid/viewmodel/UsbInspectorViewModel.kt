package com.example.flipperdroid.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
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
    private val actionUsbPermission = "${app.packageName}.USB_PERMISSION_V5"
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
                .sortedWith(compareBy<UsbDeviceInfo> { it.vendorId }.thenBy { it.productId }.thenBy { it.deviceId })
        }.getOrElse {
            _status.value = "USB refresh error: ${safeMessage(it)}"
            emptyList()
        }

        _devices.value = list
        _status.value = when {
            list.isEmpty() -> "No USB device detected. Connect a peripheral through USB OTG, then refresh."
            list.none { it.permissionGranted } -> "${list.size} USB device(s) detected · grant access to inspect protected strings and test opening"
            else -> "${list.size} USB device(s) detected · ${list.count { it.permissionGranted }} accessible"
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
            .onFailure { _status.value = "Could not request USB permission: ${safeMessage(it)}" }
    }

    /** Real non-destructive USB Host test: open the device and immediately close it. */
    fun testOpenConnection(deviceId: Int) {
        val device = manager.deviceList.values.firstOrNull { it.deviceId == deviceId } ?: run {
            _status.value = "USB device is no longer connected"
            refresh()
            return
        }
        if (!manager.hasPermission(device)) {
            _status.value = "Grant USB permission before running the connection test"
            return
        }

        val started = System.currentTimeMillis()
        val connection = runCatching { manager.openDevice(device) }.getOrNull()
        if (connection == null) {
            _status.value = "Android could not open the USB device"
            return
        }

        try {
            val elapsed = System.currentTimeMillis() - started
            _status.value = "USB OPEN/CLOSE TEST PASSED · %04X:%04X · ${device.interfaceCount} interface(s) · ${elapsed} ms".format(device.vendorId, device.productId)
        } finally {
            runCatching { connection.close() }
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

    companion object {
        fun classLabel(value: Int): String = when (value) {
            UsbConstants.USB_CLASS_AUDIO -> "Audio"
            UsbConstants.USB_CLASS_COMM -> "Communications"
            UsbConstants.USB_CLASS_HID -> "HID"
            UsbConstants.USB_CLASS_PHYSICA -> "Physical"
            UsbConstants.USB_CLASS_STILL_IMAGE -> "Imaging"
            UsbConstants.USB_CLASS_PRINTER -> "Printer"
            UsbConstants.USB_CLASS_MASS_STORAGE -> "Mass storage"
            UsbConstants.USB_CLASS_HUB -> "Hub"
            UsbConstants.USB_CLASS_CDC_DATA -> "CDC data"
            UsbConstants.USB_CLASS_CSCID -> "Smart card"
            UsbConstants.USB_CLASS_CONTENT_SEC -> "Content security"
            UsbConstants.USB_CLASS_VIDEO -> "Video"
            UsbConstants.USB_CLASS_WIRELESS_CONTROLLER -> "Wireless controller"
            UsbConstants.USB_CLASS_MISC -> "Miscellaneous"
            UsbConstants.USB_CLASS_APP_SPEC -> "Application specific"
            UsbConstants.USB_CLASS_VENDOR_SPEC -> "Vendor specific"
            UsbConstants.USB_CLASS_PER_INTERFACE -> "Per-interface"
            else -> "Class $value"
        }

        fun endpointDirectionLabel(direction: Int): String = when (direction) {
            UsbConstants.USB_DIR_IN -> "IN"
            UsbConstants.USB_DIR_OUT -> "OUT"
            else -> "?"
        }

        fun endpointTypeLabel(type: Int): String = when (type) {
            UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "control"
            UsbConstants.USB_ENDPOINT_XFER_ISOC -> "isochronous"
            UsbConstants.USB_ENDPOINT_XFER_BULK -> "bulk"
            UsbConstants.USB_ENDPOINT_XFER_INT -> "interrupt"
            else -> "type $type"
        }

        fun maskSerial(serial: String?): String? {
            val clean = serial?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return if (clean.length <= 4) "••••" else "••••••${clean.takeLast(4)}"
        }
    }

    private fun safeMessage(error: Throwable): String = error.message?.take(160) ?: error.javaClass.simpleName
}
