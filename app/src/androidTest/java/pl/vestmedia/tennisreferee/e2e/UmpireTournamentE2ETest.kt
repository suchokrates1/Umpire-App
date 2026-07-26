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
import org.junit.Assert.assertNotNull
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

@RunWith(AndroidJUnit4::class)
@LargeTest
class UmpireTournamentE2ETest {

    private val backend = E2EBackendClient()
    private lateinit var fixture: TournamentFixture

    @Before
    fun setUp() {
        RetrofitClient.overrideBaseUrl(backend.baseUrl)
        val marker = "E2E-${System.currentTimeMillis()}"
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
    fun tournamentSimulation_coversUmpireFlowsServerSyncHistoryAndCleanup() {
        val scenarios = listOf(
            scenario(
                name = "singles_advantage_mini_set_3_0",
                playerIndexes = listOf(0, 1),
                config = MatchConfig(gamesPerSet = 3, setsToWin = 1, statsMode = StatsMode.ADVANCED),
                steps = listOf(Game(true), Game(true), Game(true)),
                expectedSets = 1 to 0,
                expectedSetScores = listOf(3 to 0)
            ),
            scenario(
                name = "singles_no_ad_golden_point",
                playerIndexes = listOf(2, 3),
                config = MatchConfig(gamesPerSet = 3, setsToWin = 1, noAdvantage = true, statsMode = StatsMode.ADVANCED),
                steps = listOf(DeuceGame(team1Wins = false, noAdvantage = true), Game(false), Game(false)),
                expectedSets = 0 to 1,
                expectedSetScores = listOf(0 to 3)
            ),
            scenario(
                name = "singles_short_set_tiebreak_7_5",
                playerIndexes = listOf(4, 5),
                config = MatchConfig(gamesPerSet = 3, setsToWin = 1, tiebreakPoints = 7, statsMode = StatsMode.ADVANCED),
                steps = listOf(
                    Game(true), Game(false), Game(true), Game(false),
                    Tiebreak(points = listOf(true, false, true, false, true, false, true, false, true, false, true, true))
                ),
                expectedSets = 1 to 0,
                expectedSetScores = listOf(3 to 2),
                expectedLastTiebreakLoserPoints = 5
            ),
            scenario(
                name = "singles_super_tiebreak_10_8",
                playerIndexes = listOf(6, 7),
                config = MatchConfig(gamesPerSet = 3, setsToWin = 2, superTiebreakPoints = 10, statsMode = StatsMode.ADVANCED),
                steps = listOf(
                    Game(true), Game(true), Game(true),
                    Game(false), Game(false), Game(false),
                    Tiebreak(points = listOf(true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, true))
                ),
                expectedSets = 2 to 1,
                expectedSetScores = listOf(3 to 0, 0 to 3, 10 to 8),
                expectedLastTiebreakLoserPoints = 8,
                expectedLastSetSuperTiebreak = true
            ),
            scenario(
                name = "doubles_regular",
                playerIndexes = listOf(0, 1, 2, 3),
                isDoubles = true,
                config = MatchConfig(gamesPerSet = 3, setsToWin = 1, statsMode = StatsMode.ADVANCED),
                steps = listOf(Game(true), Game(true), Game(true)),
                expectedSets = 1 to 0,
                expectedSetScores = listOf(3 to 0)
            ),
            scenario(
                name = "mixed_doubles_tiebreak_only_12_10",
                playerIndexes = listOf(0, 5, 2, 7),
                isDoubles = true,
                isMixedDoubles = true,
                config = MatchConfig.tiebreakOnly(points = 10).copy(statsMode = StatsMode.ADVANCED),
                startInSuperTiebreak = true,
                steps = listOf(
                    Tiebreak(
                        points = listOf(
                            true, false, true, false, true, false, true, false, true, false,
                            true, false, true, false, true, false, true, false, true, false,
                            true, true
                        )
                    )
                ),
                expectedSets = 1 to 0,
                expectedSetScores = listOf(12 to 10),
                expectedLastTiebreakLoserPoints = 10,
                expectedLastSetSuperTiebreak = true
            )
        )

        scenarios.forEach { runScenario(it, verifyServerDuringMatch = it.steps.any { step -> step is Game }) }
    }

    @Test
    fun configurationMatrix_allSupportedSettingsStartOnEmulator() {
        val exhaustive = InstrumentationRegistry.getArguments().getString("e2e.exhaustive") == "true"
        val gamesPerSet = if (exhaustive) listOf(3, 4, 5, 6) else listOf(3, 6)
        val setsToWin = if (exhaustive) listOf(1, 2, 3) else listOf(1, 2)
        val tiebreakPoints = if (exhaustive) listOf(7, 10) else listOf(7)
        val superTiebreakPoints = if (exhaustive) listOf(7, 10) else listOf(10)
        val noAdvantageValues = if (exhaustive) listOf(false, true) else listOf(false, true)
        val statsModes = if (exhaustive) listOf(StatsMode.BASIC, StatsMode.ADVANCED) else listOf(StatsMode.BASIC)
        val matchTypes = if (exhaustive) listOf(MatrixType.SINGLES, MatrixType.DOUBLES, MatrixType.MIXED) else listOf(MatrixType.SINGLES, MatrixType.MIXED)

        var launched = 0
        matchTypes.forEach { type ->
            gamesPerSet.forEach { gps ->
                setsToWin.forEach { stw ->
                    tiebreakPoints.forEach { tb ->
                        superTiebreakPoints.forEach { stb ->
                            noAdvantageValues.forEach { noAd ->
                                statsModes.forEach { mode ->
                                    val config = MatchConfig(
                                        gamesPerSet = gps,
                                        setsToWin = stw,
                                        tiebreakPoints = tb,
                                        superTiebreakPoints = stb,
                                        noAdvantage = noAd,
                                        statsMode = mode
                                    )
                                    ActivityScenario.launch<MatchActivity>(intentFor(type, config, "$type-$gps-$stw-$tb-$stb-$noAd-$mode")).use {
                                        UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
                                        onView(withId(R.id.buttonPlayer1Serves)).check(matches(isDisplayed()))
                                    }
                                    launched++
                                }
                            }
                        }
                    }
                }
            }
        }

        val tbOnlyConfigs = listOf(7, 10).filter { exhaustive || it == 10 }
        tbOnlyConfigs.forEach { points ->
            ActivityScenario.launch<MatchActivity>(
                intentFor(MatrixType.MIXED, MatchConfig.tiebreakOnly(points).copy(statsMode = StatsMode.ADVANCED), "tb-only-$points", startInSuperTiebreak = true)
            ).use {
                UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
                onView(withId(R.id.buttonPlayer1Serves)).check(matches(isDisplayed()))
            }
            launched++
        }

        assertTrue("Expected configuration matrix to launch at least one scenario", launched > 0)
    }

    private fun runScenario(scenario: MatchScenario, verifyServerDuringMatch: Boolean) {
        val beforeLocalHistory = runBlocking {
            ApplicationProvider.getApplicationContext<TennisRefereeApp>().matchHistoryRepository.getMatchCount()
        }
        val matchState = scenario.toMatchState(fixture)

        ActivityScenario.launch<MatchActivity>(intentFor(matchState)).use {
            it.onActivity { activity ->
                val firstServerButton = activity.findViewById<View>(R.id.buttonPlayer1Serves)
                assertTrue(UmpireRobot.debugView(firstServerButton), firstServerButton.isShown)
            }
            UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
            UmpireRobot.clickServerButton(scenario.firstServer)

            val umpire = UmpireRobot(scenario.isDoubles, scenario.firstServer)
            var liveVerified = false
            scenario.steps.forEach { step ->
                when (step) {
                    is Game -> {
                        umpire.playGame(step.team1Wins)
                        if (verifyServerDuringMatch && !liveVerified) {
                            backend.waitForInProgressMatch(fixture.marker, matchState.getTeam1FullName(), matchState.getTeam2FullName())
                            liveVerified = true
                        }
                    }
                    is DeuceGame -> umpire.playDeuceGame(step.team1Wins, step.noAdvantage)
                    is Tiebreak -> umpire.playTiebreak(step.points)
                }
            }

            UmpireRobot.waitForView(R.id.textWinner, timeoutMs = 30_000)
            onView(withId(R.id.textWinner)).check(matches(isDisplayed()))
        }

        waitUntil("local history saved for ${scenario.name}", timeoutMs = 20_000) {
            runBlocking {
                ApplicationProvider.getApplicationContext<TennisRefereeApp>()
                    .matchHistoryRepository
                    .getMatchCount() > beforeLocalHistory
            }
        }

        val artifacts = backend.waitForFinishedArtifacts(
            marker = fixture.marker,
            player1Name = matchState.getTeam1FullName(),
            player2Name = matchState.getTeam2FullName()
        )
        verifyFinishedArtifacts(scenario, artifacts)
    }

    private fun verifyFinishedArtifacts(scenario: MatchScenario, artifacts: FinishedArtifacts) {
        val score = artifacts.match.getJSONObject("score")
        assertEquals("${scenario.name} player1 sets", scenario.expectedSets.first, score.getInt("player1_sets"))
        assertEquals("${scenario.name} player2 sets", scenario.expectedSets.second, score.getInt("player2_sets"))
        assertEquals("finished", artifacts.match.getString("status"))

        val setsHistory = score.getJSONArray("sets_history")
        assertEquals("${scenario.name} sets_history size", scenario.expectedSetScores.size, setsHistory.length())
        scenario.expectedSetScores.forEachIndexed { index, expected ->
            val set = setsHistory.getJSONObject(index)
            assertEquals("${scenario.name} set ${index + 1} p1", expected.first, set.getInt("player1_games"))
            assertEquals("${scenario.name} set ${index + 1} p2", expected.second, set.getInt("player2_games"))
        }
        scenario.expectedLastTiebreakLoserPoints?.let {
            assertEquals(it, setsHistory.getJSONObject(setsHistory.length() - 1).getInt("tiebreak_loser_points"))
        }
        assertEquals(
            scenario.expectedLastSetSuperTiebreak,
            setsHistory.getJSONObject(setsHistory.length() - 1).optBoolean("is_super_tiebreak", false)
        )

        assertEquals(artifacts.match.getInt("id"), artifacts.history.getInt("match_id"))
        assertEquals(scenario.config.statsMode.name, artifacts.statistics.getString("stats_mode"))
        assertTrue(artifacts.statistics.getLong("match_duration_ms") >= 0L)
        assertNotNull(artifacts.statistics.getString("winner"))
    }

    private fun intentFor(type: MatrixType, config: MatchConfig, suffix: String, startInSuperTiebreak: Boolean = false): Intent {
        val players = when (type) {
            MatrixType.SINGLES -> listOf(0, 1)
            MatrixType.DOUBLES -> listOf(0, 1, 2, 3)
            MatrixType.MIXED -> listOf(0, 5, 2, 7)
        }
        return intentFor(
            scenario(
                name = suffix,
                playerIndexes = players,
                isDoubles = type != MatrixType.SINGLES,
                isMixedDoubles = type == MatrixType.MIXED,
                config = config,
                startInSuperTiebreak = startInSuperTiebreak,
                firstServer = 1,
                steps = emptyList(),
                expectedSets = 0 to 0,
                expectedSetScores = emptyList()
            ).toMatchState(fixture)
        )
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
