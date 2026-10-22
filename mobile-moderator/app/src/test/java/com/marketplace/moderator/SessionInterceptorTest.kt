package com.marketplace.moderator

import com.marketplace.moderator.data.*
import okhttp3.*
import okhttp3.mockwebserver.*
import okhttp3.mockwebserver.Dispatcher
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SessionInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private val tokens = MemoryTokens()

    @Before fun setup() {
        server = MockWebServer().apply { start() }
        val plain = OkHttpClient.Builder().readTimeout(10, TimeUnit.SECONDS).followRedirects(false).build()
        client = plain.newBuilder().addInterceptor(SessionInterceptor(server.url("/api/"), tokens, plain)).build()
    }
    @After fun cleanup() { client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown(); server.shutdown() }

    private fun get() = client.newCall(Request.Builder().url(server.url("/api/auth/me/")).build()).execute()

    @Test fun rotatesTokensAndRetriesOnce() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("""{"access":"new-access","refresh":"new-refresh"}"""))
        server.enqueue(MockResponse().setBody("{}"))
        get().use { assertEquals(200, it.code) }
        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
        val refresh = server.takeRequest()
        assertEquals("/api/auth/login/refresh/", refresh.path)
        assertNull(refresh.getHeader("Authorization"))
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("new-refresh", tokens.read("refresh"))
    }

    @Test fun concurrentExpiredRequestsUseOneRefresh() {
        val refreshes = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/api/auth/login/refresh/" -> {
                    refreshes.incrementAndGet()
                    MockResponse().setBody("""{"access":"new-access","refresh":"new-refresh"}""")
                }
                request.getHeader("Authorization") == "Bearer new-access" -> MockResponse().setBody("{}")
                else -> MockResponse().setResponseCode(401)
            }
        }
        val pool = Executors.newFixedThreadPool(6)
        try {
            val results = (1..6).map { pool.submit<Int> { get().use { it.code } } }
            results.forEach { assertEquals(200, it.get(20, TimeUnit.SECONDS).toInt()) }
            assertEquals(1, refreshes.get())
        } finally { pool.shutdownNow() }
    }

    @Test fun rejectedRefreshClearsSession() {
        server.enqueue(MockResponse().setResponseCode(401)); server.enqueue(MockResponse().setResponseCode(401))
        get().use { assertEquals(401, it.code) }
        assertNull(tokens.read("access")); assertNull(tokens.read("refresh"))
    }

    @Test fun malformedRefreshIsAnIOExceptionAndKeepsCredentials() {
        for (body in listOf("not-json", "{}", """{"access":null}""", """{"access":""}""", """{"access":"bad token"}""")) {
            server.enqueue(MockResponse().setResponseCode(401)); server.enqueue(MockResponse().setBody(body))
            try { get().close(); fail("Expected IOException") } catch (_: IOException) { }
            assertEquals("old-access", tokens.read("access"))
        }
    }

    @Test fun outageDoesNotSignOutTheUser() {
        server.enqueue(MockResponse().setResponseCode(401)); server.enqueue(MockResponse().setResponseCode(503))
        try { get().close(); fail("Expected IOException") } catch (_: IOException) { }
        assertEquals("old-refresh", tokens.read("refresh"))
    }

    @Test fun logoutDuringRefreshDoesNotRestoreTokens() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == "/api/auth/login/refresh/") {
                    tokens.expireTokens()
                    return MockResponse().setBody("""{"access":"stale-access"}""")
                }
                return MockResponse().setResponseCode(401)
            }
        }
        get().use { assertEquals(401, it.code) }
        assertNull(tokens.read("access")); assertEquals(2, server.requestCount)
    }

    @Test fun loginDoesNotSendAnOldToken() {
        server.enqueue(MockResponse().setResponseCode(401))
        client.newCall(Request.Builder().url(server.url("/api/auth/login/")).build()).execute().close()
        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(1, server.requestCount)
    }

    @Test fun foreignServerCannotReceiveTokens() {
        try {
            client.newCall(Request.Builder().url("https://example.invalid/api/auth/me/").build()).execute().close()
            fail("Expected origin rejection")
        } catch (_: IOException) { }
        assertEquals(0, server.requestCount)
    }

    private class MemoryTokens : SessionTokens {
        val values = ConcurrentHashMap(mapOf("access" to "old-access", "refresh" to "old-refresh"))
        @Volatile var revision = 0L
        override fun generation() = revision
        override fun read(name: String) = values[name]
        override fun write(name: String, value: String?) { if (value == null) values.remove(name) else values[name] = value }
        override fun expireTokens() { revision++; values.clear() }
    }
}
