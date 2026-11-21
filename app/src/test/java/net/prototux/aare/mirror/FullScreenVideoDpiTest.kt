package net.prototux.aare.mirror

import org.junit.Assert.assertEquals
import org.junit.Test

class FullScreenVideoDpiTest {
    @Test
    fun aFullScreenVideoIsDrawnLargerThanTheCarScreen() {
        // MBUX at 240 dpi: the player's controls at 360, half as large again.
        assertEquals(360, fullScreenVideoDpi(240))
    }
}
