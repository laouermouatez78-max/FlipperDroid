package com.example.flipperdroid.viewmodel

import android.app.Application
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import com.example.flipperdroid.nfc.MifareClassicUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /**
     * V3 performs a metadata-only scan by default. Memory blocks are read only after an
     * explicit action in the NFC screen.
     */
    fun onTagScanned(tag: Tag) {
        _lastTag.value = tag
        val id = tag.id?.let { MifareClassicUtils.bytesToHex(it) } ?: "-"
        val tech = tag.techList.joinToString().ifBlank { "Unknown" }
        _currentTagUid.value = id
        _currentTagType.value = tech
        _currentTagDump.value = emptyList()

        _ndefSummary.value = runCatching {
            val message = Ndef.get(tag)?.cachedNdefMessage
            message?.records?.mapIndexed { index, record ->
                val type = record.type?.toString(Charsets.UTF_8).orEmpty().ifBlank { "unknown" }
                "Record ${index + 1}: TNF=${record.tnf}, type=$type, payload=${record.payload?.size ?: 0} bytes"
            }.orEmpty()
        }.getOrDefault(emptyList())

        val timestamp = now()
        _scanHistory.value = (_scanHistory.value + NfcScanResult(timestamp, id, tech, emptyList())).takeLast(100)
        addLog("Tag detected UID=$id · metadata only")
    }

    /** Explicit MIFARE Classic dump for authorized tags. */
    fun readMifareDump() {
        val tag = _lastTag.value ?: return addLog("Scan a tag first.")
        val mfc = MifareClassic.get(tag) ?: return addLog("This tag is not MIFARE Classic.")
        val dump = mutableListOf<String>()
        try {
            mfc.connect()
            val key = MifareClassicUtils.hexToBytes(MifareClassicUtils.DEFAULT_KEY)
                ?: return addLog("Unable to prepare default MIFARE key.")

            for (sector in 0 until mfc.sectorCount) {
                val blockCount = mfc.getBlockCountInSector(sector)
                val authenticated = runCatching {
                    mfc.authenticateSectorWithKeyA(sector, key)
                }.getOrDefault(false)

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
            addLog("MIFARE read error: ${error.message}")
        } finally {
            runCatching { mfc.close() }
        }
    }

    fun clearDump() {
        _currentTagDump.value = emptyList()
        addLog("Tag memory view cleared")
    }

    fun onDumpExport() {
        val uid = _currentTagUid.value ?: return addLog("Nothing to export: scan a tag first.")
        val app = getApplication<Application>()
        val baseDir = app.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: app.filesDir
        val exportDir = File(baseDir, "FlipperDroidV3").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeUid = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(exportDir, "nfc_${safeUid}_$stamp.txt")

        runCatching {
            file.bufferedWriter().use { writer ->
                writer.appendLine("FlipperDroid V3 NFC export")
                writer.appendLine("Timestamp: ${now()}")
                writer.appendLine("UID: $uid")
                writer.appendLine("Type: ${_currentTagType.value ?: "Unknown"}")
                writer.appendLine("NDEF metadata:")
                _ndefSummary.value.forEach { writer.appendLine("- $it") }
                writer.appendLine("Memory blocks: ${_currentTagDump.value.size}")
                _currentTagDump.value.forEachIndexed { index, line -> writer.appendLine("$index: $line") }
            }
        }.onSuccess {
            addLog("Export saved: ${file.absolutePath}")
        }.onFailure { error ->
            addLog("Export failed: ${error.message}")
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun addLog(msg: String) {
        _logs.value = (_logs.value + "[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $msg").takeLast(200)
    }

    private fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
}

data class NfcScanResult(
    val timestamp: String,
    val uid: String?,
    val type: String?,
    val dump: List<String>
)
