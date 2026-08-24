package pl.vestmedia.tennisreferee.startup

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.auth.CourtSession
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider
import pl.vestmedia.tennisreferee.data.auth.SharedPreferencesCourtSessionStore

/**
 * JVM stand-in for a cold start. Production must not touch Android Keystore
 * here — EncryptedSharedPreferences.create() can native-crash a real phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = StartupTestApp::class, sdk = [34])
class ApplicationStartupRobolectricTest {

    @Test
    fun onCreateInitializesAWorkingSessionStoreWithoutCrashing() {
        val app = ApplicationProvider.getApplicationContext<StartupTestApp>()
        val store = CourtSessionProvider.get()

        assertTrue(
            "Cold start must use ordinary SharedPreferences, never EncryptedSharedPreferences",
            store is SharedPreferencesCourtSessionStore
        )

        store.clear()
        store.save(
            CourtSession(
                courtId = "startup-court",
                token = "startup-token",
                expiresAtMillis = Long.MAX_VALUE
            )
        )

        val current = store.current()
        assertNotNull(current)
        assertTrue(current!!.hasValidToken())

        store.clear()
        assertNull(store.current())
        assertNotNull(app)
    }

    @Test
    fun retrofitClientIsUsableAfterApplicationOnCreate() {
        ApplicationProvider.getApplicationContext<StartupTestApp>()
        assertNotNull(RetrofitClient.apiService)
    }
}
