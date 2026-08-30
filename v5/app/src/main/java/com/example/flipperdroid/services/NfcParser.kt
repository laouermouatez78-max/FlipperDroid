package com.example.flipperdroid.services

import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import com.example.flipperdroid.model.NdefItem
import com.example.flipperdroid.model.NfcSnapshot
import java.nio.charset.Charset
import java.util.Locale

object NfcParser {
    fun parse(tag: Tag): NfcSnapshot {
        val uid = tag.id.joinToString(":") { "%02X".format(it) }
        val technologies = tag.techList.map { it.substringAfterLast('.') }.sorted()
        val items = mutableListOf<NdefItem>()

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            runCatching {
                ndef.connect()
                val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                message?.records?.forEach { record -> items += parseRecord(record) }
            }
            runCatching { ndef.close() }
        }
        return NfcSnapshot(uid, technologies, items)
    }

    private fun parseRecord(record: NdefRecord): NdefItem {
        if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
            return NdefItem("Texte", parseTextRecord(record.payload))
        }
        record.toUri()?.let { return NdefItem("URI", it.toString()) }
        val mime = runCatching { record.toMimeType() }.getOrNull()
        if (!mime.isNullOrBlank()) {
            val preview = record.payload.toString(Charsets.UTF_8).take(300)
            return NdefItem(mime, preview.ifBlank { hex(record.payload) })
        }
        val type = runCatching { String(record.type, Charsets.US_ASCII) }.getOrDefault("NDEF")
        return NdefItem(type.ifBlank { "NDEF" }, hex(record.payload))
    }

    private fun parseTextRecord(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val status = payload[0].toInt() and 0xFF
        val utf16 = status and 0x80 != 0
        val langLength = status and 0x3F
        val textStart = 1 + langLength
        if (textStart > payload.size) return hex(payload)
        val charset: Charset = if (utf16) Charsets.UTF_16 else Charsets.UTF_8
        return runCatching { String(payload, textStart, payload.size - textStart, charset) }
            .getOrElse { hex(payload) }
    }

    private fun hex(bytes: ByteArray): String = bytes.take(256).joinToString(" ") {
        String.format(Locale.US, "%02X", it.toInt() and 0xFF)
    }
}
