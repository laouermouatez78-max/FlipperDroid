package com.example.flipperdroid.model.`object`

import com.example.flipperdroid.model.`class`.AdvertisementSet
import com.example.flipperdroid.model.`class`.ManufacturerSpecificData
import com.example.flipperdroid.model.enums.AdvertisementSetType
import com.example.flipperdroid.model.enums.AdvertisementTarget
import com.example.flipperdroid.model.enums.AdvertisementSetRange
import com.example.flipperdroid.model.enums.TxPowerLevel
import com.example.flipperdroid.model.enums.AdvertiseMode

object EasySetupWatchAdvertisementSetGenerator {
    private const val manufacturerId = 117 // 0x75 == 117 = Samsung
    private val prependedBytesWatch = StringHelpers.decodeHex("010002000101FF000043")

    val genuineWatchIds = mapOf(
        "1A" to "Fallback Watch",
        "01" to "White Watch4 Classic 44m",
        "02" to "Black Watch4 Classic 40m",
        "03" to "White Watch4 Classic 40m",
        "04" to "Black Watch4 44mm",
        "05" to "Silver Watch4 44mm",
        "06" to "Green Watch4 44mm",
        "07" to "Black Watch4 40mm",
        "08" to "White Watch4 40mm",
        "09" to "Gold Watch4 40mm",
        "0A" to "French Watch4",
        "0B" to "French Watch4 Classic",
        "0C" to "Fox Watch5 44mm",
        "11" to "Black Watch5 44mm",
        "12" to "Sapphire Watch5 44mm",
        "13" to "Purpleish Watch5 40mm",
        "14" to "Gold Watch5 40mm",
        "15" to "Black Watch5 Pro 45mm",
        "16" to "Gray Watch5 Pro 45mm",
        "17" to "White Watch5 44mm",
        "18" to "White & Black Watch5",
        "1B" to "Black Watch6 Pink 40mm",
        "1C" to "Gold Watch6 Gold 40mm",
        "1D" to "Silver Watch6 Cyan 44mm",
        "1E" to "Black Watch6 Classic 43m",
        "20" to "Green Watch6 Classic 43m",
    )

    fun getAdvertisementSets(): List<AdvertisementSet> {
        val advertisementSets = mutableListOf<AdvertisementSet>()
        genuineWatchIds.forEach { (key, value) ->
            val advertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH
            advertisementSet.range = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_CLOSE
            advertisementSet.advertiseSettings.advertiseMode = AdvertiseMode.ADVERTISEMODE_LOW_LATENCY
            advertisementSet.advertiseSettings.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
            advertisementSet.advertiseSettings.connectable = false
            advertisementSet.advertiseSettings.timeout = 0
            advertisementSet.advertisingSetParameters.legacyMode = true
            advertisementSet.advertisingSetParameters.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
            advertisementSet.advertisingSetParameters.primaryPhy = null
            advertisementSet.advertisingSetParameters.secondaryPhy = null
            advertisementSet.advertiseData.includeDeviceName = false
            val manufacturerSpecificData = ManufacturerSpecificData()
            manufacturerSpecificData.manufacturerId = manufacturerId
            manufacturerSpecificData.manufacturerSpecificData = prependedBytesWatch.plus(StringHelpers.decodeHex(key))
            advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
            advertisementSet.advertiseData.includeTxPower = false
            advertisementSet.title = value
            advertisementSets.add(advertisementSet)
        }
        return advertisementSets
    }
} 