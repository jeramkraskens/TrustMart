package com.marketplace.moderator.data

import okhttp3.HttpUrl.Companion.toHttpUrl

fun normalizedServer(input: String, allowHttp: Boolean): String {
    val url = input.trim().trimEnd('/').toHttpUrl()
    require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { "Use a server URL without credentials, query or fragment." }
    require(allowHttp || url.isHttps) { "Release builds require HTTPS." }
    require(url.encodedPath.trimEnd('/').endsWith("/api")) { "The backend URL must end with /api/." }
    return url.toString().trimEnd('/') + "/"
}

/** Keep this payload aligned with Django's password-change validation. */
fun passwordChangeFields(old: String, new: String): Map<String, String> = mapOf(
    "old_password" to old, "new_password" to new, "password_confirm" to new,
)
