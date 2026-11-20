package net.prototux.aare.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarAudioOutTest {
    @Test fun soundWithoutFocusAsksAgainAfterAWhile() {
        assertTrue(CarAudioOut.askAgain(sounding = true, nowMs = 10_000, askedAtMs = 10_000 - CarAudioOut.REASK_MS))
    }

    @Test fun notTooOften() {
        assertFalse(CarAudioOut.askAgain(sounding = true, nowMs = 10_000, askedAtMs = 9_000))
    }

    @Test fun silenceNeverTakesTheCarsAudio() {
        assertFalse(CarAudioOut.askAgain(sounding = false, nowMs = 100_000, askedAtMs = 0))
    }
}
