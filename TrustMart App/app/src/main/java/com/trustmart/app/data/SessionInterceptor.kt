// HTTP middleware for adding session credentials and handling expired access tokens.
package com.trustmart.app.data

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

interface SessionTokens {
    fun generation(): Long = 0
    fun read(name: String): String?
    fun write(name: String, value: String?)
    fun expireTokens()
    fun expireIfGeneration(expected: Long) = synchronized(this) {
        if (generation() == expected) expireTokens()
    }
    fun saveRefreshed(expected: Long, access: String, refresh: String?): Boolean = synchronized(this) {
        if (generation() != expected) return@synchronized false
        write("access", access)
        refresh?.let { write("refresh", it) }
        true
    }
}

/** Keep refresh failures inside OkHttp's IOException contract so async requests cannot crash the app. */
internal class SessionInterceptor(
    private val base: HttpUrl,
    private val session: SessionTokens,
    private val plain: OkHttpClient,
) : Interceptor {
    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val generation = session.generation()
        val original = chain.request()
        if (original.url.host != base.host || original.url.port != base.port || original.url.scheme != base.scheme) {
            throw IOException("Refusing to send credentials to a different server.")
        }
        val isAuth = original.url.encodedPath == base.resolve("auth/login/")!!.encodedPath ||
            original.url.encodedPath == base.resolve("auth/register/")!!.encodedPath
        val token = if (isAuth) null else session.read("access")
        val request = original.newBuilder().apply {
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        val response = chain.proceed(request)
        if (response.code != 401 || token == null || isAuth) return response

        val next = try {
            synchronized(refreshLock) {
                if (session.generation() != generation) return@synchronized null
                val current = session.read("access")
                if (current != token) current else refreshToken(generation)
            }
        } catch (failure: Exception) {
            // The original 401 also owns a socket; close it even when refresh fails.
            response.close()
            throw if (failure is IOException) failure else IOException("Session refresh failed. Please retry.", failure)
        }
        if (next == null || session.generation() != generation) return response
        response.close()
        return chain.proceed(original.newBuilder().header("Authorization", "Bearer $next").build())
    }

    private fun refreshToken(generation: Long): String? {
        val refresh = session.read("refresh") ?: return null
        val body = apiGson.toJson(mapOf("refresh" to refresh)).toRequestBody("application/json".toMediaType())
        plain.newCall(Request.Builder().url(base.resolve("auth/login/refresh/")!!).post(body).build()).execute().use { res ->
            if (session.generation() != generation) return null
            if (res.code == 401 || res.code == 400) { session.expireIfGeneration(generation); return null }
            if (!res.isSuccessful) throw IOException("Session refresh failed. Please retry.")
            val tokens = parseRefreshedTokens(res.body?.string().orEmpty())
            if (!session.saveRefreshed(generation, tokens.access, tokens.refresh)) return null
            return tokens.access
        }
    }
}
