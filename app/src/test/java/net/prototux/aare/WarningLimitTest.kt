package net.prototux.aare

import org.junit.Assert.assertEquals
import org.junit.Test

class WarningLimitTest {
    /** A warning per message (a channel the car streams on that AARE didn't open) must not flood the log. */
    @Test fun firstFewThenOneInAHundred() {
        assertEquals(listOf(1, 2, 3, 100, 200), (1..250).filter(WarningLimit::shows))
    }
}
