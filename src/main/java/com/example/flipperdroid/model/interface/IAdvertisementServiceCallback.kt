package com.example.flipperdroid.model.`interface`

import com.example.flipperdroid.model.`class`.AdvertisementSet
import com.example.flipperdroid.model.enums.AdvertisementError

interface IAdvertisementServiceCallback {
    fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?)
    fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?)
    fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?)
    fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError)
} 