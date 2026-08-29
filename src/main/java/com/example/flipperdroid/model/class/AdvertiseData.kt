package com.example.flipperdroid.model.`class`

import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import android.util.Log
import java.io.Serializable

class AdvertiseData : Serializable {
    private var _logTag = "AdvertiseData"

    var includeDeviceName = true
    var includeTxPower = true

    var manufacturerData = mutableListOf<ManufacturerSpecificData>()
    var services = mutableListOf<ServiceData>()

    fun validate(): Boolean {
        //@Todo: implement validation here
        return true
    }
    fun build(): AdvertiseData? {
        if (validate()) {
            val builder = AdvertiseData.Builder()
            builder.setIncludeDeviceName(includeDeviceName)
            services.forEach { service ->
                service.serviceUuid?.let { uuid ->
                    builder.addServiceUuid(ParcelUuid(uuid))
                    service.serviceData?.let { data ->
                        builder.addServiceData(ParcelUuid(uuid), data)
                    }
                }
            }
            builder.setIncludeTxPowerLevel(includeTxPower)
            manufacturerData.forEach {
                builder.addManufacturerData(it.manufacturerId, it.manufacturerSpecificData)
            }
            return builder.build()
        } else {
            Log.d(_logTag, "AdvertiseDataModel could not be built because its invalid")
        }
        return null
    }
} 