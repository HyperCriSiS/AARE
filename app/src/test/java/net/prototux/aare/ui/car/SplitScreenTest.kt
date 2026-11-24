package net.prototux.aare.ui.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitScreenTest {
    private val split = CarScreen.Split("maps", "music")

    @Test
    fun openingInAPaneKeepsTheAppOnScreenInTheOther() {
        assertEquals(CarScreen.Split("phone", "maps"), splitWith(CarScreen.App("maps"), "phone", left = true, recent = emptyList()))
        assertEquals(CarScreen.Split("maps", "phone"), splitWith(CarScreen.App("maps"), "phone", left = false, recent = emptyList()))
    }

    @Test
    fun aNewAppReplacesTheChosenPaneOfASplit() {
        assertEquals(CarScreen.Split("phone", "music"), splitWith(split, "phone", left = true, recent = emptyList()))
        assertEquals(CarScreen.Split("maps", "phone"), splitWith(split, "phone", left = false, recent = emptyList()))
    }

    @Test
    fun anAppAlreadyInASplitSwapsSidesOrStays() {
        assertEquals(CarScreen.Split("music", "maps"), splitWith(split, "music", left = true, recent = emptyList()))
        assertEquals(CarScreen.Split("music", "maps"), splitWith(split, "maps", left = false, recent = emptyList()))
        assertEquals(split, splitWith(split, "maps", left = true, recent = emptyList()))
    }

    @Test
    fun withNothingElseOnScreenTheOtherPaneIsTheMostRecentApp() {
        assertEquals(CarScreen.Split("maps", "music"), splitWith(CarScreen.Dashboard, "maps", left = true, recent = listOf("maps", "music")))
        assertEquals(CarScreen.Split("music", "maps"), splitWith(CarScreen.App("maps"), "maps", left = false, recent = listOf("music")))
    }

    @Test
    fun withNoOtherAppItOpensFullScreen() {
        assertEquals(CarScreen.App("maps"), splitWith(CarScreen.Launcher, "maps", left = true, recent = listOf("maps")))
    }

    @Test
    fun theDividerClosesAPaneOnlyNearAnEdge() {
        assertEquals(CarScreen.App("music"), snapSplit(split, 0.05f))
        assertEquals(CarScreen.App("maps"), snapSplit(split, 0.95f))
        assertEquals(split, snapSplit(split, 0.1f))
        assertEquals(split, snapSplit(split, 0.5f))
        assertEquals(split, snapSplit(split, 0.9f))
    }

    @Test
    fun anAppLeavingThePanesLeavesTheOtherFullScreen() {
        assertEquals(CarScreen.App("music"), split.withoutApps { it == "maps" })
        assertEquals(CarScreen.App("maps"), split.withoutApps { it == "music" })
        assertEquals(CarScreen.Dashboard, split.withoutApps { true })
        assertEquals(split, split.withoutApps { false })
        assertEquals(CarScreen.App("maps"), CarScreen.App("maps").withoutApps { true })
    }

    @Test
    fun showsCoversBothPanes() {
        assertTrue(split.shows("maps"))
        assertTrue(split.shows("music"))
        assertFalse(split.shows("phone"))
        assertFalse(CarScreen.Dashboard.shows("maps"))
    }

    @Test
    fun aPaneRestsNoNarrowerThanItsMinimumUnlessItCloses() {
        // 1165 dp wide (the MBUX screen): 240 dp is about a fifth.
        assertEquals(240f / 1165f, splitRest(0.15f, 1165f), 0.001f)
        assertEquals(1 - 240f / 1165f, splitRest(0.85f, 1165f), 0.001f)
        assertEquals(0.4f, splitRest(0.4f, 1165f), 0.001f)
        // Near an edge it stays there, so that pane closes.
        assertEquals(0.05f, splitRest(0.05f, 1165f), 0.001f)
        // Too narrow for two minimum panes: the middle.
        assertEquals(0.5f, splitRest(0.3f, 400f), 0.001f)
    }
}
