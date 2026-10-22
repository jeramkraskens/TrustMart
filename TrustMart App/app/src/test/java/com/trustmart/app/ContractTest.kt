package com.trustmart.app

import com.trustmart.app.data.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Fast JVM checks cover API wire contracts and role/money-action eligibility without changing real data. */
class ContractTest {
    @Test fun `password change includes backend confirmation field`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/api/")).addConverterFactory(GsonConverterFactory.create(apiGson)).build().create(Api::class.java)
            server.enqueue(MockResponse().setBody("{}"))
            api.post("auth/change-password/", passwordChangeFields("OldTest1!", "NewTest2!"))
            val request = server.takeRequest()
            val fields = com.google.gson.JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("/api/auth/change-password/", request.path)
            assertEquals("OldTest1!", fields["old_password"].asString)
            assertEquals("NewTest2!", fields["new_password"].asString)
            assertEquals(fields["new_password"], fields["password_confirm"])
        } finally { server.shutdown() }
    }
    private val buyer = User(1, "buyer", "test@example.com", "buyer", "", "", "", 100)
    private val seller = buyer.copy(id = 2, role = "seller")
    private val item = Listing(7, 2, "seller", 100, 1, "Phones", "Phone", "Sample", "78000.00", "good", "verified", "Colombo", emptyList())
    private val order = Order(1, 1, "buyer", 7, "Phone", 2, "seller", "78000.00", "74100.00", "held", "card", false, "2026-09-25")

    @Test fun `only buyer can purchase another user's verified listing`() {
        assertTrue(canBuy(item, buyer)); assertFalse(canBuy(item, seller)); assertFalse(canBuy(item, null))
        assertFalse(canBuy(item.copy(status = "sold"), buyer)); assertFalse(canBuy(item.copy(seller = 1), buyer))
    }
    @Test fun `delivery confirmation requires buyer held funds and no dispute`() {
        assertTrue(canConfirm(order, buyer, false)); assertFalse(canConfirm(order, seller, false))
        assertFalse(canConfirm(order, buyer, true)); assertFalse(canConfirm(order.copy(deliveryConfirmed = true), buyer, false))
        assertFalse(canConfirm(order.copy(escrowStatus = "refunded"), buyer, false))
    }
    @Test fun `conversations group by ID even if names change`() {
        val first = Message(1, 2, "old_name", 1, "buyer", "First", "2026-09-24T10:00:00Z")
        val reply = Message(2, 1, "buyer", 2, "new_name", "Reply", "2026-09-25T10:00:00Z")
        val grouped = conversations(listOf(reply, first), 1)
        assertEquals(1, grouped.size); assertEquals("new_name", grouped[0].name); assertEquals("Reply", grouped[0].last.content)
    }
    @Test fun `server normalization preserves the API path`() {
        assertEquals("http://192.168.1.2:8000/api/", normalizedServer(" http://192.168.1.2:8000/api ", true))
        assertEquals("https://example.com/api/", normalizedServer("https://example.com/api/", false))
    }
    @Test(expected = IllegalArgumentException::class) fun `release rejects plain HTTP`() { normalizedServer("http://example.com/api/", false) }
    @Test(expected = IllegalArgumentException::class) fun `URL credentials are rejected`() { normalizedServer("https://user:secret@example.com/api/", true) }
    @Test(expected = IllegalArgumentException::class) fun `wrong API path is rejected`() { normalizedServer("https://example.com/", true) }
    @Test fun `snake case decoding retains decimal prices and image URLs`() {
        val json = """{"id":7,"seller":2,"seller_name":"seller","seller_trust_score":100,"category":1,"category_name":"Phones","title":"Phone","description":"Sample","price":"78000.00","condition":"good","status":"verified","location":"Colombo","images":[{"id":1,"image":"http://localhost:8000/media/photo.png"}]}"""
        val decoded = apiGson.fromJson(json, Listing::class.java)
        assertEquals("78000.00", decoded.price); assertEquals("seller", decoded.sellerName); assertEquals(1, decoded.images.size)
    }
    @Test fun `transport uses existing endpoint paths and JSON request field names`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/api/")).addConverterFactory(GsonConverterFactory.create(apiGson)).build().create(Api::class.java)
            server.enqueue(MockResponse().setBody("{\"id\":1}"))
            api.post("messages/", mapOf("receiver" to 2, "content" to "Hello", "listing" to 7))
            val sent = server.takeRequest()
            assertEquals("POST", sent.method); assertEquals("/api/messages/", sent.path)
            assertTrue(sent.body.readUtf8().contains("\"receiver\":2"))
            server.enqueue(MockResponse().setBody("{\"results\":[],\"next\":null}"))
            api.get("listings/", mapOf("page" to "2", "status" to "verified"))
            assertEquals("/api/listings/?page=2&status=verified", server.takeRequest().path)
        } finally { server.shutdown() }
    }
    @Test fun `multipart includes repeated Django uploaded_images fields`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/api/")).addConverterFactory(GsonConverterFactory.create(apiGson)).build().create(Api::class.java)
            server.enqueue(MockResponse().setBody("{\"id\":17}"))
            val parts = listOf(MultipartBody.Part.createFormData("title", "Sample product")) + (1..3).map {
                MultipartBody.Part.createFormData("uploaded_images", "sample-$it.png", byteArrayOf(1, 2, 3).toRequestBody("image/png".toMediaType()))
            }
            api.upload("listings/", parts)
            val request = server.takeRequest()
            assertEquals("/api/listings/", request.path)
            assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
            val body = request.body.readUtf8()
            assertEquals(3, Regex("name=\"uploaded_images\"").findAll(body).count())
            assertTrue(body.contains("Sample product"))
        } finally { server.shutdown() }
    }
}
