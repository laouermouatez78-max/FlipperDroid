package com.example.flipperdroid

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.flipperdroid.ui.FlipperDroidApp

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {
    private val viewModel: AppViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent { FlipperDroidApp(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAll()
        nfcAdapter?.takeIf { it.isEnabled }?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE,
            null
        )
    }

    override fun onPause() {
        viewModel.stopActiveRadios()
        viewModel.cancelNfcWrite()
        runCatching { nfcAdapter?.disableReaderMode(this) }
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        viewModel.onNfcTag(tag)
    }
}
