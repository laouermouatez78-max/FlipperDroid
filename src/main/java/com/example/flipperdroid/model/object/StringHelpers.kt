package com.example.flipperdroid.model.`object`

object StringHelpers {
    fun decodeHex(string: String): ByteArray {
        check(string.length % 2 == 0) { "Must have an even length" }
        return string.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
    fun intToHexString(input: Int): String {
        return String.format("%02x", input)
    }
}