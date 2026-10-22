package com.marketplace.moderator.data

import android.content.Context
import com.marketplace.moderator.BuildConfig
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** One refresh at a time; rejected sessions return the app to sign-in. */
object RetrofitClient {
    @Volatile private var apiService: ApiService? = null
    @Volatile private var serviceBase: String? = null

    fun getApiService(context: Context): ApiService {
        val base = TokenManager(context).server
        return synchronized(this) {
            if (serviceBase != base || apiService == null) {
                apiService = build(context.applicationContext, base)
                serviceBase = base
            }
            apiService!!
        }
    }

    private fun build(context: Context, base: String): ApiService {
        val manager = TokenManager(context)
        val tokens = object : SessionTokens {
            override fun generation() = runBlocking { manager.generation() }
            override fun read(name: String) = runBlocking {
                if (name == "access") manager.getAccessToken() else manager.getRefreshToken()
            }
            override fun write(name: String, value: String?) = runBlocking {
                if (value != null) {
                    if (name == "access") manager.saveAccessToken(value) else manager.saveRefreshToken(value)
                }
            }
            override fun expireTokens() { runBlocking { manager.clear() } }
            override fun expireIfGeneration(expected: Long) { runBlocking { manager.expireIfGeneration(expected) } }
            override fun saveRefreshed(expected: Long, access: String, refresh: String?) = runBlocking {
                manager.saveRefreshed(expected, access, refresh)
            }
        }
        val plain = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
        val client = plain.newBuilder()
            .addInterceptor(SessionInterceptor(base.toHttpUrl(), tokens, plain))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }).build()
        return Retrofit.Builder().baseUrl(base).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
    }
}
