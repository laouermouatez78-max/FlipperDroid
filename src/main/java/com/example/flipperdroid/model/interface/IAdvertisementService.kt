package com.example.flipperdroid.model.`interface`

import com.example.flipperdroid.model.`class`.AdvertisementSet
import com.example.flipperdroid.model.enums.TxPowerLevel

interface IAdvertisementService {
    fun startAdvertisement(advertisementSet: AdvertisementSet)
    fun stopAdvertisement()
    fun setTxPowerLevel(txPowerLevel: TxPowerLevel)
    fun getTxPowerLevel(): TxPowerLevel
    fun addAdvertisementServiceCallback(callback: IAdvertisementServiceCallback)
    fun removeAdvertisementServiceCallback(callback: IAdvertisementServiceCallback)
    fun isLegacyService(): Boolean
} 