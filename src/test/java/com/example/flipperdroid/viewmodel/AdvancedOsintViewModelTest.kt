package com.example.flipperdroid.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedOsintViewModelTest {

    @Test
    fun domainSimilarity_generatesLocalCandidates() {
        val vm = AdvancedOsintViewModel()
        vm.generateDomainVariants("example.org")
        val output = vm.results.value.last().output
        assertTrue(output.contains("Reference: example.org"))
        assertTrue(output.contains("exmple.org") || output.contains("exampl.org"))
        assertTrue(output.contains("No registration or availability checks were performed"))
    }

    @Test
    fun indicatorExtractor_findsCommonArtifacts() {
        val vm = AdvancedOsintViewModel()
        val sha = "a".repeat(64)
        vm.extractIndicators(
            "Visit https://example.org/path and contact analyst@example.net from 8.8.8.8 hash=$sha"
        )
        val output = vm.results.value.last().output
        assertTrue(output.contains("https://example.org/path"))
        assertTrue(output.contains("analyst@example.net"))
        assertTrue(output.contains("8.8.8.8"))
        assertTrue(output.contains(sha))
    }

    @Test
    fun emailHeaderAnalyzer_reportsAuthenticationHints() {
        val vm = AdvancedOsintViewModel()
        vm.analyzeEmailHeaders(
            """
            From: Example <sender@example.org>
            Reply-To: reply@example.org
            Return-Path: <bounce@example.org>
            Message-ID: <1234@example.org>
            Received: from mail.example.org (8.8.8.8) by mx.example.net;
            Authentication-Results: mx.example.net; spf=pass smtp.mailfrom=example.org; dkim=pass; dmarc=pass
            """.trimIndent()
        )
        val output = vm.results.value.last().output
        assertTrue(output.contains("SPF result hint: pass"))
        assertTrue(output.contains("DKIM result hint: pass"))
        assertTrue(output.contains("DMARC result hint: pass"))
        assertTrue(output.contains("Message-ID domain: example.org"))
    }

    @Test
    fun fingerprint_doesNotEchoSecretsBeyondInputPurpose() {
        val vm = AdvancedOsintViewModel()
        vm.fingerprintText("hello")
        val output = vm.results.value.last().output
        assertTrue(output.contains("SHA-256:"))
        assertTrue(output.contains("Bytes (UTF-8): 5"))
        assertFalse(output.contains("password="))
    }
}
