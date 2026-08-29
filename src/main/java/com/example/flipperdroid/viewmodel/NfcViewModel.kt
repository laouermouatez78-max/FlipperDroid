package com.example.flipperdroid.viewmodel

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.flipperdroid.nfc.MifareClassicUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NfcViewModel(app: Application) : AndroidViewModel(app) {

    // État du tag courant
    private val _currentTagUid = MutableStateFlow<String?>(null)
    val currentTagUid: StateFlow<String?> = _currentTagUid.asStateFlow()

    private val _currentTagType = MutableStateFlow<String?>(null)
    val currentTagType: StateFlow<String?> = _currentTagType.asStateFlow()

    private val _currentTagDump = MutableStateFlow<List<String>>(emptyList())
    val currentTagDump: StateFlow<List<String>> = _currentTagDump.asStateFlow()

    // Historique des scans
    private val _scanHistory = MutableStateFlow<List<NfcScanResult>>(emptyList())
    val scanHistory: StateFlow<List<NfcScanResult>> = _scanHistory.asStateFlow()

    // Logs/feedback
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    // Actions principales
    fun onTagScanned(tag: Tag) {
        // Lecture UID
        val id = tag.id?.let { MifareClassicUtils.bytesToHex(it) } ?: "-"
        _currentTagUid.value = id
        _currentTagType.value = "Mifare Classic"
        // Dump rapide avec la clé par défaut sur tous les secteurs
        val dump = mutableListOf<String>()
        val mfc = android.nfc.tech.MifareClassic.get(tag)
        if (mfc != null) {
            try {
                mfc.connect()
                val sectorCount = mfc.sectorCount
                for (sector in 0 until sectorCount) {
                    val key = MifareClassicUtils.hexToBytes(MifareClassicUtils.DEFAULT_KEY) ?: continue
                    val auth = mfc.authenticateSectorWithKeyA(sector, key)
                    if (auth) {
                        val firstBlock = mfc.sectorToBlock(sector)
                        val blockCount = mfc.getBlockCountInSector(sector)
                        for (i in 0 until blockCount) {
                            val blockBytes = try { mfc.readBlock(firstBlock + i) } catch (_: Exception) { null }
                            dump.add(blockBytes?.let { MifareClassicUtils.bytesToHex(it) } ?: MifareClassicUtils.NO_DATA)
                        }
                    } else {
                        // Secteur non accessible avec la clé par défaut
                        val blockCount = mfc.getBlockCountInSector(sector)
                        repeat(blockCount) { dump.add(MifareClassicUtils.NO_DATA) }
                    }
                }
                mfc.close()
            } catch (e: Exception) {
                addLog("Erreur lecture tag: ${e.message}")
            }
        } else {
            addLog("Tag non compatible MifareClassic")
        }
        _currentTagDump.value = dump
        // Ajout à l'historique
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val scan = NfcScanResult(timestamp, id, "Mifare Classic", dump)
        _scanHistory.value = _scanHistory.value + scan
        addLog("Tag scanné UID=$id, ${dump.size} blocks lus")
    }
    fun onCloneUid(newUid: String) {
        // Clonage UID (block 0) - nécessite une carte "magic"
        val lastScan = _scanHistory.value.lastOrNull() ?: return addLog("Aucun tag scanné")
        val tag = lastScan.uid?.let { _currentTagUid.value } ?: return addLog("Aucun tag scanné")
        // On ne peut pas retrouver le Tag Android à partir de l'UID, donc cette fonction doit être appelée juste après un scan
        // (dans une vraie app, il faudrait garder le dernier Tag en mémoire)
        addLog("Clonage UID non supporté sans accès au Tag Android courant")
        // Pour une vraie intégration, il faudrait passer le Tag Android courant à cette fonction
    }
    fun onDumpExport() {
        // TODO : export du dump courant
    }
    fun clearLogs() {
        _logs.value = emptyList()
    }
    fun addLog(msg: String) {
        _logs.value = _logs.value + msg
    }
}

// Modèle pour l’historique
data class NfcScanResult(
    val timestamp: String,
    val uid: String?,
    val type: String?,
    val dump: List<String>
)