package com.trustmart.app

import com.trustmart.app.data.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Exercise Retrofit's asynchronous path, where a non-IOException in an interceptor can be fatal. */
class SessionInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var plain: OkHttpClient
    private lateinit var api: Api
    private val tokens = MemoryTokens()

    @Before fun setup() {
        server = MockWebServer().apply { start() }
        plain = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).followRedirects(false).build()
        client = plain.newBuilder().addInterceptor(SessionInterceptor(server.url("/api/"), tokens, plain)).build()
        api = Retrofit.Builder().baseUrl(server.url("/api/")).client(client)
            .addConverterFactory(GsonConverterFactory.create(apiGson)).build().create(Api::class.java)
    }

    @After fun cleanup() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        plain.dispatcher.executorService.shutdown()
        server.shutdown()
    }

    @Test fun rotatesTokensAndRetriesOriginalRequest() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("""{"access":"new-access","refresh":"new-refresh"}"""))
        server.enqueue(MockResponse().setBody("{}"))
        assertTrue(api.get("auth/me/").isSuccessful)
        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
        val refresh = server.takeRequest()
        assertEquals("/api/auth/login/refresh/", refresh.path)
        assertNull(refresh.getHeader("Authorization"))
        assertEquals("""{"refresh":"old-refresh"}""", refresh.body.readUtf8())
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("new-refresh", tokens.read("refresh"))
    }

    @Test fun malformedRefreshFailsSafelyAndPreservesCredentials() = runBlocking {
        listOf("not-json", "[]", "{}", """{"access":null}""", """{"access":7}""",
            """{"access":""}""", """{"access":"new-access","refresh":{}}""").forEach { body ->
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(MockResponse().setBody(body))
            try {
                api.get("auth/me/")
                fail("Malformed refresh must fail as an IOException")
            } catch (_: IOException) { }
            assertEquals("old-access", tokens.read("access"))
            assertEquals("old-refresh", tokens.read("refresh"))
        }
    }

    @Test fun rejectedRefreshExpiresCredentials() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        assertEquals(401, api.get("auth/me/").code())
        assertNull(tokens.read("access"))
        assertNull(tokens.read("refresh"))
    }

    @Test fun unavailableRefreshPreservesCredentials() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(503))
        try {
            api.get("auth/me/")
            fail("Expected refresh failure")
        } catch (_: IOException) { }
        assertEquals("old-access", tokens.read("access"))
        assertEquals("old-refresh", tokens.read("refresh"))
    }

    @Test fun loginDoesNotAttachOrRefreshExistingCredentials() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        assertEquals(401, api.post("auth/login/", emptyMap()).code())
        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(1, server.requestCount)
        assertEquals("old-access", tokens.read("access"))
    }

    @Test fun foreignOriginIsRejectedBeforeSendingAnyCredentials() = runBlocking {
        try {
            api.get("https://example.invalid/api/auth/me/")
            fail("Expected origin rejection")
        } catch (_: IOException) { }
        assertEquals(0, server.requestCount)
    }

    @Test fun logoutDuringRefreshCannotRestoreTheOldSession() = runBlocking {
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                if (request.path == "/api/auth/login/refresh/") {
                    tokens.expireTokens()
                    return MockResponse().setBody("""{"access":"stale-access","refresh":"stale-refresh"}""")
                }
                return MockResponse().setResponseCode(401)
            }
        }
        assertEquals(401, api.get("auth/me/").code())
        assertNull(tokens.read("access"))
        assertEquals(2, server.requestCount)
    }

    @Test fun aNewLoginDuringRefreshIsNeverOverwritten() = runBlocking {
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                if (request.path == "/api/auth/login/refresh/") {
                    tokens.replaceSession()
                    return MockResponse().setBody("""{"access":"stale-access","refresh":"stale-refresh"}""")
                }
                return MockResponse().setResponseCode(401)
            }
        }
        assertEquals(401, api.get("auth/me/").code())
        assertEquals("other-access", tokens.read("access"))
        assertEquals(2, server.requestCount)
    }

    private class MemoryTokens : SessionTokens {
        private val values = java.util.concurrent.ConcurrentHashMap(mapOf("access" to "old-access", "refresh" to "old-refresh"))
        @Volatile private var revision = 0L
        override fun generation() = revision
        override fun read(name: String) = values[name]
        override fun write(name: String, value: String?) { if (value == null) values.remove(name) else values[name] = value }
        override fun expireTokens() { revision++; values.clear() }
        fun replaceSession() { revision++; values["access"] = "other-access"; values["refresh"] = "other-refresh" }
    }
}
