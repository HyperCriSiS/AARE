package net.prototux.aare.ui.car

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowsMediaTest {
    private val kinds = mapOf("music" to CarAppKind.MEDIA, "map" to CarAppKind.NAVIGATION, "phone" to CarAppKind.PHONE)
    private fun kind(id: String) = kinds[id]

    @Test
    fun aMediaAppFullScreenOrInAPaneHoldsTheStream() {
        assertTrue(CarScreen.App("music").showsMedia(::kind))
        assertTrue(CarScreen.Split("map", "music").showsMedia(::kind))
    }

    @Test
    fun otherScreensDont() {
        assertFalse(CarScreen.App("map").showsMedia(::kind))
        assertFalse(CarScreen.Split("map", "phone").showsMedia(::kind))
        assertFalse(CarScreen.Dashboard.showsMedia(::kind))
        assertFalse(CarScreen.Launcher.showsMedia(::kind))
    }
}
