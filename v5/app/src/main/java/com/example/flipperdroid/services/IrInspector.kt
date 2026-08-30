package com.example.flipperdroid.services

import android.content.Context
import android.hardware.ConsumerIrManager
import com.example.flipperdroid.model.IrInfo

class IrInspector(context: Context) {
    private val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun info(): IrInfo {
        val hasEmitter = ir?.hasIrEmitter() == true
        val ranges = if (hasEmitter) {
            runCatching {
                ir?.carrierFrequencies?.map { "${it.minFrequency / 1000}–${it.maxFrequency / 1000} kHz" }.orEmpty()
            }.getOrDefault(emptyList())
        } else emptyList()
        return IrInfo(hasEmitter, ranges)
    }
}
