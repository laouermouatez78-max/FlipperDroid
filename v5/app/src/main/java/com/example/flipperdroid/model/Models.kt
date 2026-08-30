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
    val serviceUuids: List<String>,
    val connectable: Boolean?,
    val lastSeen: Long = System.currentTimeMillis()
)

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val channel: Int?,
    val security: String,
    val capabilities: String
)

data class NdefItem(
    val type: String,
    val value: String
)

data class NfcSnapshot(
    val uid: String,
    val technologies: List<String>,
    val ndefItems: List<NdefItem>,
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
    val endpoints: List<UsbEndpointInfo>
)

data class IrInfo(
    val hasEmitter: Boolean,
    val carrierRanges: List<String>
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

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.MINT,
    val privacyMode: Boolean = true
)

data class BeaconState(
    val active: Boolean = false,
    val message: String = "Prêt"
)
