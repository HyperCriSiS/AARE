package net.prototux.aare

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupImportTest {
    @Test
    fun aFileCannotOpenTheHeadUnitServerToTheNetwork() {
        listOf("hu_server", "hu_server_lan", "hu_server_lan_once", "head_unit_server_mirror").forEach { assertFalse(it, SettingsBackup.imports(it)) }
    }

    @Test
    fun ordinarySettingsAreImported() {
        assertTrue(SettingsBackup.imports("wireless"))
        assertTrue(SettingsBackup.imports("tcp"))
    }
}
