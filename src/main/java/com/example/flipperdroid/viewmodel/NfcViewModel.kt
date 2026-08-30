package com.example.flipperdroid.viewmodel

import android.app.Application
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flipperdroid.nfc.MifareClassicUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NfcViewModel(app: Application) : AndroidViewModel(app) {

    private val _lastTag = MutableStateFlow<Tag?>(null)
    val lastTag: StateFlow<Tag?> = _lastTag.asStateFlow()

    private val _currentTagUid = MutableStateFlow<String?>(null)
    val currentTagUid: StateFlow<String?> = _currentTagUid.asStateFlow()

    private val _currentTagType = MutableStateFlow<String?>(null)
    val currentTagType: StateFlow<String?> = _currentTagType.asStateFlow()

    private val _ndefSummary = MutableStateFlow<List<String>>(emptyList())
    val ndefSummary: StateFlow<List<String>> = _ndefSummary.asStateFlow()

    private val _currentTagDump = MutableStateFlow<List<String>>(emptyList())
    val currentTagDump: StateFlow<List<String>> = _currentTagDump.asStateFlow()

    private val _scanHistory = MutableStateFlow<List<NfcScanResult>>(emptyList())
    val scanHistory: StateFlow<List<NfcScanResult>> = _scanHistory.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun onTagScanned(tag: Tag) {
        _lastTag.value = tag
        val id = tag.id?.let { MifareClassicUtils.bytesToHex(it) } ?: "-"
        val tech = tag.techList.joinToString().ifBlank { "Unknown" }
        _currentTagUid.value = id
        _currentTagType.value = tech
        _currentTagDump.value = emptyList()
        refreshNdefSummary(tag)

        val timestamp = now()
        _scanHistory.value = (_scanHistory.value + NfcScanResult(timestamp, id, tech, emptyList())).takeLast(100)
        addLog("Tag detected UID=$id · metadata only")
    }

    private fun refreshNdefSummary(tag: Tag) {
        _ndefSummary.value = runCatching {
            val ndef = Ndef.get(tag)
            val message = ndef?.cachedNdefMessage
            val records = message?.records?.mapIndexed { index, record ->
                val type = record.type?.toString(Charsets.UTF_8).orEmpty().ifBlank { "unknown" }
                "Record ${index + 1}: TNF=${record.tnf}, type=$type, payload=${record.payload?.size ?: 0} bytes"
            }.orEmpty().toMutableList()
            if (ndef != null) records.add(0, "Writable=${ndef.isWritable} · maxSize=${ndef.maxSize} bytes")
            records
        }.getOrDefault(emptyList())
    }

    fun writeNdefText(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return addLog("Enter text before writing NDEF.")
        if (clean.length > 8_192) return addLog("NDEF text is too long for the V5 editor (max 8192 characters).")
        val tag = _lastTag.value ?: return addLog("Scan an NDEF tag first.")
        if (_isBusy.value) return addLog("Another NFC operation is already running.")

        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            val ndef = Ndef.get(tag)
            if (ndef == null) {
                addLog("This tag does not expose writable NDEF technology.")
                _isBusy.value = false
                return@launch
            }

            val language = Locale.getDefault().language.ifBlank { "en" }
            val record = NdefRecord.createTextRecord(language, clean)
            val message = NdefMessage(arrayOf(record))
            val encodedSize = message.toByteArray().size

            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    addLog("Tag is NDEF but read-only.")
                    return@launch
                }
                if (encodedSize > ndef.maxSize) {
                    addLog("Text is too large for this tag ($encodedSize/${ndef.maxSize} bytes).")
                    return@launch
                }
                ndef.writeNdefMessage(message)
                addLog("NDEF text written successfully · $encodedSize encoded bytes · language=$language")
                refreshNdefSummary(tag)
            } catch (e: Exception) {
                addLog("NDEF write failed: ${safeMessage(e)}")
            } finally {
                runCatching { ndef.close() }
                _isBusy.value = false
            }
        }
    }

    fun readMifareDump() {
        val tag = _lastTag.value ?: return addLog("Scan a tag first.")
        if (_isBusy.value) return addLog("Another NFC operation is already running.")

        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            val mfc = MifareClassic.get(tag)
            if (mfc == null) {
                addLog("This tag is not MIFARE Classic.")
                _isBusy.value = false
                return@launch
            }
            val dump = mutableListOf<String>()
            try {
                mfc.connect()
                val key = MifareClassicUtils.hexToBytes(MifareClassicUtils.DEFAULT_KEY)
                if (key == null) {
                    addLog("Unable to prepare default MIFARE key.")
                    return@launch
                }

                for (sector in 0 until mfc.sectorCount) {
                    val blockCount = mfc.getBlockCountInSector(sector)
                    val authenticated = runCatching { mfc.authenticateSectorWithKeyA(sector, key) }.getOrDefault(false)
                    if (!authenticated) {
                        repeat(blockCount) { dump += MifareClassicUtils.NO_DATA }
                        continue
                    }
                    val firstBlock = mfc.sectorToBlock(sector)
                    repeat(blockCount) { offset ->
                        val block = runCatching { mfc.readBlock(firstBlock + offset) }.getOrNull()
                        dump += block?.let(MifareClassicUtils::bytesToHex) ?: MifareClassicUtils.NO_DATA
                    }
                }
                _currentTagDump.value = dump
                addLog("Explicit MIFARE read complete · ${dump.count { it != MifareClassicUtils.NO_DATA }} readable block(s)")
            } catch (error: Exception) {
                addLog("MIFARE read error: ${safeMessage(error)}")
            } finally {
                runCatching { mfc.close() }
                _isBusy.value = false
            }
        }
    }

    fun clearDump() {
        _currentTagDump.value = emptyList()
        addLog("Tag memory view cleared")
    }

    fun onDumpExport() {
        val uid = _currentTagUid.value ?: return addLog("Nothing to export: scan a tag first.")
        if (_isBusy.value) return addLog("Another NFC operation is already running.")

        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            try {
                val app = getApplication<Application>()
                val baseDir = app.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: app.filesDir
                val exportDir = File(baseDir, "FlipperDroidV5").apply { mkdirs() }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val safeUid = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
                val file = File(exportDir, "nfc_${safeUid}_$stamp.txt")

                file.bufferedWriter().use { writer ->
                    writer.appendLine("FlipperDroid V5 NFC export")
                    writer.appendLine("Timestamp: ${now()}")
                    writer.appendLine("UID: $uid")
                    writer.appendLine("Type: ${_currentTagType.value ?: "Unknown"}")
                    writer.appendLine("NDEF metadata:")
                    _ndefSummary.value.forEach { writer.appendLine("- $it") }
                    writer.appendLine("Memory blocks: ${_currentTagDump.value.size}")
                    _currentTagDump.value.forEachIndexed { index, line -> writer.appendLine("$index: $line") }
                }
                addLog("Export saved in app Documents: ${file.name}")
            } catch (error: Exception) {
                addLog("Export failed: ${safeMessage(error)}")
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun clearLogs() { _logs.value = emptyList() }
    fun clearHistory() { _scanHistory.value = emptyList() }

    fun addLog(msg: String) {
        _logs.value = (_logs.value + "[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $msg").takeLast(200)
    }

    private fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    private fun safeMessage(error: Throwable): String = error.message?.take(160) ?: error.javaClass.simpleName
}

data class NfcScanResult(
    val timestamp: String,
    val uid: String?,
    val type: String?,
    val dump: List<String>
)
