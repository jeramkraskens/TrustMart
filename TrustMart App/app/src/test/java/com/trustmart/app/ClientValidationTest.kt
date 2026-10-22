package com.trustmart.app

import com.trustmart.app.data.parseRefreshedTokens
import com.trustmart.app.data.selectedPhotos
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ClientValidationTest {
    @Test fun cancelledPickerPreservesSelectedPhotos() {
        assertEquals(listOf("content://first"), selectedPhotos(listOf("content://first"), emptyList(), 5))
    }
    @Test fun replacementPhotosAreUniqueAndRespectRemainingCapacity() {
        assertEquals(listOf("a", "b"), selectedPhotos(listOf("old"), listOf("a", "a", "b", "c"), 2))
        assertTrue(selectedPhotos(emptyList(), listOf("a"), -1).isEmpty())
    }
    @Test fun refreshAllowsAccessOnlyOrRotatedTokens() {
        assertEquals("access", parseRefreshedTokens("""{"access":"access"}""").access)
        assertNull(parseRefreshedTokens("""{"access":"access"}""").refresh)
        assertEquals("rotated", parseRefreshedTokens("""{"access":"access","refresh":"rotated"}""").refresh)
    }
    @Test fun malformedRefreshAlwaysUsesRecoverableIOException() {
        listOf("", "not-json", "[]", "{}", """{"access":null}""", """{"access":7}""",
            """{"access":" "}""", """{"access":"bad\nheader"}""", """{"access":"bad token"}""",
            """{"access":"valid","refresh":{}}""").forEach { response ->
            try { parseRefreshedTokens(response); fail("Accepted malformed response: $response") }
            catch (_: IOException) { }
        }
    }
}
