package com.example.flipperdroid.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkToolsViewModelTest {
    private val viewModel = NetworkToolsViewModel()

    @Test
    fun normalizeHost_supportsDomainIpv4AndUrl() {
        assertEquals("example.org", viewModel.normalizeHost("example.org"))
        assertEquals("192.168.1.1", viewModel.normalizeHost("192.168.1.1"))
        assertEquals("example.org", viewModel.normalizeHost("https://example.org/path?q=1"))
    }

    @Test
    fun normalizeHost_preservesRawIpv6InsteadOfTruncatingAtColon() {
        assertEquals("2001:db8::1", viewModel.normalizeHost("2001:db8::1"))
        assertEquals("2001:db8::1", viewModel.normalizeHost("[2001:db8::1]"))
    }

    @Test
    fun normalizeHost_rejectsWhitespaceAndEmptyInput() {
        assertNull(viewModel.normalizeHost(""))
        assertNull(viewModel.normalizeHost("bad host name"))
        assertNull(viewModel.normalizeHost("host\nother"))
    }
}
