package com.example.flipperdroid.model.`object`

import com.example.flipperdroid.model.`class`.AdvertisementSet
import com.example.flipperdroid.model.`class`.ManufacturerSpecificData
import com.example.flipperdroid.model.enums.AdvertisementSetType
import com.example.flipperdroid.model.enums.AdvertisementTarget
import com.example.flipperdroid.model.enums.AdvertisementSetRange
import com.example.flipperdroid.model.enums.TxPowerLevel
import com.example.flipperdroid.model.enums.AdvertiseMode
import com.example.flipperdroid.model.`class`.AdvertiseData

object EasySetupBudsAdvertisementSetGenerator {
    private const val manufacturerId = 117 // 0x75 == 117 = Samsung
    private val prependedBudsBytes = StringHelpers.decodeHex("42098102141503210109")
    private val appendedBudsBytes = StringHelpers.decodeHex("063C948E00000000C700")

    val genuineBudsIds = mapOf(
        "EE7A0C" to "Fallback Buds",
        "9D1700" to "Fallback Dots",
        "39EA48" to "Light Purple Buds2",
        "A7C62C" to "Bluish Silver Buds2",
        "850116" to "Black Buds Live",
        "3D8F41" to "Gray & Black Buds2",
        "3B6D02" to "Bluish Chrome Buds2",
        "AE063C" to "Gray Beige Buds2",
        "B8B905" to "Pure White Buds",
        "EAAA17" to "Pure White Buds2",
        "D30704" to "Black Buds",
        "9DB006" to "French Flag Buds",
        "101F1A" to "Dark Purple Buds Live",
        "859608" to "Dark Blue Buds",
        "8E4503" to "Pink Buds",
        "2C6740" to "White & Black Buds2",
        "3F6718" to "Bronze Buds Live",
        "42C519" to "Red Buds Live",
        "AE073A" to "Black & White Buds2",
        "011716" to "Sleek Black Buds2",
        // Ajouter ici tout autre modèle Samsung Buds trouvé dans Bluetooth-LE-Spam
    )

    fun getAdvertisementSets(): List<AdvertisementSet> {
        val advertisementSets = mutableListOf<AdvertisementSet>()
        genuineBudsIds.forEach { (key, value) ->
            val advertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS
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
            val payload = StringHelpers.decodeHex(key.substring(0,4) + "01" + key.substring(4))
            val fullPayload = prependedBudsBytes.plus(payload).plus(appendedBudsBytes)
            manufacturerSpecificData.manufacturerSpecificData = fullPayload
            advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
            advertisementSet.advertiseData.includeTxPower = false
            // Scan Response
            advertisementSet.scanResponse = AdvertiseData()
            advertisementSet.scanResponse!!.includeDeviceName = false
            val scanResponseManufacturerSpecificData = ManufacturerSpecificData()
            scanResponseManufacturerSpecificData.manufacturerId = manufacturerId
            scanResponseManufacturerSpecificData.manufacturerSpecificData = StringHelpers.decodeHex("0000000000000000000000000000")
            advertisementSet.scanResponse!!.manufacturerData.add(scanResponseManufacturerSpecificData)
            advertisementSet.title = value
            advertisementSets.add(advertisementSet)
        }
        return advertisementSets
    }
} 