package com.example.flipperdroid.viewmodel

import android.app.Application
import android.nfc.Tag
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

    private val _currentTagUid = MutableStateFlow<String?>(null)
    val currentTagUid: StateFlow<String?> = _currentTagUid.asStateFlow()

    private val _currentTagType = MutableStateFlow<String?>(null)
    val currentTagType: StateFlow<String?> = _currentTagType.asStateFlow()

    private val _currentTagDump = MutableStateFlow<List<String>>(emptyList())
    val currentTagDump: StateFlow<List<String>> = _currentTagDump.asStateFlow()

    private val _scanHistory = MutableStateFlow<List<NfcScanResult>>(emptyList())
    val scanHistory: StateFlow<List<NfcScanResult>> = _scanHistory.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun onTagScanned(tag: Tag) {
        val id = tag.id?.let { MifareClassicUtils.bytesToHex(it) } ?: "-"
        _currentTagUid.value = id
        _currentTagType.value = tag.techList.joinToString().ifBlank { "Unknown" }

        val dump = mutableListOf<String>()
        val mfc = android.nfc.tech.MifareClassic.get(tag)

        if (mfc != null) {
            try {
                mfc.connect()
                for (sector in 0 until mfc.sectorCount) {
                    val key = MifareClassicUtils.hexToBytes(MifareClassicUtils.DEFAULT_KEY) ?: continue
                    val authenticated = mfc.authenticateSectorWithKeyA(sector, key)
                    val blockCount = mfc.getBlockCountInSector(sector)

                    if (authenticated) {
                        val firstBlock = mfc.sectorToBlock(sector)
                        repeat(blockCount) { offset ->
                            val block = try {
                                mfc.readBlock(firstBlock + offset)
                            } catch (_: Exception) {
                                null
                            }
                            dump.add(block?.let(MifareClassicUtils::bytesToHex) ?: MifareClassicUtils.NO_DATA)
                        }
                    } else {
                        repeat(blockCount) { dump.add(MifareClassicUtils.NO_DATA) }
                    }
                }
            } catch (e: Exception) {
                addLog("NFC read error: ${e.message}")
            } finally {
                runCatching { mfc.close() }
            }
        } else {
            addLog("Tag detected; MIFARE Classic block reading is not available for this tag type.")
        }

        _currentTagDump.value = dump
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val scan = NfcScanResult(timestamp, id, _currentTagType.value, dump)
        _scanHistory.value = _scanHistory.value + scan
        addLog("Tag scanned UID=$id (${dump.size} readable blocks)")
    }

    fun onDumpExport() {
        val uid = _currentTagUid.value ?: return addLog("Nothing to export: scan a tag first.")
        val dump = _currentTagDump.value
        val app = getApplication<Application>()
        val baseDir = app.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: app.filesDir
        val exportDir = File(baseDir, "FlipperDroidV2").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeUid = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(exportDir, "nfc_${safeUid}_$stamp.txt")

        runCatching {
            file.bufferedWriter().use { writer ->
                writer.appendLine("FlipperDroid V2 NFC export")
                writer.appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                writer.appendLine("UID: $uid")
                writer.appendLine("Type: ${_currentTagType.value ?: "Unknown"}")
                writer.appendLine("Blocks: ${dump.size}")
                writer.appendLine()
                dump.forEachIndexed { index, line ->
                    writer.appendLine("$index: $line")
                }
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
        _logs.value = (_logs.value + msg).takeLast(200)
    }
}

data class NfcScanResult(
    val timestamp: String,
    val uid: String?,
    val type: String?,
    val dump: List<String>
)
