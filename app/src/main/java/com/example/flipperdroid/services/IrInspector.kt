package com.example.flipperdroid.services

import android.content.Context
import android.hardware.ConsumerIrManager
import com.example.flipperdroid.model.CarrierRange
import com.example.flipperdroid.model.IrInfo
import com.example.flipperdroid.model.IrTransmitResult

/**
 * IR diagnostic + a deliberately bounded "lab" transmit function, following the
 * same controlled-demonstrator philosophy as [BleLabBeacon]: no preloaded remote
 * code database, no replay of captured signals, hard caps on pattern length and
 * total on-air time. It exists to verify the emitter physically works and to let
 * a user test a *frequency + pattern they explicitly typed in* — not to operate
 * arbitrary third-party equipment automatically.
 */
class IrInspector(context: Context) {
    companion object {
        private const val MAX_PATTERN_ENTRIES = 32
        private const val MAX_TOTAL_MICROS = 500_000 // 0.5 s hard ceiling per burst
        private const val MIN_FREQUENCY_HZ = 20_000
        private const val MAX_FREQUENCY_HZ = 60_000
    }

    private val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun info(): IrInfo {
        val hasEmitter = ir?.hasIrEmitter() == true
        val ranges = if (hasEmitter) {
            runCatching {
                ir?.carrierFrequencies?.map { CarrierRange(it.minFrequency, it.maxFrequency) }.orEmpty()
            }.getOrDefault(emptyList())
        } else emptyList()
        return IrInfo(hasEmitter, ranges)
    }

    /**
     * Transmits a short, explicitly user-defined pattern at a chosen carrier frequency.
     * `pattern` is a sequence of on/off durations in microseconds (must start with "on"),
     * exactly as Android's ConsumerIrManager expects. Bounded to a short single burst.
     */
    fun transmitTest(frequencyHz: Int, pattern: IntArray): IrTransmitResult {
        val manager = ir ?: return IrTransmitResult(false, "Émetteur IR indisponible sur cet appareil", frequencyHz)
        if (!manager.hasIrEmitter()) {
            return IrTransmitResult(false, "Aucun émetteur IR matériel détecté", frequencyHz)
        }
        if (frequencyHz !in MIN_FREQUENCY_HZ..MAX_FREQUENCY_HZ) {
            return IrTransmitResult(false, "Fréquence hors plage raisonnable (20–60 kHz)", frequencyHz)
        }
        val supported = info().carrierRanges
        if (supported.isNotEmpty() && supported.none { it.contains(frequencyHz) }) {
            return IrTransmitResult(false, "Fréquence non annoncée comme supportée par ce matériel", frequencyHz)
        }
        if (pattern.isEmpty() || pattern.size > MAX_PATTERN_ENTRIES) {
            return IrTransmitResult(false, "Motif invalide (1 à $MAX_PATTERN_ENTRIES segments)", frequencyHz)
        }
        if (pattern.any { it <= 0 }) {
            return IrTransmitResult(false, "Chaque segment doit être une durée positive (µs)", frequencyHz)
        }
        val total = pattern.sum()
        if (total > MAX_TOTAL_MICROS) {
            return IrTransmitResult(false, "Motif trop long (limite ${MAX_TOTAL_MICROS / 1000} ms au total)", frequencyHz)
        }
        return runCatching {
            manager.transmit(frequencyHz, pattern)
            IrTransmitResult(true, "Impulsion de test émise à ${frequencyHz / 1000} kHz", frequencyHz)
        }.getOrElse {
            IrTransmitResult(false, "Échec d'émission: ${it.message ?: it.javaClass.simpleName}", frequencyHz)
        }
    }
}
