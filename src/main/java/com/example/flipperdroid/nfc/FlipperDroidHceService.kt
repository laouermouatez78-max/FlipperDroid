package com.example.flipperdroid.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ApduLabState {
    @Volatile
    var enabled: Boolean = false

    private val _transcript = MutableStateFlow<List<String>>(emptyList())
    val transcript: StateFlow<List<String>> = _transcript

    fun clear() {
        _transcript.value = emptyList()
    }

    fun append(line: String) {
        _transcript.value = (_transcript.value + line).takeLast(120)
    }
}

class FlipperDroidHceService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val command = commandApdu ?: return SW_ERROR
        ApduLabState.append("CARD RX ${command.toHex()}")

        val response = when {
            !ApduLabState.enabled -> SW_CONDITIONS_NOT_SATISFIED
            command.contentEquals(SELECT_APDU) -> "FDV4".toByteArray(Charsets.US_ASCII) + SW_OK
            command.contentEquals(PING_APDU) -> "PONG".toByteArray(Charsets.US_ASCII) + SW_OK
            isEcho(command) -> {
                val length = command[4].toInt() and 0xFF
                val data = command.copyOfRange(5, 5 + length)
                data + SW_OK
            }
            else -> SW_INS_NOT_SUPPORTED
        }

        ApduLabState.append("CARD TX ${response.toHex()}")
        return response
    }

    override fun onDeactivated(reason: Int) {
        ApduLabState.append("CARD link closed · reason=$reason")
    }

    private fun isEcho(command: ByteArray): Boolean {
        if (command.size < 5) return false
        if ((command[0].toInt() and 0xFF) != 0x80 || (command[1].toInt() and 0xFF) != 0x20) return false
        val length = command[4].toInt() and 0xFF
        return length in 1..24 && command.size >= 5 + length
    }

    companion object {
        const val AID_HEX = "F0010203040506"
        val SELECT_APDU: ByteArray = "00A4040007${AID_HEX}00".hexToBytes()
        val PING_APDU: ByteArray = "8010000000".hexToBytes()
        val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        val SW_CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())
        val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00)
        val SW_ERROR = byteArrayOf(0x6F.toByte(), 0x00)
    }
}

fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

fun String.hexToBytes(): ByteArray {
    val clean = replace(" ", "")
    require(clean.length % 2 == 0)
    return ByteArray(clean.length / 2) { index ->
        clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
}
