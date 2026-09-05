package com.fbr.ntn.data

import android.content.Context
import android.content.SharedPreferences

/** Simple app settings (theme, sounds). Plain prefs with failure-safe access —
 *  settings must never crash or block the app, unlike the encrypted session. */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }.getOrNull()

    var themeMode: String
        get() = runCatching { prefs?.getString("theme_mode", "system") ?: "system" }.getOrDefault("system")
        set(value) { runCatching { prefs?.edit()?.putString("theme_mode", value)?.apply() } }

    var soundsEnabled: Boolean
        get() = runCatching { prefs?.getBoolean("sounds", false) ?: false }.getOrDefault(false)
        set(value) { runCatching { prefs?.edit()?.putBoolean("sounds", value)?.apply() } }
}
