package pl.vestmedia.tennisreferee.data.auth

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.security.GeneralSecurityException

class CourtSessionStoreFactoryTest {

    @Before
    @After
    fun resetProvider() {
        CourtSessionProvider.resetForTests()
    }

    @Test
    fun createStoreUsesEncryptedStoreWhenKeystoreWorks() {
        val encrypted = InMemoryCourtSessionStore()

        val store = createCourtSessionStore(
            encryptedFactory = { encrypted },
            fallbackFactory = { fail("fallback must not run"); InMemoryCourtSessionStore() }
        )

        assertSame(encrypted, store)
    }

    @Test
    fun createStoreFallsBackWhenEncryptedSharedPreferencesThrows() {
        val fallback = InMemoryCourtSessionStore()

        val store = createCourtSessionStore(
            encryptedFactory = { throw GeneralSecurityException("Keystore is unavailable") },
            fallbackFactory = { fallback }
        )

        assertSame(fallback, store)
        store.save(CourtSession("court-7", token = "tok", expiresAtMillis = 2_000L))
        assertEquals("court-7", store.current()?.courtId)
        assertTrue(store.current()!!.hasValidToken(1_000L))
    }

    @Test
    fun createStoreFallsBackWhenR8StrippedEncryptedClasses() {
        val fallback = InMemoryCourtSessionStore()

        val store = createCourtSessionStore(
            encryptedFactory = {
                throw NoClassDefFoundError("androidx.security.crypto.EncryptedSharedPreferences")
            },
            fallbackFactory = { fallback }
        )

        assertSame(fallback, store)
    }

    @Test
    fun createStoreSurfacesFallbackFailureWithEncryptedCause() {
        try {
            createCourtSessionStore(
                encryptedFactory = { throw GeneralSecurityException("keystore") },
                fallbackFactory = { throw IllegalStateException("fallback prefs missing") }
            )
            fail("expected fallback failure")
        } catch (error: IllegalStateException) {
            assertEquals("fallback prefs missing", error.message)
            assertTrue(error.suppressed.any { it is GeneralSecurityException })
        }
    }

    @Test
    fun getThrowsBeforeInitialize() {
        try {
            CourtSessionProvider.get()
            fail("expected uninitialized provider to throw")
        } catch (error: IllegalStateException) {
            assertTrue(error.message!!.contains("Application.onCreate"))
        }
    }

    @Test
    fun initializeForTestsMakesStoreAvailableToRetrofitPath() {
        val store = InMemoryCourtSessionStore()
        CourtSessionProvider.initializeForTests(store)
        assertSame(store, CourtSessionProvider.get())
    }
}
