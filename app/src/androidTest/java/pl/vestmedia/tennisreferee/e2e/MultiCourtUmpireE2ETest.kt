package pl.vestmedia.tennisreferee.e2e

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode
import pl.vestmedia.tennisreferee.ui.match.MatchActivity

/**
 * One short singles match on an assigned court (0–3).
 *
 * Instrumentation args:
 * - `e2e.courtIndex` (0-3, default 0)
 * - `e2e.marker` + `e2e.tournamentId` — join a shared fixture (parallel multi-device)
 * - `e2e.baseUrl` — backend URL (default emulator→host Docker on 18087)
 *
 * Without marker/tournamentId, creates a private fixture (single-device fallback).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MultiCourtUmpireE2ETest {

    private val backend = E2EBackendClient()
    private lateinit var fixture: TournamentFixture
    private var ownsFixture: Boolean = false
    private var courtIndex: Int = 0

    @Before
    fun setUp() {
        courtIndex = E2EBackendClient.instrumentationArg("e2e.courtIndex")?.toIntOrNull() ?: 0
        require(courtIndex in 0..3) { "e2e.courtIndex must be 0..3, got $courtIndex" }

        // App sync (Retrofit) must hit the same host as the E2E admin client — not production.
        RetrofitClient.overrideBaseUrl(backend.baseUrl)

        val sharedMarker = E2EBackendClient.instrumentationArg("e2e.marker")
        val sharedTournamentId = E2EBackendClient.instrumentationArg("e2e.tournamentId")?.toIntOrNull()

        if (sharedMarker != null && sharedTournamentId != null) {
            fixture = backend.loadTournamentFixture(sharedMarker, sharedTournamentId)
            ownsFixture = false
        } else {
            val marker = sharedMarker ?: "E2E-${System.currentTimeMillis()}-c$courtIndex"
            backend.cleanup(marker)
            fixture = backend.createTournamentFixture(marker)
            ownsFixture = true
        }

        runBlocking {
            ApplicationProvider.getApplicationContext<TennisRefereeApp>()
                .matchHistoryRepository
                .deleteAllMatches()
        }
    }

    @After
    fun tearDown() {
        try {
            if (ownsFixture && ::fixture.isInitialized) {
                backend.cleanup(fixture.marker)
            }
            runBlocking {
                ApplicationProvider.getApplicationContext<TennisRefereeApp>()
                    .matchHistoryRepository
                    .deleteAllMatches()
            }
        } finally {
            RetrofitClient.overrideBaseUrl(null)
            backend.close()
        }
    }

    @Test
    fun shortMatch_onAssignedCourt() {
        val p1 = courtIndex * 2
        val p2 = p1 + 1
        val matchScenario = scenario(
            name = "court_${courtIndex}_singles_3_0",
            playerIndexes = listOf(p1, p2),
            config = MatchConfig(gamesPerSet = 3, setsToWin = 1, statsMode = StatsMode.ADVANCED),
            steps = listOf(Game(true), Game(true), Game(true)),
            expectedSets = 1 to 0,
            expectedSetScores = listOf(3 to 0)
        )
        val matchState = matchScenario.toMatchState(fixture, courtIndex = courtIndex)

        ActivityScenario.launch<MatchActivity>(intentFor(matchState)).use {
            it.onActivity { activity ->
                val firstServerButton = activity.findViewById<View>(R.id.buttonPlayer1Serves)
                assertTrue(UmpireRobot.debugView(firstServerButton), firstServerButton.isShown)
            }
            UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
            UmpireRobot.clickServerButton(matchScenario.firstServer)

            val umpire = UmpireRobot(doubles = false, firstServer = matchScenario.firstServer)
            matchScenario.steps.forEach { step ->
                when (step) {
                    is Game -> umpire.playGame(step.team1Wins)
                    is DeuceGame -> umpire.playDeuceGame(step.team1Wins, step.noAdvantage)
                    is Tiebreak -> umpire.playTiebreak(step.points)
                }
            }

            UmpireRobot.waitForView(R.id.textWinner, timeoutMs = 30_000)
            onView(withId(R.id.textWinner)).check(matches(isDisplayed()))
        }

        val artifacts = backend.waitForFinishedArtifacts(
            marker = fixture.marker,
            player1Name = matchState.getTeam1FullName(),
            player2Name = matchState.getTeam2FullName()
        )
        val score = artifacts.match.getJSONObject("score")
        assertEquals(1, score.getInt("player1_sets"))
        assertEquals(0, score.getInt("player2_sets"))
        assertEquals("finished", artifacts.match.getString("status"))
        val expectedCourtId = fixture.courtIdFor(courtIndex)
        assertEquals(expectedCourtId, artifacts.match.optString("court_id"))
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

    companion object {
        @JvmStatic
        @AfterClass
        fun finishInstrumentation() {
            InstrumentationRegistry.getInstrumentation().finish(Activity.RESULT_OK, Bundle())
        }
    }
}
