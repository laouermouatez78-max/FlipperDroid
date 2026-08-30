package com.example.flipperdroid

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flipperdroid.model.*
import com.example.flipperdroid.services.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val settingsStore = SettingsStore(context)
    private val bleScanner = BleScanner(context)
    private val wifiScanner = WifiScanner(context)
    private val networkDiagnostics = NetworkDiagnostics(context)
    private val usbInspector = UsbInspector(context)
    private val irInspector = IrInspector(context)
    private val labBeacon = BleLabBeacon(context)

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    val bleDevices: StateFlow<List<BleDevice>> = bleScanner.devices
    val wifiNetworks: StateFlow<List<WifiNetwork>> = wifiScanner.networks
    val wifiStatus: StateFlow<String> = wifiScanner.status
    val beaconState: StateFlow<BeaconState> = labBeacon.state

    private val _nfc = MutableStateFlow<NfcSnapshot?>(null)
    val nfc: StateFlow<NfcSnapshot?> = _nfc.asStateFlow()

    private val _nfcWritePending = MutableStateFlow<String?>(null)
    val nfcWritePending: StateFlow<String?> = _nfcWritePending.asStateFlow()

    private val _nfcWriteResult = MutableStateFlow<NfcWriteResult?>(null)
    val nfcWriteResult: StateFlow<NfcWriteResult?> = _nfcWriteResult.asStateFlow()

    private val _hardware = MutableStateFlow(readHardware())
    val hardware: StateFlow<HardwareSnapshot> = _hardware.asStateFlow()

    private val _network = MutableStateFlow(networkDiagnostics.snapshot())
    val network: StateFlow<NetworkSnapshot> = _network.asStateFlow()

    private val _usb = MutableStateFlow<List<UsbDeviceInfo>>(emptyList())
    val usb: StateFlow<List<UsbDeviceInfo>> = _usb.asStateFlow()

    private val _ir = MutableStateFlow(irInspector.info())
    val ir: StateFlow<IrInfo> = _ir.asStateFlow()

    private val _irTransmitResult = MutableStateFlow<IrTransmitResult?>(null)
    val irTransmitResult: StateFlow<IrTransmitResult?> = _irTransmitResult.asStateFlow()

    private val _subGhz = MutableStateFlow(readSubGhzStatus())
    val subGhz: StateFlow<SubGhzStatus> = _subGhz.asStateFlow()

    private val _probe = MutableStateFlow<ProbeResult?>(null)
    val probe: StateFlow<ProbeResult?> = _probe.asStateFlow()

    private val _multiProbe = MutableStateFlow<PortProbeSummary?>(null)
    val multiProbe: StateFlow<PortProbeSummary?> = _multiProbe.asStateFlow()

    private val _bleScanning = MutableStateFlow(false)
    val bleScanning: StateFlow<Boolean> = _bleScanning.asStateFlow()
    private var bleStopJob: Job? = null

    init {
        bleScanner.onEvent = { event ->
            _bleScanning.value = bleScanner.isScanning
            log("BLE", event)
        }
        log("APP", "FlipperDroid 6.0 démarré")
        refreshAll()
    }

    fun setThemeMode(mode: ThemeMode) = updateSettings(_settings.value.copy(themeMode = mode))
    fun setAccent(accent: Accent) = updateSettings(_settings.value.copy(accent = accent))
    fun setPrivacy(enabled: Boolean) = updateSettings(_settings.value.copy(privacyMode = enabled))

    private fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        settingsStore.save(newSettings)
        log("APP", "Réglages mis à jour")
    }

    fun refreshAll() {
        _hardware.value = readHardware()
        _network.value = networkDiagnostics.snapshot()
        _usb.value = runCatching { usbInspector.devices() }.getOrDefault(emptyList())
        _ir.value = irInspector.info()
        _subGhz.value = readSubGhzStatus()
    }

    fun refreshNetwork() {
        _network.value = networkDiagnostics.snapshot()
        log("NET", "État réseau actualisé")
    }

    fun refreshUsb() {
        _usb.value = runCatching { usbInspector.devices() }.getOrDefault(emptyList())
        _subGhz.value = readSubGhzStatus()
        log("USB", "Inventaire actualisé: ${_usb.value.size} périphérique(s)")
    }

    fun startBleScan() {
        bleStopJob?.cancel()
        bleScanner.start()
        _bleScanning.value = bleScanner.isScanning
        if (bleScanner.isScanning) {
            bleStopJob = viewModelScope.launch {
                delay(30_000)
                stopBleScan()
            }
        }
    }

    fun stopBleScan() {
        bleStopJob?.cancel()
        bleStopJob = null
        bleScanner.stop()
        _bleScanning.value = false
    }

    fun clearBle() {
        bleScanner.clear()
        log("BLE", "Liste BLE vidée")
    }

    /** Returns a CSV snapshot of the current BLE scan for local export/sharing. */
    fun exportBleCsv(): String {
        log("BLE", "Export CSV (${bleDevices.value.size} appareil(s))")
        return bleScanner.exportCsv()
    }

    fun scanWifi() {
        wifiScanner.scan()
        log("WIFI", wifiScanner.status.value)
    }

    /** Returns a CSV snapshot of the current Wi‑Fi scan for local export/sharing. */
    fun exportWifiCsv(): String {
        log("WIFI", "Export CSV (${wifiNetworks.value.size} réseau(x))")
        return wifiScanner.exportCsv()
    }

    fun onNfcTag(tag: Tag) {
        viewModelScope.launch {
            val pendingText = _nfcWritePending.value
            if (pendingText != null) {
                val result = withContext(Dispatchers.IO) { NfcParser.writeText(tag, pendingText) }
                _nfcWriteResult.value = result
                _nfcWritePending.value = null
                log("NFC", "Écriture: ${result.message}")
                if (result.success) {
                    val reread = runCatching { withContext(Dispatchers.IO) { NfcParser.parse(tag) } }
                    reread.onSuccess { _nfc.value = it }
                }
                return@launch
            }
            val parsed = runCatching { withContext(Dispatchers.IO) { NfcParser.parse(tag) } }
            parsed.onSuccess {
                _nfc.value = it
                log("NFC", "Tag lu: ${it.uid} — ${it.tagTypeGuess}")
            }.onFailure {
                log("NFC", "Lecture impossible: ${it.message ?: it.javaClass.simpleName}")
            }
        }
    }

    /** Arms a one-shot write: the next tag physically presented receives this text. */
    fun armNfcWrite(text: String) {
        _nfcWritePending.value = text
        _nfcWriteResult.value = null
        log("NFC", "Écriture armée — approchez le tag cible")
    }

    fun cancelNfcWrite() {
        _nfcWritePending.value = null
        log("NFC", "Écriture annulée")
    }

    fun clearNfc() {
        _nfc.value = null
        log("NFC", "Dernière lecture effacée")
    }

    fun resolveHost(host: String) {
        viewModelScope.launch {
            _probe.value = ProbeResult(host, false, null, "Résolution en cours…")
            val result = networkDiagnostics.resolve(host)
            _probe.value = result
            log("NET", "DNS ${result.target}: ${result.detail}")
        }
    }

    fun tcpProbe(host: String, port: Int) {
        viewModelScope.launch {
            _probe.value = ProbeResult("$host:$port", false, null, "Test en cours…")
            val result = networkDiagnostics.tcpProbe(host, port)
            _probe.value = result
            log("NET", "TCP ${result.target}: ${result.detail}")
        }
    }

    /** Probes several ports on the single provided host (e.g. "22,80,443"). */
    fun tcpProbeMultiple(host: String, ports: List<Int>) {
        viewModelScope.launch {
            _multiProbe.value = PortProbeSummary(host, emptyList())
            val results = networkDiagnostics.tcpProbeMultiple(host, ports)
            _multiProbe.value = PortProbeSummary(host, results)
            val openCount = results.count { it.success }
            log("NET", "Sondage $host (${ports.size} port(s)): $openCount ouvert(s)")
        }
    }

    fun startLabBeacon() {
        labBeacon.start()
        log("LAB", "Demande de balise BLE limitée à 30 s")
    }

    fun stopLabBeacon() {
        labBeacon.stop()
        log("LAB", "Balise BLE arrêtée")
    }

    /** Fires a short, bounded IR test burst at a user-specified frequency/pattern. */
    fun transmitIrTest(frequencyHz: Int, pattern: IntArray) {
        val result = irInspector.transmitTest(frequencyHz, pattern)
        _irTransmitResult.value = result
        log("IR", result.message)
    }

    fun clearLogs() {
        _logs.value = emptyList()
        log("APP", "Journal réinitialisé")
    }

    /** Resets stored preferences to defaults and clears the in-memory log buffer. */
    fun resetLocalData() {
        val defaults = AppSettings()
        _settings.value = defaults
        settingsStore.save(defaults)
        _logs.value = emptyList()
        log("APP", "Données locales réinitialisées (réglages + journal)")
    }

    fun logsAsText(): String = _logs.value.asReversed().joinToString("\n") { entry ->
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
        "$time [${entry.module}] ${entry.message}"
    }

    /** Builds a JSON array export of the current log buffer for local sharing/analysis. */
    fun logsAsJson(): String {
        val items = _logs.value.asReversed().joinToString(",\n") { entry ->
            val escapedMessage = entry.message.replace("\\", "\\\\").replace("\"", "\\\"")
            val escapedModule = entry.module.replace("\\", "\\\\").replace("\"", "\\\"")
            "  {\"timestamp\": ${entry.timestamp}, \"module\": \"$escapedModule\", \"message\": \"$escapedMessage\"}"
        }
        return "[\n$items\n]"
    }

    fun stopActiveRadios() {
        stopBleScan()
        labBeacon.stop()
    }

    private fun log(module: String, message: String) {
        val next = listOf(LogEntry(module = module, message = message)) + _logs.value
        _logs.value = next.take(500)
    }

    private fun readHardware(): HardwareSnapshot {
        val pm = context.packageManager
        val nfc = NfcAdapter.getDefaultAdapter(context)
        val bt = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        return HardwareSnapshot(
            nfcPresent = nfc != null,
            nfcEnabled = nfc?.isEnabled == true,
            bluetoothPresent = bt != null,
            bluetoothEnabled = runCatching { bt?.isEnabled == true }.getOrDefault(false),
            bleAdvertisingSupported = runCatching { bt?.isMultipleAdvertisementSupported == true }.getOrDefault(false),
            wifiPresent = pm.hasSystemFeature(PackageManager.FEATURE_WIFI) || wifi != null,
            usbHost = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
            irEmitter = ir?.hasIrEmitter() == true
        )
    }

    private fun readSubGhzStatus(): SubGhzStatus {
        val usbHost = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        val dongle = runCatching { usbInspector.findSdrDongle() }.getOrNull()
        val note = when {
            dongle != null -> "Dongle SDR/Sub-GHz reconnu: ${dongle.knownDeviceLabel}"
            usbHost -> "Aucune radio Sub-GHz native sur Android. Branchez un dongle SDR/Sub-GHz compatible (RTL-SDR, HackRF, YARD Stick One…) en USB‑OTG."
            else -> "Aucune radio Sub-GHz native et pas d'hôte USB détecté sur cet appareil."
        }
        return SubGhzStatus(
            nativeRadioAvailable = false,
            usbHostAvailable = usbHost,
            recognizedDongle = dongle,
            note = note
        )
    }

    override fun onCleared() {
        stopActiveRadios()
        wifiScanner.close()
        super.onCleared()
    }
}
