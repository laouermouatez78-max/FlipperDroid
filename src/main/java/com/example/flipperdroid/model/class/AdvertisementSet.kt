package com.example.flipperdroid.model.`class`

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertisingSetCallback
import android.util.Log
import java.io.Serializable
import com.example.flipperdroid.model.enums.AdvertisementTarget
import com.example.flipperdroid.model.enums.AdvertisementSetType
import com.example.flipperdroid.model.enums.AdvertisementSetRange

// Enum et classes associées à adapter/ajouter selon besoin
// import com.example.flipperdroid.model.AdvertisementSetType
// import com.example.flipperdroid.model.AdvertisementSetRange
// import com.example.flipperdroid.model.AdvertisementTarget
// ...

class AdvertisementSet : Serializable {
    private val _logTag = "AdvertisementSet"

    // Data
    var id = 0
    var title = ""
    var target: AdvertisementTarget = AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED
    var type: AdvertisementSetType = AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED
    var range: AdvertisementSetRange = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_UNKNOWN

    // Related Data
    var advertiseSettings: AdvertiseSettings = AdvertiseSettings()
    var advertisingSetParameters: AdvertisingSetParameters = AdvertisingSetParameters()
    var advertiseData: AdvertiseData = AdvertiseData()
    var scanResponse: AdvertiseData? = null

    // Callbacks
    lateinit var advertisingSetCallback: AdvertisingSetCallback
    lateinit var advertisingCallback: AdvertiseCallback


    fun validate(): Boolean {
        //@todo: implement checks here
        return true
    }

    fun build() {
        if (validate()) {
            // Build logic if needed
        } else {
            Log.d(_logTag, "Advertisement set could not be built because it is invalid")
        }
    }
} 