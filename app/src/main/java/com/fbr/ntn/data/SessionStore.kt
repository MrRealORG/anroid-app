package com.fbr.ntn.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fbr.ntn.model.Session

/**
 * Session storage with encrypted-first strategy and plain fallback.
 *
 * EncryptedSharedPreferences depends on the Android Keystore, which is broken or
 * unavailable on some devices (custom ROMs, work profiles, outdated security
 * providers). Previously a Keystore failure crashed or silently lost the session.
 * Now: try encrypted, fall back to private plain prefs so login state still saves.
 */
class SessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                appContext,
                "secure_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            // Fallback for devices where the Keystore/encrypted prefs fail.
            appContext.getSharedPreferences("session_fallback", Context.MODE_PRIVATE)
        }
    }

    fun read(): Session? = runCatching {
        val token = prefs.getString("token", null) ?: return null
        val expiry = prefs.getLong("expiry", 0)
        Session(
            token = token,
            expiresAtEpochSeconds = expiry,
            ntn = prefs.getString("ntn", null),
            displayName = prefs.getString("name", null),
            maskedEmail = prefs.getString("email", null),
            apiUrl = prefs.getString("api_url", null),
            username = prefs.getString("username", null)
        ).takeIf { expiry > System.currentTimeMillis() / 1000 }
    }.getOrNull()

    /** Saves a session without wiping previously stored fields that are null here
     *  (e.g. web login must not erase the workspace connection). */
    fun save(session: Session) = runCatching {
        val e = prefs.edit()
            .putString("token", session.token)
            .putLong("expiry", session.expiresAtEpochSeconds)
        session.ntn?.let { e.putString("ntn", it) }
        session.displayName?.let { e.putString("name", it) }
        session.maskedEmail?.let { e.putString("email", it) }
        session.apiUrl?.let { e.putString("api_url", it) }
        session.username?.let { e.putString("username", it) }
        e.apply()
    }

    fun saveConnection(apiUrl: String, username: String) = runCatching {
        prefs.edit()
            .putString("api_url", apiUrl)
            .putString("username", username)
            .apply()
    }

    fun clear() = runCatching { prefs.edit().clear().apply() }
}
