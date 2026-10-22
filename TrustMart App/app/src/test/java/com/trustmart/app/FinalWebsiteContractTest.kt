package com.trustmart.app

import com.trustmart.app.data.*
import org.junit.Assert.*
import org.junit.Test

class FinalWebsiteContractTest {
    @Test fun phoneRequiresExactlyTenAsciiDigits() {
        assertTrue(validPhone("0771234567"))
        listOf("+94771234567", "077 1234567", "077123456", "07712345678", "٠٧٧١٢٣٤٥٦٧").forEach { assertFalse(validPhone(it)) }
    }
    @Test fun passwordMatchesFinalWebsiteRules() {
        assertTrue(validPassword("SamplePass9"))
        listOf("short1A", "lowercase9", "UPPERCASE9", "NoNumberHere", "123456789").forEach { assertFalse(validPassword(it)) }
    }
    @Test fun accountDecodesDistrictAndNic() {
        val user = apiGson.fromJson("""{"id":1,"district":"Colombo","nic_number":"123456789","nic_suffix":"V"}""", User::class.java)
        assertEquals("Colombo", user.district)
        assertEquals("123456789", user.nicNumber)
        assertEquals("V", user.nicSuffix)
    }
    @Test fun listingDecodesServerManagementAndContactFields() {
        val listing = apiGson.fromJson("""{"id":1,"district":"Colombo","city":"Dehiwala","contact_phone":"0771234567","whatsapp_enabled":true,"can_manage":false}""", Listing::class.java)
        assertEquals("Dehiwala", listing.city)
        assertEquals("0771234567", listing.contactPhone)
        assertTrue(listing.whatsappEnabled)
        assertFalse(listing.canManage)
    }
    @Test fun promotionUsesServerComputedDecimalText() {
        val ad = apiGson.fromJson("""{"id":1,"original_price":"1000.00","effective_offer_price":"800.00","effective_discount_percent":"20.00"}""", Advertisement::class.java)
        assertEquals("800.00", ad.effectiveOfferPrice)
        assertEquals("20.00", ad.effectiveDiscountPercent)
    }
    @Test fun suggestionRetainsWebsiteNavigationTarget() {
        val suggestion = apiGson.fromJson("""{"id":"listing-4","label":"Phone","detail":"Colombo","kind":"Listing","url":"/listings/4"}""", SearchSuggestion::class.java)
        assertEquals("/listings/4", suggestion.url)
    }
}
