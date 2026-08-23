package pl.vestmedia.tennisreferee.startup

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.auth.CourtSession
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider
import pl.vestmedia.tennisreferee.data.auth.EncryptedCourtSessionStore

/**
 * Device/emulator cold-start smoke. Run against the minified release APK with:
 * `./gradlew :app:connectedReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=pl.vestmedia.tennisreferee.startup.ApplicationStartupSmokeTest`
 */
@RunWith(AndroidJUnit4::class)
class ApplicationStartupSmokeTest {

    @Test
    fun applicationOnCreateLeavesAWorkingCourtSessionStore() {
        ApplicationProvider.getApplicationContext<TennisRefereeApp>()
        val store = CourtSessionProvider.get()
        store.clear()
        try {
            store.save(
                CourtSession(
                    courtId = "instrumented-court",
                    token = "instrumented-token",
                    expiresAtMillis = Long.MAX_VALUE
                )
            )
            val current = store.current()
            assertEquals("instrumented-court", current?.courtId)
            assertTrue(current!!.hasValidToken())
        } finally {
            store.clear()
            assertNull(store.current())
        }
    }

    @Test
    fun retrofitClientCanBeResolvedAfterProcessStart() {
        ApplicationProvider.getApplicationContext<TennisRefereeApp>()
        assertNotNull(RetrofitClient.apiService)
    }

    @Test
    fun encryptedSharedPreferencesClassesSurviveR8() {
        Class.forName("androidx.security.crypto.EncryptedSharedPreferences")
        Class.forName("androidx.security.crypto.MasterKey")
        Class.forName("com.google.crypto.tink.Aead")
        EncryptedCourtSessionStore::class.java.getDeclaredConstructor(
            android.content.Context::class.java
        )
    }
}
