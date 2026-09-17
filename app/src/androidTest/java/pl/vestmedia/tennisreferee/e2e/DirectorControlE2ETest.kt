package pl.vestmedia.tennisreferee.e2e

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.containsString
import org.json.JSONObject
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Live match on court 1, then director rename + court move — the Vilnius tablet case.
 *
 * Instrumentation args: `e2e.baseUrl`, `e2e.adminPassword` (same as MultiCourt).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DirectorControlE2ETest {

    private val backend = E2EBackendClient()
    private lateinit var fixture: TournamentFixture

    @Before
    fun setUp() {
        RetrofitClient.overrideBaseUrl(backend.baseUrl)
        val marker = "E2E-${System.currentTimeMillis()}-dir"
        backend.cleanup(marker)
        fixture = backend.createTournamentFixture(marker, publicOverlay = true)
        runBlocking {
            ApplicationProvider.getApplicationContext<TennisRefereeApp>()
                .matchHistoryRepository
                .deleteAllMatches()
        }
    }

    @After
    fun tearDown() {
        try {
            if (::fixture.isInitialized) {
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
    fun directorRenamesAndMovesLiveMatch() {
        val matchScenario = scenario(
            name = "director_rename_and_court_move",
            playerIndexes = listOf(0, 1),
            config = MatchConfig(gamesPerSet = 4, setsToWin = 1, statsMode = StatsMode.ADVANCED),
            steps = emptyList(),
            expectedSets = 0 to 0,
            expectedSetScores = emptyList()
        )
        val matchState = matchScenario.toMatchState(fixture, courtIndex = 0)
        val court1 = fixture.courtIdFor(0)
        val court2 = fixture.courtIdFor(1)
        val originalP1 = matchState.getTeam1FullName()
        val originalP2 = matchState.getTeam2FullName()
        backend.seedAppCourtSession(court1)

        var matchId = 0
        ActivityScenario.launch<MatchActivity>(intentFor(matchState)).use {
            it.onActivity { activity ->
                val firstServerButton = activity.findViewById<View>(R.id.buttonPlayer1Serves)
                assertTrue(UmpireRobot.debugView(firstServerButton), firstServerButton.isShown)
            }
            UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
            UmpireRobot.clickServerButton(matchScenario.firstServer)

            val umpire = UmpireRobot(doubles = false, firstServer = matchScenario.firstServer)
            umpire.playGame(true)

            val live = backend.waitForMatch(
                marker = fixture.marker,
                description = "in-progress match on $court1"
            ) { row ->
                row.optString("player1_name") == originalP1
                    && row.optString("player2_name") == originalP2
                    && row.optString("status") == "in_progress"
                    && row.optString("court_id") == court1
            }
            matchId = live.getInt("id")

            backend.directorControl(
                matchId,
                JSONObject()
                    .put("court_id", court2)
                    .put("player1_name", "Jessica González")
            )

            waitUntil("scoreboard shows González after director rename", timeoutMs = 25_000) {
                onView(withId(R.id.textPlayer1Name)).check(matches(withText(containsString("González"))))
                true
            }

            backend.waitForCourtSnapshot(
                court2,
                timeoutMs = 20_000,
                description = "overlay court 2 after director move"
            ) { snap ->
                overlayNames(snap).contains("González")
                    && snap.optJSONObject("match_status")?.optBoolean("active") == true
            }

            umpire.playGame(true)
        }

        val moved = backend.waitForMatch(
            marker = fixture.marker,
            description = "match renamed and on $court2"
        ) { row ->
            row.optInt("id") == matchId
                && row.optString("player1_name") == "Jessica González"
                && row.optString("court_id") == court2
        }
        assertEquals("Jessica González", moved.optString("player1_name"))
        assertEquals(court2, moved.optString("court_id"))
        val old = backend.fetchCourtSnapshot(court1)
        assertFalse(
            "court 1 should be released",
            old?.optJSONObject("match_status")?.optBoolean("active") == true
        )
    }

    @Test
    fun directorCorrectsScoreAndRules() {
        val matchScenario = scenario(
            name = "director_score_and_rules",
            playerIndexes = listOf(0, 1),
            config = MatchConfig(gamesPerSet = 4, setsToWin = 1, statsMode = StatsMode.ADVANCED),
            steps = emptyList(),
            expectedSets = 0 to 0,
            expectedSetScores = emptyList()
        )
        val matchState = matchScenario.toMatchState(fixture, courtIndex = 0)
        val court1 = fixture.courtIdFor(0)
        val originalP1 = matchState.getTeam1FullName()
        val originalP2 = matchState.getTeam2FullName()
        backend.seedAppCourtSession(court1)

        ActivityScenario.launch<MatchActivity>(intentFor(matchState)).use {
            UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
            UmpireRobot.clickServerButton(matchScenario.firstServer)
            val umpire = UmpireRobot(doubles = false, firstServer = matchScenario.firstServer)
            umpire.playGame(true)

            val live = backend.waitForMatch(
                marker = fixture.marker,
                description = "in-progress match for score correction"
            ) { row ->
                row.optString("player1_name") == originalP1
                    && row.optString("player2_name") == originalP2
                    && row.optString("status") == "in_progress"
            }

            backend.directorControl(
                live.getInt("id"),
                JSONObject()
                    .put(
                        "score",
                        JSONObject()
                            .put("player1_sets", 0)
                            .put("player2_sets", 0)
                            .put("player1_games", 2)
                            .put("player2_games", 0)
                            .put("player1_points", 2)
                            .put("player2_points", 0)
                    )
                    .put(
                        "match_config",
                        JSONObject()
                            .put("games_per_set", 3)
                            .put("sets_to_win", 1)
                            .put("no_advantage", true)
                    )
            )

            waitUntil("director score 2 games and 30 on the board", timeoutMs = 25_000) {
                onView(withId(R.id.textPlayer1Points)).check(matches(withText("30")))
                onView(withId(R.id.textPlayer1Set1)).check(matches(withText("2")))
                true
            }
        }

        backend.waitForCourtSnapshot(
            court1,
            timeoutMs = 20_000,
            description = "overlay 2:0 30 after director score"
        ) { snap ->
            snap.optJSONObject("A")?.optInt("current_games") == 2
                && snap.optJSONObject("A")?.optString("points") == "30"
        }
    }

    private fun overlayNames(snap: JSONObject): String {
        val side = snap.optJSONObject("A") ?: return ""
        return "${side.optString("full_name")} ${side.optString("surname")}"
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
