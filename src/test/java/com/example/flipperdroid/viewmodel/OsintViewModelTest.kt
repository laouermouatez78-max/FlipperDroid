package com.example.flipperdroid.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsintViewModelTest {

    @Test
    fun identityInspectorBuildsPublicHandleVariantsAndLinks() {
        val viewModel = OsintViewModel()

        viewModel.inspectIdentity(
            firstName = "Alex",
            lastName = "Martin",
            handle = "alex_dev",
            publicEmail = ""
        )

        val result = viewModel.results.value.last()
        assertFalse(result.isError)
        assertTrue(result.output.contains("alexmartin"))
        assertTrue(result.output.contains("alex.dev"))
        assertTrue(result.links.any { it.label == "GitHub" && it.url.endsWith("/alex_dev") })
        assertTrue(result.links.any { it.label.contains("Google") })
    }

    @Test
    fun domainInspectorParsesNormalHostnameAndAddsPassiveSources() {
        val viewModel = OsintViewModel()

        viewModel.inspectDomain("https://sub.example.org/path")

        val result = viewModel.results.value.last()
        assertFalse(result.isError)
        assertTrue(result.output.contains("Host: sub.example.org"))
        assertTrue(result.output.contains("TLD / suffix hint: .org"))
        assertTrue(result.links.any { it.label.contains("RDAP") })
        assertTrue(result.links.any { it.label.contains("Certificate Transparency") })
    }

    @Test
    fun urlInspectorReportsQueryNamesWithoutValues() {
        val viewModel = OsintViewModel()

        viewModel.inspectUrl("https://example.org/path?utm_source=test&id=12345#section")

        val result = viewModel.results.value.last()
        assertFalse(result.isError)
        assertTrue(result.output.contains("Query present: true"))
        assertTrue(result.output.contains("utm_source"))
        assertTrue(result.output.contains("id"))
        assertFalse(result.output.contains("12345"))
    }

    @Test
    fun urlInspectorRejectsNonHttpSchemes() {
        val viewModel = OsintViewModel()

        viewModel.inspectUrl("ftp://example.org/file")

        val result = viewModel.results.value.last()
        assertTrue(result.isError)
    }
}
