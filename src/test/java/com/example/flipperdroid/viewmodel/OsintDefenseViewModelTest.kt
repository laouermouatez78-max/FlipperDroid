package com.example.flipperdroid.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsintDefenseViewModelTest {

    @Test
    fun cleanHttpsUrl_hasLowLocalScore() {
        val vm = OsintDefenseViewModel()
        vm.analyzeUrlRisk("https://example.org/docs")
        val output = vm.results.value.last().output
        assertTrue(output.contains("Heuristic score: 0/10"))
        assertTrue(output.contains("Query values displayed: false"))
    }

    @Test
    fun suspiciousStructure_accumulatesSignalsWithoutVerdict() {
        val vm = OsintDefenseViewModel()
        vm.analyzeUrlRisk("http://user:pass@127.0.0.1:8080/path?access_token=secret&redirect=https%3A%2F%2Fexample.org")
        val output = vm.results.value.last().output
        assertTrue(output.contains("embedded user-info/credentials"))
        assertTrue(output.contains("literal IP address"))
        assertTrue(output.contains("sensitive-looking query parameter names"))
        assertTrue(output.contains("redirect/navigation parameter names"))
        assertTrue(output.contains("not a malware/phishing verdict"))
        assertFalse(output.contains("access_token=secret"))
    }

    @Test
    fun invalidUrl_returnsErrorResult() {
        val vm = OsintDefenseViewModel()
        vm.analyzeUrlRisk("not a valid host")
        val result = vm.results.value.last()
        assertTrue(result.isError)
    }
}
