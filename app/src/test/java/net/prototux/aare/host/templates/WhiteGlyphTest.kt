package net.prototux.aare.host.templates

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhiteGlyphTest {
    private val clear = 0x00000000
    private val white = 0xFFFFFFFF.toInt()
    private val offWhite = 0xFFEEEEEE.toInt()
    private val blue = 0xFF2266DD.toInt()
    private val faintBlack = 0x20000000 // nearly transparent: anti-aliasing, not part of the glyph

    /** A 10x10 icon: [glyph] on [count] pixels, the rest [ground]. */
    private fun icon(glyph: Int, count: Int, ground: Int = clear) = IntArray(100) { if (it < count) glyph else ground }

    @Test
    fun aWhiteGlyphOnClearGroundIsOne() {
        assertTrue(isWhiteGlyph(icon(white, 30)))
        assertTrue(isWhiteGlyph(icon(offWhite, 30, ground = faintBlack)))
    }

    @Test
    fun aColoredIconIsNot() {
        assertFalse(isWhiteGlyph(icon(blue, 30)))
        assertFalse(isWhiteGlyph(icon(white, 30).also { it[0] = blue }))
    }

    @Test
    fun aWhiteTileOrPhotoFillingItsBoxIsNot() {
        assertFalse(isWhiteGlyph(icon(white, 100)))
    }

    @Test
    fun anEmptyIconIsNot() {
        assertFalse(isWhiteGlyph(icon(white, 0)))
    }
}
