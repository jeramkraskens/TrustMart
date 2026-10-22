package com.marketplace.moderator

import com.marketplace.moderator.data.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RequestContractTest {
    @Test fun passwordChangeIncludesConfirmationRequiredByDjango() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/api/")).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
            server.enqueue(MockResponse().setBody("{}"))
            api.changePassword(passwordChangeFields("OldTest1!", "NewTest2!"))
            val request = server.takeRequest()
            val fields = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("/api/auth/change-password/", request.path)
            assertEquals("OldTest1!", fields["old_password"].asString)
            assertEquals("NewTest2!", fields["new_password"].asString)
            assertEquals(fields["new_password"], fields["password_confirm"])
        } finally { server.shutdown() }
    }
    @Test fun serverAddressSupportsEmulatorAndPhysicalPhone() {
        assertEquals("http://10.0.2.2:8000/api/", normalizedServer(" http://10.0.2.2:8000/api ", true))
        assertEquals("http://192.168.1.20:8000/api/", normalizedServer("http://192.168.1.20:8000/api/", true))
    }
    @Test fun rejectsUnsafeOrInvalidServerAddresses() {
        for (url in listOf("https://user:secret@example.com/api/", "https://example.com/", "https://example.com/api/?token=x", "http://example.com/api/")) {
            try { normalizedServer(url, false); fail("Expected rejection: $url") } catch (_: IllegalArgumentException) { }
        }
    }
}
