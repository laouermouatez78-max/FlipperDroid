package com.example.flipperdroid.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Legacy compatibility stub for FlipperDroid V3.
 *
 * The real V3 EMV Lab runs entirely inside Compose and does not register this service.
 * If somebody accidentally registers the legacy service later, it refuses every APDU
 * instead of impersonating a payment application.
 */
class EmvCardEmulationService : HostApduService() {

    private val unsupported = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray =
        unsupported.copyOf()

    override fun onDeactivated(reason: Int) = Unit
}
