package net.prototux.aare.host

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunsWhileDrivingTest {
    @Test
    fun navigationAppsRunOn() {
        assertTrue(runsWhileDriving("com.google.android.apps.maps"))
        assertTrue(runsWhileDriving("com.waze"))
    }

    /** Not in the known list, but a maps app by its category (OsmAnd, Organic Maps...). */
    @Test
    fun mapsCategoryAppsRunOn() {
        assertTrue(runsWhileDriving("net.osmand", android.content.pm.ApplicationInfo.CATEGORY_MAPS))
        assertFalse(runsWhileDriving("org.wisso.newpipematerial", android.content.pm.ApplicationInfo.CATEGORY_VIDEO))
    }

    @Test
    fun otherAppsPause() {
        assertFalse(runsWhileDriving("org.wisso.newpipematerial"))
        assertFalse(runsWhileDriving("com.anthropic.claude"))
    }
}
