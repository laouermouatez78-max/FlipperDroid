package com.example.flipperdroid.model.`class`

import java.io.Serializable
import java.util.UUID

class ServiceData : Serializable {
    var serviceUuid: UUID? = null
    var serviceData: ByteArray? = null
} 