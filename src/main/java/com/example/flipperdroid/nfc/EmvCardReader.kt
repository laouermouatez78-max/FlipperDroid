package com.example.flipperdroid.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Privacy-first EMV metadata reader for FlipperDroid V5.
 *
 * V5 identifies a supported contactless payment application (AID/scheme) only.
 * It deliberately does not issue commands intended to retrieve PAN, expiry date,
 * cardholder name, Track 2 or transaction data.
 */
class EmvCardReader {

    companion object {
        private val KNOWN_AIDS = linkedMapOf(
            "A0000000031010" to "Visa",
            "A0000000041010" to "Mastercard",
            "A0000000651010" to "JCB",
            "A0000000042203" to "Maestro",
            "A0000000043060" to "Maestro UK"
        )

        private const val SELECT_PPSE = "2PAY.SYS.DDF01"
    }

    suspend fun readCard(tag: Tag): EmvCardData? = withContext(Dispatchers.IO) {
        val iso = IsoDep.get(tag) ?: return@withContext null
        try {
            if (!iso.isConnected) iso.connect()
            iso.timeout = 2_500

            val ppse = iso.transceive(buildSelectCommand(SELECT_PPSE.toByteArray(Charsets.US_ASCII)))
            if (!isSuccessful(ppse)) return@withContext null

            for ((aid, scheme) in KNOWN_AIDS) {
                val response = runCatching { iso.transceive(buildSelectCommand(aid.hexToByteArray())) }.getOrNull() ?: continue
                if (isSuccessful(response)) {
                    return@withContext EmvCardData(
                        cardType = scheme,
                        aid = aid,
                        contactless = true,
                        privacyMode = true
                    )
                }
            }
            null
        } finally {
            runCatching { iso.close() }
        }
    }

    private fun buildSelectCommand(data: ByteArray): ByteArray = ByteArrayOutputStream().apply {
        write(0x00)
        write(0xA4)
        write(0x04)
        write(0x00)
        write(data.size)
        write(data)
        write(0x00)
    }.toByteArray()

    private fun isSuccessful(response: ByteArray?): Boolean =
        response != null && response.size >= 2 &&
            response[response.size - 2] == 0x90.toByte() &&
            response[response.size - 1] == 0x00.toByte()

    private fun String.hexToByteArray(): ByteArray {
        require(length % 2 == 0)
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}

/** Legacy nullable personal-data fields remain for source compatibility but V5 never populates them. */
data class EmvCardData(
    var pan: String? = null,
    var expiryDate: String? = null,
    var cardholderName: String? = null,
    var cardType: String? = null,
    var aid: String? = null,
    var contactless: Boolean = false,
    var privacyMode: Boolean = true
) {
    fun isValid(): Boolean = !cardType.isNullOrBlank() && !aid.isNullOrBlank()
    fun isComplete(): Boolean = isValid()

    override fun toString(): String = buildString {
        cardType?.let { appendLine("Scheme: $it") }
        aid?.let { appendLine("AID: $it") }
        appendLine("Contactless: $contactless")
        append("Privacy mode: $privacyMode")
    }
}
