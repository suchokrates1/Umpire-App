package pl.vestmedia.tennisreferee.ui.playerselection

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.ScheduleSuggestion
import pl.vestmedia.tennisreferee.domain.match.MatchProgressReducer
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode
import pl.vestmedia.tennisreferee.startup.StartupTestApp
import pl.vestmedia.tennisreferee.ui.courtselection.CourtSelectionViewModel

@RunWith(RobolectricTestRunner::class)
@Config(application = StartupTestApp::class, sdk = [34])
class LocalizedViewModelErrorsTest {

    private lateinit var server: MockWebServer
    private val app: TennisRefereeApp
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        app.container.overrideBaseUrl(server.url("/").toString())
    }

    @After
    fun tearDown() {
        app.container.overrideBaseUrl(null)
        server.shutdown()
    }

    @Test
    fun playerAndCourtErrorsUseLocalizedStringsWithoutLeakingRawServerText() {
        val playerVm = PlayerSelectionViewModel(app)
        playerVm.setDemoPlayers("t1-1", emptyList())
        val applied = playerVm.applySuggestedMatch(
            ScheduleSuggestion(
                id = 1,
                tournamentId = 1,
                courtId = "t1-1",
                player1Name = "Missing One",
                player2Name = "Missing Two",
            )
        )
        assertFalse(applied)
        assertEquals(app.getString(R.string.error_suggested_players_unavailable), playerVm.error.value)

        // 401 maps to localized expired-session message
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"sqlite trace /data/secret"}"""))
        playerVm.loadPlayers("t1-1")
        waitForIdleError { playerVm.error.value }
        assertEquals(app.getString(R.string.error_court_auth_expired), playerVm.error.value)
        assertFalse(playerVm.error.value.orEmpty().contains("sqlite"))

        // 400 on addPlayer maps to localized add-player error without leaking internal error text,
        // and the request body includes kort_id for bearer-authenticated sessions.
        server.takeRequest() // consume GET /api/players
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"internal_sql_constraint_failed"}"""))
        playerVm.clearError()
        playerVm.addPlayer("Jan", "Kowalski", "PL", "B1", "t1-1")
        waitForIdleError { playerVm.error.value }
        val addReqBody = server.takeRequest().body.readUtf8()
        assertTrue("addPlayer must send kort_id: $addReqBody", addReqBody.contains("\"kort_id\":\"t1-1\""))
        assertFalse("addPlayer must not send legacy pin: $addReqBody", addReqBody.contains("\"pin\""))
        assertEquals(app.getString(R.string.error_adding_player), playerVm.error.value)
        assertFalse(playerVm.error.value.orEmpty().contains("internal_sql"))

        // 404 on courts maps to localized courts error
        val courtVm = CourtSelectionViewModel(app)
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"stack_trace_leak"}"""))
        courtVm.loadCourts(1)
        waitForIdleError { courtVm.error.value }
        assertEquals(app.getString(R.string.error_loading_courts), courtVm.error.value)
    }

    @Test
    fun matchStateInheritsNoAdvantageAndStatsModeFromMatchConfig() {
        val p1 = Player(id = 1, name = "A", firstName = "A", lastName = "One")
        val p2 = Player(id = 2, name = "B", firstName = "B", lastName = "Two")
        val state = MatchState(
            player1 = p1,
            player2 = p2,
            courtId = "1",
            courtName = "Court 1",
            matchConfig = MatchConfig(noAdvantage = true, statsMode = StatsMode.BASIC),
        ).apply {
            player1Points = 3
            player2Points = 3
        }

        assertEquals(StatsMode.BASIC, state.statsMode)
        assertTrue(state.isNoAdvantage())
        val reduced = MatchProgressReducer.reduceAfterPoint(state, currentAnnouncementType = null, nowMs = 1_000L)
        assertEquals(MatchProgressReducer.ANNOUNCEMENT_DECIDING_POINT, reduced.announcementType)
        state.player1Points = 4
        assertTrue(state.isGameWon())
    }

    private fun waitForIdleError(read: () -> String?) {
        val deadline = System.currentTimeMillis() + 5_000L
        while (read() == null && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(25)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }
}
