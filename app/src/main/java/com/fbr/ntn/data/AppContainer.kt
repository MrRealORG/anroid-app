package com.fbr.ntn.data

import android.content.Context

class AppContainer(context: Context) {
    val settings = SettingsStore(context)
    val repository = FbrRepository(SessionStore(context), context.cacheDir)
}
