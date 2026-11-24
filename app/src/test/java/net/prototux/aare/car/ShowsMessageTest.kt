package net.prototux.aare.car

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowsMessageTest {
    @Test
    fun anAppSwitchedOnShows() = assertTrue(showsMessage("org.chat", emptySet(), emptySet()))

    @Test
    fun mutedOrNotInTheCarStaysOff() {
        assertFalse(showsMessage("org.chat", setOf("org.chat"), emptySet()))
        assertFalse(showsMessage("org.chat", emptySet(), setOf("org.chat")))
    }

    @Test
    fun aMutedAppGetsNoAutoReply() {
        assertFalse(autoReplies("com.whatsapp", setOf("com.whatsapp"), emptySet()))
        assertTrue(autoReplies("com.whatsapp", setOf("org.telegram.messenger"), emptySet()))
        assertFalse(autoReplies("com.whatsapp", emptySet(), setOf("com.whatsapp")))
    }
}
