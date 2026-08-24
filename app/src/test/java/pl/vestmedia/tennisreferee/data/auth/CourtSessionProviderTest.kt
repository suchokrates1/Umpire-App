package pl.vestmedia.tennisreferee.data.auth

import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CourtSessionProviderTest {

    @Before
    @After
    fun resetProvider() {
        CourtSessionProvider.resetForTests()
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
