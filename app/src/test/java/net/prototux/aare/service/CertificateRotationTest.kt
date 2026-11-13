package net.prototux.aare.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CertificateRotationTest {
    @Test
    fun aGoogleCertificateStaysAfterAHangUp() {
        assertFalse(rotates(googleIssued = true, explicit = false))
    }

    @Test
    fun aGoogleCertificateTheCarRefusesGivesWay() {
        assertTrue(rotates(googleIssued = true, explicit = true))
    }

    @Test
    fun anyOtherCertificateGivesWay() {
        assertTrue(rotates(googleIssued = false, explicit = false))
        assertTrue(rotates(googleIssued = false, explicit = true))
    }
}
