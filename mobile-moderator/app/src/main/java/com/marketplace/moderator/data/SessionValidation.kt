// Form checks performed before sending an Android request.
// The server also validates input, so client checks provide feedback rather than replacing backend rules.
package com.marketplace.moderator.data

import com.google.gson.JsonParser
import java.io.IOException

internal data class RefreshedTokens(val access: String, val refresh: String?)

/** Validate the complete response before changing stored credentials. */
internal fun parseRefreshedTokens(body: String): RefreshedTokens = try {
    val json = JsonParser.parseString(body).asJsonObject
    fun token(name: String): String? {
        val value = json[name] ?: return null
        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString || value.asString.isBlank()) {
            throw IOException("Invalid session refresh response. Please retry.")
        }
        if (value.asString.any { it <= ' ' || it >= '\u007f' }) {
            throw IOException("Invalid session refresh token. Please retry.")
        }
        return value.asString
    }
    RefreshedTokens(token("access") ?: throw IOException("Missing refreshed access token. Please retry."), token("refresh"))
} catch (error: Exception) {
    throw if (error is IOException) error else IOException("Invalid session refresh response. Please retry.", error)
}
