package com.example.flipperdroid.services

/**
 * Offline VID/PID catalog for commonly used external radio/security dongles
 * (SDR receivers, Sub-GHz transceivers). Android phones have no native
 * Sub-GHz radio, so the honest way to support 315/433/868/915 MHz work is to
 * detect a recognized USB-OTG dongle rather than fake a capability the
 * hardware doesn't have.
 */
object UsbDeviceCatalog {
    private data class Entry(val label: String, val sdrCapable: Boolean)

    private val catalog: Map<Pair<Int, Int>, Entry> = mapOf(
        (0x0BDA to 0x2838) to Entry("RTL-SDR (RTL2832U) — récepteur SDR large bande", true),
        (0x1D50 to 0x6089) to Entry("HackRF One — émetteur/récepteur SDR", true),
        (0x1D50 to 0x604B) to Entry("HackRF (bootloader) — SDR", true),
        (0x0483 to 0xA2D2) to Entry("YARD Stick One (CC1101) — Sub-GHz", true),
        (0x1209 to 0x7331) to Entry("LimeSDR Mini — SDR", true),
        (0x2CBA to 0x0002) to Entry("PlutoSDR (ADALM-PLUTO) — SDR", true),
        (0x0403 to 0x6001) to Entry("FTDI FT232 — pont série (souvent utilisé avec CC1101/nRF24)", false),
        (0x10C4 to 0xEA60) to Entry("Silicon Labs CP2102 — pont série USB", false),
        (0x1A86 to 0x7523) to Entry("CH340 — pont série USB", false),
        (0x0403 to 0x6015) to Entry("FTDI FT230X — pont série", false)
    )

    fun lookup(vendorId: Int, productId: Int): Pair<String?, Boolean> {
        val entry = catalog[vendorId to productId] ?: return null to false
        return entry.label to entry.sdrCapable
    }
}
