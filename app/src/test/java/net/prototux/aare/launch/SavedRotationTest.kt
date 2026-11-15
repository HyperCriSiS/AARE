package net.prototux.aare.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedRotationTest {
    @Test
    fun keepsWhatTheCommandPrinted() {
        assertEquals("lock 0", savedRotation("lock 0\n"))
        assertEquals("free", savedRotation(" free "))
    }

    @Test
    fun anythingElseFreesTheRotation() {
        assertEquals("free", savedRotation(""))
        assertEquals("free", savedRotation("Error: unknown command"))
    }
}
