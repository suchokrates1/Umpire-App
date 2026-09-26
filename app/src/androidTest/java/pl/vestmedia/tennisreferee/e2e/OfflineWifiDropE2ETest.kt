package pl.vestmedia.tennisreferee.e2e

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode
import pl.vestmedia.tennisreferee.ui.match.MatchActivity

/**
 * The radio drops in the middle of a match and comes back minutes later.
 *
 * Scoring is local, so the umpire must be able to keep going while nothing reaches the
 * server; the queued games then have to arrive once the radio is back. The offline window
 * is `offline.minutes` (default 1) so the same test can be run for 1, 2 and 5 minutes.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OfflineWifiDropE2ETest {

    private val backend = E2EBackendClient()
    private lateinit var fixture: TournamentFixture

    @Before
    fun setUp() {
        setAirplane(false)
        overrideUmpireBackend(backend.baseUrl)
        val marker = "E2E-${System.currentTimeMillis()}-wifidrop"
        backend.cleanup(marker)
        fixture = backend.createTournamentFixture(marker)
        runBlocking {
            ApplicationProvider.getApplicationContext<TennisRefereeApp>()
                .matchHistoryRepository
                .deleteAllMatches()
        }
    }

    @After
    fun tearDown() {
        try {
            setAirplane(false)
            if (::fixture.isInitialized) backend.cleanup(fixture.marker)
            runBlocking {
                ApplicationProvider.getApplicationContext<TennisRefereeApp>()
                    .matchHistoryRepository
                    .deleteAllMatches()
            }
        } finally {
            overrideUmpireBackend(null)
            backend.close()
        }
    }

    @Test
    fun gamesPlayedWithTheRadioOffArriveWhenItIsBack() {
        val offlineMinutes = instrumentationArg("offline.minutes")?.toLongOrNull() ?: 1L
        val matchScenario = scenario(
            name = "wifi_drop",
            playerIndexes = listOf(0, 1),
            config = MatchConfig(gamesPerSet = 4, setsToWin = 1, statsMode = StatsMode.ADVANCED),
            steps = listOf(Game(true)),
            expectedSets = 1 to 0,
            expectedSetScores = listOf(4 to 0),
        )
        val matchState = matchScenario.toMatchState(fixture, courtIndex = 0)
        val team1 = matchState.getTeam1FullName()
        val team2 = matchState.getTeam2FullName()
        backend.seedAppCourtSession(matchState.courtId)

        ActivityScenario.launch<MatchActivity>(intentFor(matchState)).use {
            UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
            UmpireRobot.clickServerButton(matchScenario.firstServer)
            val robot = UmpireRobot(doubles = false, firstServer = matchScenario.firstServer)

            // Online: the first game reaches the server, so there is a row to fall behind.
            robot.playGame(team1Wins = true)
            val online = backend.waitForMatch(
                fixture.marker,
                description = "first game on the server",
            ) { match ->
                match.optString("player1_name") == team1 &&
                    match.optJSONObject("score")?.optInt("player1_games") == 1
            }
            val matchId = online.getInt("id")

            setAirplane(true)
            try {
                // Offline: two more games. The instrumentation has no network either, so the
                // proof that the tablet kept scoring has to come off its own screen.
                robot.playGame(team1Wins = true)
                assertCurrentGames("2")
                robot.playGame(team1Wins = true)
                assertCurrentGames("3")

                Thread.sleep(offlineMinutes * 60_000L)

                // Still counting after minutes with no radio, and still on the same match.
                assertCurrentGames("3")
            } finally {
                setAirplane(false)
            }

            // Nothing flushes on reconnect alone: the queue drains on the next sync.
            val beforeFlush = backend.waitForMatch(
                fixture.marker,
                description = "match row still behind right after the radio returns",
            ) { match -> match.optInt("id") == matchId }
            assertEquals(
                "the offline games must still be queued, not lost",
                1,
                beforeFlush.getJSONObject("score").optInt("player1_games"),
            )

            // Back online: the closing game finishes the set, which flushes the queue.
            // The wait stays inside the activity: the flush runs in the match scope, so
            // closing the screen would cancel a drain that is still working through backoff.
            val flushStart = System.currentTimeMillis()
            robot.playGame(team1Wins = true)
            val finished = backend.waitForMatch(
                fixture.marker,
                timeoutMs = DRAIN_TIMEOUT_MS,
                description = "finished match once the queue drained",
            ) { match -> match.optInt("id") == matchId && match.optString("status") == "finished" }
            Log.i(TAG, "queue drained in ${System.currentTimeMillis() - flushStart} ms")

            val score = finished.getJSONObject("score")
            val sets = score.getJSONArray("sets_history")
            assertEquals("one completed set", 1, sets.length())
            assertEquals(4, sets.getJSONObject(0).getInt("player1_games"))
            assertEquals(0, sets.getJSONObject(0).getInt("player2_games"))
            assertEquals(1, score.getInt("player1_sets"))
            assertTrue("a winner was recorded", finished.optString("winner_name").isNotBlank())
        }
    }

    /** The in-progress set column carries the current game count. */
    private fun assertCurrentGames(games: String) {
        onView(allOf(withId(R.id.textPlayer1Set1), isDisplayed())).check(matches(withText(games)))
    }

    private fun intentFor(matchState: MatchState): Intent {
        val extras = Bundle().apply {
            classLoader = MatchState::class.java.classLoader
            putParcelable(MatchActivity.EXTRA_MATCH_STATE, matchState)
            putBoolean(MatchActivity.EXTRA_IS_DOUBLES, matchState.isDoubles)
        }
        return Intent(ApplicationProvider.getApplicationContext(), MatchActivity::class.java)
            .putExtras(extras)
    }

    private fun instrumentationArg(name: String): String? =
        InstrumentationRegistry.getArguments().getString(name)?.trim()?.takeIf { it.isNotEmpty() }

    private fun setAirplane(enabled: Boolean) {
        val command = if (enabled) {
            "cmd connectivity airplane-mode enable"
        } else {
            "cmd connectivity airplane-mode disable"
        }
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.readBytes()
        }
    }

    companion object {
        private const val TAG = "OfflineWifiDrop"

        /** Each queued mutation sleeps its own backoff, so a backlog drains slowly. */
        private const val DRAIN_TIMEOUT_MS = 6L * 60_000L

        @JvmStatic
        @AfterClass
        fun finishInstrumentation() {
            InstrumentationRegistry.getInstrumentation().finish(Activity.RESULT_OK, Bundle())
        }
    }
}
