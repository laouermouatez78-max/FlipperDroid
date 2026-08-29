package com.example.flipperdroid

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
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
import com.example.flipperdroid.view.HomeScreen
import com.example.flipperdroid.view.InfraredScreen
import com.example.flipperdroid.view.LegalTextScreen
import com.example.flipperdroid.view.NetworkToolsScreen
import com.example.flipperdroid.view.NfcScreen
import com.example.flipperdroid.view.PasswordGeneratorScreen
import com.example.flipperdroid.view.SettingsScreen
import com.example.flipperdroid.view.V2AboutScreen
import com.example.flipperdroid.viewmodel.NetworkToolsViewModel
import com.example.flipperdroid.viewmodel.NfcViewModel
import com.example.flipperdroid.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val nfcViewModel: NfcViewModel by viewModels()
    private val networkToolsViewModel: NetworkToolsViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFiltersArray: Array<IntentFilter> = emptyArray()
    private var techListsArray: Array<Array<String>> = emptyArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureNfc()

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

        val launchIntent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val mutabilityFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }

        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
        )

        intentFiltersArray = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        )

        techListsArray = arrayOf(
            arrayOf(
                NfcA::class.java.name,
                NfcB::class.java.name,
                NfcF::class.java.name,
                NfcV::class.java.name
            )
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
            intent.getParcelableCompat(NfcAdapter.EXTRA_TAG, Tag::class.java)
                ?.let(nfcViewModel::onTagScanned)
        }
    }

    private fun <T : Parcelable> Intent.getParcelableCompat(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            (getParcelableExtra<Parcelable>(key) as? T)
        }
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter ?: return
        val foregroundIntent = pendingIntent ?: return

        if (adapter.isEnabled) {
            adapter.enableForegroundDispatch(
                this,
                foregroundIntent,
                intentFiltersArray,
                techListsArray
            )
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    @SuppressLint("ComposableDestinationInComposeScope")
    @Composable
    private fun AppNavigation(themeViewModel: ThemeViewModel) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(navController = navController, nfcViewModel = nfcViewModel)
            }
            composable("nfc") {
                NfcScreen(navController = navController, nfcViewModel = nfcViewModel)
            }
            composable("network") {
                NetworkToolsScreen(navController = navController, viewModel = networkToolsViewModel)
            }
            composable("ir") {
                InfraredScreen(navController = navController)
            }
            composable("password_generator") {
                PasswordGeneratorScreen(navController = navController)
            }
            composable("settings") {
                SettingsScreen(navController = navController, themeViewModel = themeViewModel)
            }
            composable("about") {
                V2AboutScreen(navController = navController)
            }
            composable("legal_mit") {
                LegalTextScreen(
                    navController = navController,
                    assetPath = "legacy/mit.txt",
                    title = "MIT License"
                )
            }
            composable("legal_cgu") {
                LegalTextScreen(
                    navController = navController,
                    assetPath = "legacy/term_of_use.txt",
                    title = "Terms of Use"
                )
            }
            composable("legal_mentions") {
                LegalTextScreen(
                    navController = navController,
                    assetPath = "legacy/legacy_notice.txt",
                    title = "Legal Notice"
                )
            }
        }
    }
}
