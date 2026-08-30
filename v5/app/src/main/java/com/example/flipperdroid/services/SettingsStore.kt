package com.example.flipperdroid.services

import android.content.Context
import com.example.flipperdroid.model.Accent
import com.example.flipperdroid.model.AppSettings
import com.example.flipperdroid.model.ThemeMode

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("flipperdroid_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM),
        accent = runCatching {
            Accent.valueOf(prefs.getString("accent", Accent.MINT.name) ?: Accent.MINT.name)
        }.getOrDefault(Accent.MINT),
        privacyMode = prefs.getBoolean("privacy", true)
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString("theme", settings.themeMode.name)
            .putString("accent", settings.accent.name)
            .putBoolean("privacy", settings.privacyMode)
            .apply()
    }
}
