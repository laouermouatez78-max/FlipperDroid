package com.example.flipperdroid.services

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import com.example.flipperdroid.model.NdefItem
import com.example.flipperdroid.model.NfcSnapshot
import com.example.flipperdroid.model.NfcWriteResult
import java.nio.charset.Charset
import java.util.Locale

object NfcParser {
    fun parse(tag: Tag): NfcSnapshot {
        val uid = tag.id.joinToString(":") { "%02X".format(it) }
        val technologies = tag.techList.map { it.substringAfterLast('.') }.sorted()
        val items = mutableListOf<NdefItem>()

        var rawSize: Int? = null
        var writable: Boolean? = null

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            runCatching {
                ndef.connect()
                rawSize = runCatching { ndef.maxSize }.getOrNull()
                writable = runCatching { ndef.isWritable }.getOrNull()
                val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                message?.records?.forEach { record -> items += parseRecord(record) }
            }
            runCatching { ndef.close() }
        }

        return NfcSnapshot(
            uid = uid,
            technologies = technologies,
            tagTypeGuess = guessTagType(tag, technologies),
            ndefItems = items,
            rawSizeBytes = rawSize,
            writable = writable
        )
    }

    /**
     * Best-effort identification of the chip family from the reported tech list.
     * This mirrors what Android already exposes (no proprietary APDU probing),
     * just translated into a label that's actually useful during an audit.
     */
    private fun guessTagType(tag: Tag, technologies: List<String>): String {
        return when {
            technologies.any { it.contains("MifareClassic", true) } -> {
                val size = runCatching { MifareClassic.get(tag)?.size }.getOrNull()
                val sectors = runCatching { MifareClassic.get(tag)?.sectorCount }.getOrNull()
                "MIFARE Classic" + if (size != null) " ($size octets, $sectors secteurs)" else ""
            }
            technologies.any { it.contains("MifareUltralight", true) } -> {
                val type = runCatching { MifareUltralight.get(tag)?.type }.getOrNull()
                val label = when (type) {
                    MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C"
                    MifareUltralight.TYPE_ULTRALIGHT -> "MIFARE Ultralight"
                    else -> "MIFARE Ultralight / NTAG"
                }
                label
            }
            technologies.any { it.contains("IsoDep", true) } &&
                technologies.any { it.contains("NfcA", true) } -> "ISO 14443-4 (carte à puce / EMV-compatible)"
            technologies.any { it.contains("NfcA", true) } -> "ISO 14443-3A (NfcA)"
            technologies.any { it.contains("NfcB", true) } -> "ISO 14443-3B (NfcB)"
            technologies.any { it.contains("NfcF", true) } -> "FeliCa (NfcF)"
            technologies.any { it.contains("NfcV", true) } -> "ISO 15693 (NfcV)"
            technologies.any { it.contains("NfcBarcode", true) } -> "NFC Barcode (Thinfilm)"
            else -> "Type non déterminé"
        }
    }

    /**
     * Writes a single NDEF text record to the tag the user is physically holding
     * against the phone right now. This is standard consumer NFC writing (the same
     * capability every NFC tag-writer app exposes) — not silent, not automated on
     * arbitrary tags: it only fires once armed explicitly and a tag is presented.
     */
    fun writeText(tag: Tag, text: String): NfcWriteResult {
        val record = NdefRecord.createTextRecord(Locale.getDefault().language, text)
        val message = NdefMessage(arrayOf(record))
        val payloadSize = message.toByteArray().size

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            val result = runCatching {
                ndef.connect()
                when {
                    !ndef.isWritable -> NfcWriteResult(false, "Tag non inscriptible (protégé en écriture)")
                    ndef.maxSize < payloadSize -> NfcWriteResult(false, "Message trop grand pour ce tag ($payloadSize > ${ndef.maxSize} octets)")
                    else -> {
                        ndef.writeNdefMessage(message)
                        NfcWriteResult(true, "Texte écrit avec succès ($payloadSize octets)")
                    }
                }
            }.getOrElse { NfcWriteResult(false, "Échec d'écriture: ${it.message ?: it.javaClass.simpleName}") }
            runCatching { ndef.close() }
            return result
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            val result = runCatching {
                formatable.connect()
                formatable.format(message)
                NfcWriteResult(true, "Tag vierge formaté et texte écrit ($payloadSize octets)")
            }.getOrElse { NfcWriteResult(false, "Échec de formatage: ${it.message ?: it.javaClass.simpleName}") }
            runCatching { formatable.close() }
            return result
        }

        return NfcWriteResult(false, "Ce tag ne supporte pas NDEF (ni inscriptible ni formatable)")
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
