package net.prototux.aare.video

import org.junit.Assert.assertEquals
import org.junit.Test

class EncoderTweaksTest {
    @Test
    fun eachVariantChangesOnlyItsOwnPartAndAnUnknownOneNothing() {
        assertEquals(EncoderTweaks(), EncoderTweaks.of(""))
        assertEquals(EncoderTweaks(), EncoderTweaks.of("gone"))
        assertEquals(EncoderTweaks(level30 = true), EncoderTweaks.of("level30"))
        assertEquals(EncoderTweaks(constrainedBaseline = true), EncoderTweaks.of("constrained"))
        assertEquals(EncoderTweaks(noRepeatedHeaders = true), EncoderTweaks.of("headers"))
        assertEquals(EncoderTweaks(softwareEncoder = true), EncoderTweaks.of("software"))
        assertEquals(EncoderTweaks(level30 = true, constrainedBaseline = true, softwareEncoder = true, shortGop = true), EncoderTweaks.of("all"))
        assertEquals(6, EncoderTweaks.VARIANTS.size)
    }
}
