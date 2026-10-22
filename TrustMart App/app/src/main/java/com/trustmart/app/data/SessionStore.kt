// Persists the selected server and login-session values on the device.
package com.trustmart.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypt JWTs and checkout recovery state with an Android Keystore key; backups are disabled. */
class SessionStore(context: Context) : SessionTokens {
    @Volatile private var revision = 0L
    override fun generation(): Long = revision
    private val prefs = context.getSharedPreferences("trustmart_session", Context.MODE_PRIVATE)
    private val alias = "trustmart_session_key"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized override fun read(name: String): String? {
        val value = prefs.getString(name, null) ?: return null
        return try {
            val parts = value.split(":")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) { prefs.edit().remove(name).apply(); null }
    }
    @Synchronized override fun write(name: String, value: String?) {
        if (value == null) { prefs.edit().remove(name).apply(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val body = Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        prefs.edit().putString(name, "$iv:$body").apply()
    }
    @Synchronized fun start(access: String, refresh: String) {
        revision++
        write("access", access); write("refresh", refresh)
    }
    @Synchronized fun clear() { revision++; prefs.edit().remove("access").remove("refresh").remove("checkout").apply() }
    @Synchronized override fun expireTokens() { revision++; prefs.edit().remove("access").remove("refresh").apply() }
    var server: String
        get() = prefs.getString("server", "http://10.0.2.2:8000/api/")!!
        set(value) { prefs.edit().putString("server", value).apply() }
}
