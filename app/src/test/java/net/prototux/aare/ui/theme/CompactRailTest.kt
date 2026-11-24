package net.prototux.aare.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactRailTest {
    @Test
    fun theRailNarrowsButTheTapAreaKeepsItsSize() {
        val normal = CarDimens()
        val compact = normal.compactRail()
        assertEquals(normal.railButton, compact.railButton)
        assertTrue(compact.railWidth < normal.railWidth)
        assertTrue(compact.railWidth >= compact.railButton)
        assertTrue(compact.railAppIcon < normal.railAppIcon)
        assertEquals(normal.touchTarget, compact.touchTarget)
        assertEquals(36.dp, compact.iconSize) // the rest of the car screen is untouched
    }
}
