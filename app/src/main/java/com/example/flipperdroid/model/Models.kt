package com.example.flipperdroid.model

enum class ThemeMode { SYSTEM, DARK, LIGHT }
enum class Accent { MINT, CYAN, AMBER }

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val module: String,
    val message: String
)

data class BleDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val txPower: Int?,
    val manufacturerId: Int?,
    val manufacturerName: String? = null,
    val serviceUuids: List<String>,
    val connectable: Boolean?,
    val randomAddress: Boolean = false,
    val rssiHistory: List<Int> = emptyList(),
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val packetCount: Int = 1
)

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val channel: Int?,
    val channelWidth: String,
    val security: String,
    val capabilities: String,
    val standard: String,
    val band: String
)

data class NdefItem(
    val type: String,
    val value: String
)

data class NfcWriteResult(
    val success: Boolean,
    val message: String
)

data class NfcSnapshot(
    val uid: String,
    val technologies: List<String>,
    val tagTypeGuess: String,
    val ndefItems: List<NdefItem>,
    val rawSizeBytes: Int? = null,
    val writable: Boolean? = null,
    val scannedAt: Long = System.currentTimeMillis()
)

data class NetworkSnapshot(
    val transport: String,
    val interfaceName: String?,
    val addresses: List<String>,
    val dnsServers: List<String>,
    val gateways: List<String>,
    val validated: Boolean,
    val metered: Boolean
)

data class ProbeResult(
    val target: String,
    val success: Boolean,
    val latencyMs: Long?,
    val detail: String
)

data class UsbEndpointInfo(
    val interfaceId: Int,
    val interfaceClass: Int,
    val endpointAddress: Int,
    val direction: String,
    val type: String,
    val maxPacketSize: Int
)

data class UsbDeviceInfo(
    val deviceId: Int,
    val name: String,
    val manufacturer: String?,
    val product: String?,
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int,
    val interfaceCount: Int,
    val endpoints: List<UsbEndpointInfo>,
    val knownDeviceLabel: String? = null,
    val sdrCapable: Boolean = false
)

data class IrInfo(
    val hasEmitter: Boolean,
    val carrierRanges: List<CarrierRange>
)

data class CarrierRange(val minHz: Int, val maxHz: Int) {
    fun contains(hz: Int) = hz in minHz..maxHz
    override fun toString(): String = "${minHz / 1000}–${maxHz / 1000} kHz"
}

data class IrTransmitResult(
    val success: Boolean,
    val message: String,
    val frequencyHz: Int
)

/**
 * Sub-GHz (315/433/868/915 MHz) has no native radio on Android smartphones — this is
 * a hardware limitation, not a software one. This model reports that honestly and
 * surfaces any recognized external SDR/Sub-GHz USB dongle instead of faking a capability.
 */
data class SubGhzStatus(
    val nativeRadioAvailable: Boolean = false,
    val usbHostAvailable: Boolean,
    val recognizedDongle: UsbDeviceInfo?,
    val note: String
)

data class HardwareSnapshot(
    val nfcPresent: Boolean,
    val nfcEnabled: Boolean,
    val bluetoothPresent: Boolean,
    val bluetoothEnabled: Boolean,
    val bleAdvertisingSupported: Boolean,
    val wifiPresent: Boolean,
    val usbHost: Boolean,
    val irEmitter: Boolean
)

data class PortProbeSummary(
    val host: String,
    val results: List<ProbeResult>,
    val runningAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.MINT,
    val privacyMode: Boolean = true
)

data class BeaconState(
    val active: Boolean = false,
    val message: String = "Prêt"
)
