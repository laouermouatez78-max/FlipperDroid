package com.example.flipperdroid

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flipperdroid.ui.theme.FlipperDroidTheme
import com.example.flipperdroid.view.*
import com.example.flipperdroid.viewmodel.*

class MainActivity : ComponentActivity() {

    private val nfcViewModel: NfcViewModel by viewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private val bleAdvertiserViewModel: BleAdvertiserViewModel by viewModels()
    private val networkToolsViewModel: NetworkToolsViewModel by viewModels()
    private val wifiAuditViewModel: WifiDeautherViewModel by viewModels()
    private val lanAnalyzerViewModel: LanAnalyzerViewModel by viewModels()
    private val usbInspectorViewModel: UsbInspectorViewModel by viewModels()
    private val badUsbViewModel: BadUsbViewModel by viewModels()
    private val emvCardEmulationViewModel: EmvCardEmulationViewModel by viewModels()
    private val emvReaderViewModel: EmvReaderViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureNfc()
        bluetoothViewModel.initialize(this)
        networkToolsViewModel.initialize(this)

        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            FlipperDroidTheme(darkTheme = isDarkMode) {
                AppNavigation(themeViewModel)
            }
        }
        handleNfcIntent(intent)
    }

    private fun configureNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) return

        val launchIntent = Intent(this, javaClass).apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        val mutabilityFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            intent.getParcelableCompat(NfcAdapter.EXTRA_TAG, Tag::class.java)?.let(nfcViewModel::onTagScanned)
        }
    }

    private fun <T : Parcelable> Intent.getParcelableCompat(key: String, clazz: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableExtra(key, clazz)
        else {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            (getParcelableExtra<Parcelable>(key) as? T)
        }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter ?: return
        val foregroundIntent = pendingIntent ?: return
        if (runCatching { adapter.isEnabled }.getOrDefault(false)) {
            adapter.enableForegroundDispatch(this, foregroundIntent, null, null)
        }
    }

    override fun onPause() {
        super.onPause()
        runCatching { nfcAdapter?.disableForegroundDispatch(this) }
    }

    @SuppressLint("ComposableDestinationInComposeScope")
    @Composable
    private fun AppNavigation(themeViewModel: ThemeViewModel) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "home") {
            composable("home") { HomeScreen(navController, nfcViewModel) }
            composable("nfc") { NfcScreen(navController, nfcViewModel) }
            composable("bluetooth_scan") { BluetoothScannerScreen(navController, bluetoothViewModel) }
            composable("ble_advertiser") { BleAdvertiserScreen(navController, bleAdvertiserViewModel) }
            composable("ble_payload_lab") { BleSpamScreen(navController) }
            composable("wifi_deauther") { WifiDeautherScreen(navController, wifiAuditViewModel) }
            composable("lan_analyzer") { LanAnalyzerScreen(navController, lanAnalyzerViewModel) }
            composable("network") { NetworkToolsScreen(navController, networkToolsViewModel) }
            composable("usb_inspector") { UsbInspectorScreen(navController, usbInspectorViewModel) }
            composable("usb_hid_lab") { BadUsbScreen(navController, badUsbViewModel, usbInspectorViewModel) }
            composable("ir") { InfraredScreen(navController) }
            composable("password_generator") { PasswordGeneratorScreen(navController) }
            composable("qr_scanner") { QrScannerScreen(navController) }
            composable("device_status") { DeviceStatusScreen(navController) }
            composable("emv_reader") { EmvReaderScreen(navController, emvReaderViewModel, nfcViewModel) }
            composable("emv_emulation") { EmvCardEmulationScreen(navController, emvCardEmulationViewModel, nfcViewModel) }
            composable("settings") { SettingsScreen(navController, themeViewModel) }
            composable("about") { V4AboutScreen(navController) }
            composable("legal_mit") { LegalTextScreen(navController, "legacy/mit.txt", "MIT License") }
            composable("legal_cgu") { LegalTextScreen(navController, "legacy/term_of_use.txt", "Terms of Use") }
            composable("legal_mentions") { LegalTextScreen(navController, "legacy/legacy_notice.txt", "Legal Notice") }
        }
    }
}
