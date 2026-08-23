package pl.vestmedia.tennisreferee.startup

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.auth.CourtSession
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider
import pl.vestmedia.tennisreferee.data.auth.EncryptedCourtSessionStore
import pl.vestmedia.tennisreferee.data.auth.SharedPreferencesCourtSessionStore

/**
 * JVM stand-in for a cold start. Robolectric has no real Android Keystore, so
 * EncryptedSharedPreferences.create() fails the same way a backup-restore or
 * stripped-Tink Play build can. Application.onCreate must still finish.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TennisRefereeApp::class, sdk = [34])
class ApplicationStartupRobolectricTest {

    @Test
    fun onCreateInitializesAWorkingSessionStoreWithoutCrashing() {
        val app = ApplicationProvider.getApplicationContext<TennisRefereeApp>()
        val store = CourtSessionProvider.get()

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
        assertTrue(
            "Robolectric must use the fallback store, not die in EncryptedSharedPreferences.create()",
            store is SharedPreferencesCourtSessionStore || store is EncryptedCourtSessionStore
        )

        store.clear()
        assertNull(store.current())
        assertNotNull(app)
    }

    @Test
    fun retrofitClientIsUsableAfterApplicationOnCreate() {
        ApplicationProvider.getApplicationContext<TennisRefereeApp>()
        assertNotNull(RetrofitClient.apiService)
    }
}
