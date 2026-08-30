package com.example.flipperdroid.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiSecurityClassificationTest {
    private val viewModel = WifiDeautherViewModel()

    @Test
    fun classifiesEnhancedOpenAndLegacyModes() {
        assertEquals(SecurityType.OWE, viewModel.getSecurityType("[OWE_TRANSITION][ESS]"))
        assertEquals(SecurityType.WEP, viewModel.getSecurityType("[WEP][ESS]"))
        assertEquals(SecurityType.WPA, viewModel.getSecurityType("[WPA-PSK-CCMP][ESS]"))
    }

    @Test
    fun distinguishesPureWpa3FromPskTransition() {
        assertEquals(SecurityType.WPA3, viewModel.getSecurityType("[RSN-SAE-CCMP][ESS]"))
        assertEquals(SecurityType.WPA2_WPA3, viewModel.getSecurityType("[RSN-PSK+SAE-CCMP][ESS]"))
        assertEquals(SecurityType.WPA2, viewModel.getSecurityType("[RSN-PSK-CCMP][ESS]"))
    }

    @Test
    fun mapsCommonWifiBandsAndChannels() {
        assertEquals(1, WifiNetwork.frequencyToChannel(2412))
        assertEquals("2.4 GHz", WifiNetwork.frequencyBand(2412))
        assertEquals(36, WifiNetwork.frequencyToChannel(5180))
        assertEquals("5 GHz", WifiNetwork.frequencyBand(5180))
        assertEquals(1, WifiNetwork.frequencyToChannel(5955))
        assertEquals("6 GHz", WifiNetwork.frequencyBand(5955))
    }
}
