package com.fbr.ntn.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fbr.ntn.model.Session

class SessionStore(context: Context) {
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun read(): Session? {
        val token = prefs.getString("token", null) ?: return null
        val expiry = prefs.getLong("expiry", 0)
        return Session(
            token = token,
            expiresAtEpochSeconds = expiry,
            ntn = prefs.getString("ntn", null),
            displayName = prefs.getString("name", null),
            maskedMobile = prefs.getString("mobile", null)
        ).takeIf { expiry > System.currentTimeMillis() / 1000 }
    }

    fun save(session: Session) = prefs.edit()
        .putString("token", session.token)
        .putLong("expiry", session.expiresAtEpochSeconds)
        .putString("ntn", session.ntn)
        .putString("name", session.displayName)
        .putString("mobile", session.maskedMobile)
        .apply()
    fun clear() = prefs.edit().clear().apply()
}
