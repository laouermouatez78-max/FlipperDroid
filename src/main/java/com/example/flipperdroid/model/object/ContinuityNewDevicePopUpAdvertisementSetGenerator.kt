package com.example.flipperdroid.model.`object`

import com.example.flipperdroid.model.`class`.AdvertisementSet
import com.example.flipperdroid.model.`class`.ManufacturerSpecificData
import com.example.flipperdroid.model.enums.AdvertisementSetType
import com.example.flipperdroid.model.enums.AdvertisementTarget
import com.example.flipperdroid.model.enums.AdvertisementSetRange
import com.example.flipperdroid.model.enums.TxPowerLevel
import com.example.flipperdroid.model.enums.AdvertiseMode
import kotlin.random.Random

@OptIn(ExperimentalStdlibApi::class)
object ContinuityNewDevicePopUpAdvertisementSetGenerator {
    private val manufacturerId = 76 // 0x004c == 76 = Apple
    val deviceData = mapOf(
        "0E20" to "AirPods Pro",
        "0A20" to "AirPods Max",
        "0220" to "AirPods",
        "0F20" to "AirPods 2nd Gen",
        "1320" to "AirPods 3rd Gen",
        "1420" to "AirPods Pro 2nd Gen",
        "1020" to "Beats Flex",
        "0620" to "Beats Solo 3",
        "0320" to "Powerbeats 3",
        "0B20" to "Powerbeats Pro",
        "0C20" to "Beats Solo Pro",
        "1120" to "Beats Studio Buds",
        "0520" to "Beats X",
        "0920" to "Beats Studio 3",
        "1720" to "Beats Studio Pro",
        "1220" to "Beats Fit Pro",
        "1620" to "Beats Studio Buds+",
        // Ajout d'autres modèles Apple/Beats si présents dans Bluetooth-LE-Spam
    )

    fun getRandomBudsBatteryLevel(): String {
        val level = ((0..9).random() shl 4) + (0..9).random()
        return StringHelpers.intToHexString(level)
    }
    fun getRandomChargingCaseBatteryLevel(): String {
        val level = ((Random.nextInt(8) % 8) shl 4) + (Random.nextInt(10) % 10)
        return StringHelpers.intToHexString(level)
    }
    fun getRandomLidOpenCounter(): String {
        val counter = Random.nextInt(256)
        return StringHelpers.intToHexString(counter)
    }

    fun prepareAdvertisementSet(advertisementSet: AdvertisementSet): AdvertisementSet {
        if (advertisementSet.advertiseData.manufacturerData.size > 0) {
            val payload = advertisementSet.advertiseData.manufacturerData[0].manufacturerSpecificData
            payload[6] = StringHelpers.decodeHex(getRandomBudsBatteryLevel())[0]
            payload[7] = StringHelpers.decodeHex(getRandomChargingCaseBatteryLevel())[0]
            payload[8] = StringHelpers.decodeHex(getRandomLidOpenCounter())[0]
            for (i in 11..26) {
                payload[i] = Random.nextBytes(1)[0]
            }
        }
        return advertisementSet
    }

    fun getAdvertisementSets(): List<AdvertisementSet> {
        val advertisementSets = mutableListOf<AdvertisementSet>()
        deviceData.forEach { (key, value) ->
            val prefix = "07" // NEW DEVICE
            val color = "00"
            val advertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_IOS
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE
            advertisementSet.range = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_CLOSE
            advertisementSet.advertiseSettings.advertiseMode = AdvertiseMode.ADVERTISEMODE_LOW_LATENCY
            advertisementSet.advertiseSettings.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
            advertisementSet.advertiseSettings.connectable = false
            advertisementSet.advertiseSettings.timeout = 0
            advertisementSet.advertisingSetParameters.legacyMode = true
            advertisementSet.advertisingSetParameters.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
            advertisementSet.advertisingSetParameters.scanable = true
            advertisementSet.advertisingSetParameters.connectable = false
            advertisementSet.advertiseData.includeDeviceName = false
            val manufacturerSpecificData = ManufacturerSpecificData()
            manufacturerSpecificData.manufacturerId = manufacturerId
            val continuityType = "07"
            val payloadSize = "19"
            val status = "55"
            var payload = continuityType + payloadSize + prefix + key + status + getRandomBudsBatteryLevel() + getRandomChargingCaseBatteryLevel() + getRandomLidOpenCounter() + color + "00"
            payload += Random.nextBytes(16).toHexString()
            manufacturerSpecificData.manufacturerSpecificData = StringHelpers.decodeHex(payload)
            advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
            advertisementSet.advertiseData.includeTxPower = false
            advertisementSet.title = "New $value"
            advertisementSets.add(advertisementSet)
        }
        return advertisementSets
    }
} 