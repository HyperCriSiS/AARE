package net.prototux.aare.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionReplaceTest {
    @Test
    fun aNewUsbLinkReplacesAUsbRetryThatNeverReachedTheCar() {
        assertTrue(replacesSession(newWireless = false, oldWireless = false, oldReachedHeadUnit = false))
    }

    @Test
    fun aRunningUsbSessionStays() {
        assertFalse(replacesSession(newWireless = false, oldWireless = false, oldReachedHeadUnit = true))
    }

    @Test
    fun wirelessNeverReplacesOrIsReplaced() {
        assertFalse(replacesSession(newWireless = true, oldWireless = false, oldReachedHeadUnit = false))
        assertFalse(replacesSession(newWireless = false, oldWireless = true, oldReachedHeadUnit = false))
    }
}
