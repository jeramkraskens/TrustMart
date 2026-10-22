// Stores and retrieves login tokens using Android DataStore.
// Other layers use this class instead of reading token storage directly.
package com.marketplace.moderator.data

/** Persists the access/refresh pair in app-private DataStore. Clear both tokens on logout; DataStore is not encrypted storage. */

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.marketplace.moderator.BuildConfig

private val Context.dataStore by preferencesDataStore(name = "moderator_prefs")

class TokenManager(private val context: Context) {
    private val connection = context.getSharedPreferences("moderator_connection", Context.MODE_PRIVATE)
    var server: String
        get() = connection.getString("server", BuildConfig.API_BASE_URL) ?: BuildConfig.API_BASE_URL
        set(value) { connection.edit().putString("server", value).apply() }

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val GENERATION = longPreferencesKey("auth_generation")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }

    suspend fun getAccessToken(): String? = accessTokenFlow.first()
    suspend fun getRefreshToken(): String? = refreshTokenFlow.first()
    suspend fun generation(): Long = context.dataStore.data.first()[GENERATION] ?: 0L
    suspend fun saveRefreshed(expected: Long, access: String, refresh: String?): Boolean {
        var saved = false
        context.dataStore.edit {
            if ((it[GENERATION] ?: 0L) == expected) {
                it[ACCESS_TOKEN] = access
                if (refresh != null) it[REFRESH_TOKEN] = refresh
                saved = true
            }
        }
        return saved
    }
    suspend fun expireIfGeneration(expected: Long) {
        context.dataStore.edit {
            if ((it[GENERATION] ?: 0L) == expected) {
                it.remove(ACCESS_TOKEN); it.remove(REFRESH_TOKEN)
                it[GENERATION] = expected + 1
            }
        }
    }

    suspend fun saveTokens(access: String, refresh: String) {
        context.dataStore.edit {
            it[GENERATION] = (it[GENERATION] ?: 0L) + 1
            it[ACCESS_TOKEN] = access
            it[REFRESH_TOKEN] = refresh
        }
    }

    suspend fun saveAccessToken(access: String) {
        context.dataStore.edit { it[ACCESS_TOKEN] = access }
    }

    suspend fun saveRefreshToken(refresh: String) {
        context.dataStore.edit { it[REFRESH_TOKEN] = refresh }
    }

    suspend fun clear() {
        context.dataStore.edit {
            val next = (it[GENERATION] ?: 0L) + 1
            it.clear()
            it[GENERATION] = next
        }
    }
}
