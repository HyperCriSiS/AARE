package net.prototux.aare.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarLinksTest {
    private val claude = "com.anthropic.claude"

    private fun parse(path: String, query: Map<String, String> = emptyMap(), host: String? = CarLinks.HOST, caller: String? = claude) =
        CarLinks.parse(host, path, query, caller, claude)

    @Test
    fun aLinkNamesTheToolAndItsArguments() {
        val args = mapOf("destination" to "Berlin")
        assertEquals("car_navigate" to args, parse("/AARE/go/car_navigate", args))
        assertEquals("car_media" to mapOf("action" to "next"), parse("/AARE/go/car_media/", mapOf("action" to "next")))
    }

    @Test
    fun toolsThatSendReadOrOpenAppsAreRefused() {
        assertNull(parse("/AARE/go/car_reply", mapOf("conversation" to "Mom", "message" to "hi")))
        assertNull(parse("/AARE/go/car_messages"))
        assertNull(parse("/AARE/go/car_mark_read", mapOf("conversation" to "Mom")))
        assertNull(parse("/AARE/go/car_open", mapOf("what" to "phone screen")))
    }

    @Test
    fun onlyTheAssistantMaySendOne() {
        assertNull(parse("/AARE/go/car_media", mapOf("action" to "next"), caller = "com.android.chrome"))
        assertNull(parse("/AARE/go/car_media", mapOf("action" to "next"), caller = null))
        assertNull(CarLinks.parse(CarLinks.HOST, "/AARE/go/car_media", emptyMap(), null, null))
    }

    @Test
    fun otherAddressesAreNotLinks() {
        assertNull(parse("/AARE/"))
        assertNull(parse("/AARE/go/"))
        assertNull(parse("/other/go/car_media", mapOf("action" to "next")))
        assertNull(parse("/AARE/go/car_media", mapOf("action" to "next"), host = "example.com"))
        assertNull(parse("/AARE/go/x/../car_media", mapOf("action" to "next")))
    }
}
