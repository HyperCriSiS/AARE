package net.prototux.aare.ui.car

import net.prototux.aare.ui.theme.CarAccents
import net.prototux.aare.ui.theme.DarkGlassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StyleChoicesTest {
    @Test
    fun tenStylesDoNotFitOneRowAFewShortNamesDo() {
        assertFalse(fitsOneRow(List(10) { "Milchglas" }))
        assertFalse(fitsOneRow(listOf("Automatisch", "Links", "Rechts", "Unten")))
        assertTrue(fitsOneRow(listOf("Auto", "Dunkel", "Hell")))
        assertTrue(fitsOneRow(emptyList()))
    }

    @Test
    fun everyStyleHasADotAndAMixOrThePhonesAccentHasNone() {
        CarStyles.all.forEach { assertTrue(it.themeId, styleSwatch("${it.themeId}/${it.layout.id}").size >= 2) }
        assertEquals(DarkGlassTheme.nightBackdrop + DarkGlassTheme.night.primary, styleSwatch("dark_glass"))
        CarAccents.all.forEach { assertEquals(listOf(it.dark.primary), styleSwatch(it.id)) }
        assertEquals(emptyList<Any>(), styleSwatch("custom"))
        assertEquals(emptyList<Any>(), styleSwatch(CarAccents.PHONE))
    }
}
