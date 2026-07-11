package pl.vestmedia.tennisreferee.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourtSessionTest {
    @Test
    fun parsesIsoAndUnixExpiryValues() {
        assertEquals(1_783_787_400_000L, parseSessionExpiry("2026-07-11T16:30:00Z"))
        assertEquals(1_783_787_400_000L, parseSessionExpiry("1783787400"))
        assertNull(parseSessionExpiry("not-an-expiry"))
    }

    @Test
    fun validTokenRequiresANonExpiredExpiry() {
        assertTrue(CourtSession("1", token = "token", expiresAtMillis = 2_000L).hasValidToken(1_000L))
        assertFalse(CourtSession("1", token = "token", expiresAtMillis = 1_000L).hasValidToken(1_000L))
        assertFalse(CourtSession("1", token = "token").hasValidToken(1_000L))
    }
}
