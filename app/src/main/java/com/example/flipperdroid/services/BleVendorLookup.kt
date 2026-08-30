package com.example.flipperdroid.services

/**
 * Offline lookup table for BLE "Company Identifiers" (manufacturer-specific data,
 * AD type 0xFF) as assigned by the Bluetooth SIG. Intentionally local/static —
 * no network call, no telemetry, works fully offline like the rest of the app.
 *
 * This list covers the most commonly seen IDs in the field; unknown IDs fall back
 * to a hex label so the UI never breaks on an unrecognized manufacturer.
 */
object BleVendorLookup {
    private val ids: Map<Int, String> = mapOf(
        0x004C to "Apple",
        0x0006 to "Microsoft",
        0x00E0 to "Google",
        0x0075 to "Samsung Electronics",
        0x038F to "Xiaomi",
        0x0157 to "Anhui Huami (Amazfit)",
        0x0171 to "Amazon",
        0x00D2 to "AbTemp",
        0x004F to "Nordic Semiconductor ASA",
        0x0002 to "Intel",
        0x000F to "Broadcom",
        0x0087 to "Garmin International",
        0x0059 to "Nordic Semiconductor",
        0x0131 to "Tile, Inc.",
        0x0499 to "Ruuvi Innovations",
        0x0165 to "Withings",
        0x0BA6 to "Ubiquiti Networks",
        0x02E1 to "Sonos",
        0x01D9 to "GN Hearing / Jabra",
        0x00C4 to "LG Electronics",
        0x0078 to "Bose",
        0x030E to "Anker Innovations",
        0x0303 to "Beats Electronics",
        0x0022 to "Wicentric",
        0x02D0 to "Skullcandy",
        0x0053 to "Nike",
        0x00E5 to "Nintendo",
        0x0143 to "Fitbit",
        0x0184 to "Flic",
        0x0301 to "Wyze Labs",
        0x0409 to "TP-Link Systems",
        0x08E3 to "Espressif Systems",
        0x0330 to "Eve Systems",
        0x0503 to "Govee",
        0x0745 to "Aqara / Lumi United",
        0x055D to "Meta Platforms (Oculus)",
        0x000D to "Texas Instruments",
        0x0030 to "STMicroelectronics"
    )

    /** Returns a human-readable vendor label, falling back to a hex identifier. */
    fun name(companyId: Int?): String? {
        if (companyId == null) return null
        return ids[companyId] ?: "Fabricant 0x%04X".format(companyId)
    }

    /**
     * Whether a BLE address is a "random" (privately generated/rotating) address,
     * as opposed to a fixed public IEEE MAC. Detected via the two most-significant
     * bits of the first address octet, per the Bluetooth Core Spec address rules:
     * 11 = random static/resolvable/non-resolvable, 00 = public.
     * Devices using randomized/rotating addresses are harder to track long-term —
     * useful context when reviewing scan results for privacy/tracking analysis.
     */
    fun isLikelyRandomAddress(address: String): Boolean {
        val firstOctet = address.split(":").firstOrNull()?.toIntOrNull(16) ?: return false
        val topTwoBits = (firstOctet shr 6) and 0b11
        return topTwoBits == 0b11
    }
}
