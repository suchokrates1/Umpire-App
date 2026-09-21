package pl.vestmedia.tennisreferee.utils

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.vestmedia.tennisreferee.data.api.dto.TournamentOptionDto
import pl.vestmedia.tennisreferee.data.api.dto.toModel
import pl.vestmedia.tennisreferee.startup.StartupTestApp

@RunWith(RobolectricTestRunner::class)
@Config(application = StartupTestApp::class, sdk = [34])
class TestLabTest {

    // Shape of GET /api/tournaments/active on production, 2026-09-21
    private val activeJson = """
        [{"id":32,"name":"RAKIETY ATNiS VII","city":"Giebułtów","is_public":1,"is_simulation":0},
         {"id":26,"name":"App Review Access","city":"Review","is_public":0,"is_simulation":1}]
    """.trimIndent()

    private val tournaments = Gson().fromJson(activeJson, Array<TournamentOptionDto>::class.java).map { it.toModel() }

    @Test
    fun theReviewSandboxIsTheOnlyTournamentInTestLab() {
        assertEquals(listOf(26), TestLab.visibleTournaments(tournaments, inTestLab = true).map { it.id })
    }

    @Test
    fun umpiresSeeEveryActiveTournament() {
        assertEquals(listOf(32, 26), TestLab.visibleTournaments(tournaments, inTestLab = false).map { it.id })
    }

    @Test
    fun aTestLabDeviceIsRecognisedByItsSystemSetting() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(TestLab.isRunning(context))
        Settings.System.putString(context.contentResolver, "firebase.test.lab", "true")
        assertTrue(TestLab.isRunning(context))
    }
}
