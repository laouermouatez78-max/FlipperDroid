package com.example.flipperdroid.services

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import com.example.flipperdroid.model.UsbDeviceInfo
import com.example.flipperdroid.model.UsbEndpointInfo

class UsbInspector(context: Context) {
    private val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    @SuppressLint("NewApi")
    fun devices(): List<UsbDeviceInfo> = manager.deviceList.values.map { device ->
        val endpoints = buildList {
            repeat(device.interfaceCount) { index ->
                val intf = device.getInterface(index)
                repeat(intf.endpointCount) { endpointIndex ->
                    val ep = intf.getEndpoint(endpointIndex)
                    add(
                        UsbEndpointInfo(
                            interfaceId = intf.id,
                            interfaceClass = intf.interfaceClass,
                            endpointAddress = ep.address,
                            direction = if (ep.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT",
                            type = when (ep.type) {
                                UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "Control"
                                UsbConstants.USB_ENDPOINT_XFER_ISOC -> "Isochronous"
                                UsbConstants.USB_ENDPOINT_XFER_BULK -> "Bulk"
                                UsbConstants.USB_ENDPOINT_XFER_INT -> "Interrupt"
                                else -> "${ep.type}"
                            },
                            maxPacketSize = ep.maxPacketSize
                        )
                    )
                }
            }
        }
        val (knownLabel, sdrCapable) = UsbDeviceCatalog.lookup(device.vendorId, device.productId)
        UsbDeviceInfo(
            deviceId = device.deviceId,
            name = device.deviceName,
            manufacturer = runCatching { device.manufacturerName }.getOrNull(),
            product = runCatching { device.productName }.getOrNull(),
            vendorId = device.vendorId,
            productId = device.productId,
            deviceClass = device.deviceClass,
            interfaceCount = device.interfaceCount,
            endpoints = endpoints,
            knownDeviceLabel = knownLabel,
            sdrCapable = sdrCapable
        )
    }.sortedBy { it.name }

    /** Convenience accessor used by the Sub-GHz screen to spot a recognized SDR/RF dongle. */
    fun findSdrDongle(): UsbDeviceInfo? = devices().firstOrNull { it.sdrCapable }
}
