package pl.vestmedia.tennisreferee.e2e

import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.hamcrest.Matchers.allOf
import org.json.JSONArray
import org.json.JSONObject
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
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode
import pl.vestmedia.tennisreferee.ui.match.MatchActivity
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
class UmpireTournamentE2ETest {

    private val backend = E2EBackendClient()
    private lateinit var fixture: TournamentFixture

    @Before
    fun setUp() {
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
                                        waitForView(R.id.buttonPlayer1Serves)
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
                waitForView(R.id.buttonPlayer1Serves)
                onView(withId(R.id.buttonPlayer1Serves)).check(matches(isDisplayed()))
            }
            launched++
        }

        assertTrue("Expected configuration matrix to launch at least one scenario", launched > 0)
    }

    private fun runScenario(scenario: Scenario, verifyServerDuringMatch: Boolean) {
        val beforeLocalHistory = runBlocking {
            ApplicationProvider.getApplicationContext<TennisRefereeApp>().matchHistoryRepository.getMatchCount()
        }
        val matchState = scenario.toMatchState(fixture)

        ActivityScenario.launch<MatchActivity>(intentFor(matchState)).use {
            it.onActivity { activity ->
                val firstServerButton = activity.findViewById<View>(R.id.buttonPlayer1Serves)
                assertTrue(debugView(firstServerButton), firstServerButton.isShown)
            }
            waitForView(R.id.buttonPlayer1Serves)
            clickServerButton(scenario.firstServer)

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

            waitForView(R.id.textWinner, timeoutMs = 30_000)
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

    private fun verifyFinishedArtifacts(scenario: Scenario, artifacts: FinishedArtifacts) {
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

    private fun scenario(
        name: String,
        playerIndexes: List<Int>,
        config: MatchConfig,
        steps: List<Step>,
        expectedSets: Pair<Int, Int>,
        expectedSetScores: List<Pair<Int, Int>>,
        isDoubles: Boolean = false,
        isMixedDoubles: Boolean = false,
        startInSuperTiebreak: Boolean = false,
        firstServer: Int = 1,
        expectedLastTiebreakLoserPoints: Int? = null,
        expectedLastSetSuperTiebreak: Boolean = false
    ) = Scenario(
        name = name,
        playerIndexes = playerIndexes,
        isDoubles = isDoubles,
        isMixedDoubles = isMixedDoubles,
        config = config,
        startInSuperTiebreak = startInSuperTiebreak,
        firstServer = firstServer,
        steps = steps,
        expectedSets = expectedSets,
        expectedSetScores = expectedSetScores,
        expectedLastTiebreakLoserPoints = expectedLastTiebreakLoserPoints,
        expectedLastSetSuperTiebreak = expectedLastSetSuperTiebreak
    )

    private fun intentFor(type: MatrixType, config: MatchConfig, suffix: String, startInSuperTiebreak: Boolean = false): Intent {
        val players = when (type) {
            MatrixType.SINGLES -> listOf(0, 1)
            MatrixType.DOUBLES -> listOf(0, 1, 2, 3)
            MatrixType.MIXED -> listOf(0, 5, 2, 7)
        }
        return intentFor(
            Scenario(
                name = suffix,
                playerIndexes = players,
                isDoubles = type != MatrixType.SINGLES,
                isMixedDoubles = type == MatrixType.MIXED,
                config = config,
                startInSuperTiebreak = startInSuperTiebreak,
                firstServer = 1,
                steps = emptyList(),
                expectedSets = 0 to 0,
                expectedSetScores = emptyList(),
                expectedLastTiebreakLoserPoints = null,
                expectedLastSetSuperTiebreak = false
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

    private fun clickServerButton(server: Int) {
        val buttonId = when (server) {
            2 -> R.id.buttonPlayer2Serves
            3 -> R.id.buttonPlayer3Serves
            4 -> R.id.buttonPlayer4Serves
            else -> R.id.buttonPlayer1Serves
        }
        waitForView(buttonId)
        onView(withId(buttonId)).perform(click())
    }

    private class UmpireRobot(
        private val doubles: Boolean,
        firstServer: Int
    ) {
        private var serverSlot = firstServer
        private var completedGames = 0
        private var sidesSwapped = false

        fun playGame(team1Wins: Boolean) {
            repeat(4) { playNormalPoint(team1Wins) }
            completeGame()
            dismissAnnouncementIfVisible()
        }

        fun playDeuceGame(team1Wins: Boolean, noAdvantage: Boolean) {
            listOf(true, true, true, false, false, false).forEach { playNormalPoint(it) }
            dismissAnnouncementIfVisible()
            if (noAdvantage) {
                playNormalPoint(team1Wins)
            } else {
                playNormalPoint(team1Wins)
                playNormalPoint(!team1Wins)
                playNormalPoint(team1Wins)
                playNormalPoint(team1Wins)
            }
            completeGame()
            dismissAnnouncementIfVisible()
        }

        fun playTiebreak(points: List<Boolean>) {
            points.forEachIndexed { index, team1Wins ->
                playPoint(team1Wins, tiebreakPointIndex = index + 1)
                dismissAnnouncementIfVisible()
            }
        }

        private fun playNormalPoint(team1Wins: Boolean) {
            playPoint(team1Wins, tiebreakPointIndex = null)
        }

        private fun playPoint(team1Wins: Boolean, tiebreakPointIndex: Int?) {
            dismissDialogIfVisible()
            dismissAnnouncementIfVisible()
            selectServerIfVisible()
            val serverTeam1 = serverSlot == 1 || (doubles && serverSlot == 3)
            if (team1Wins == serverTeam1) {
                clickFirstDisplayed(R.id.buttonAceLeft, R.id.buttonAceRight)
            } else {
                clickFirstDisplayed(R.id.buttonFaultLeft, R.id.buttonFaultRight)
                clickFirstDisplayed(R.id.buttonFaultLeft, R.id.buttonFaultRight)
            }

            if (tiebreakPointIndex != null && tiebreakPointIndex % 2 == 1) {
                advanceServer()
            }
        }

        private fun completeGame() {
            completedGames++
            sidesSwapped = completedGames % 2 == 1
            advanceServer()
        }

        private fun advanceServer() {
            serverSlot = if (doubles) {
                when (serverSlot) {
                    1 -> 2
                    2 -> 3
                    3 -> 4
                    else -> 1
                }
            } else {
                if (serverSlot == 1) 2 else 1
            }
        }

        private fun selectServerIfVisible() {
            val buttonId = if (doubles) {
                when {
                    !sidesSwapped && serverSlot == 1 -> R.id.buttonPlayer1Serves
                    !sidesSwapped && serverSlot == 2 -> R.id.buttonPlayer2Serves
                    !sidesSwapped && serverSlot == 3 -> R.id.buttonPlayer3Serves
                    !sidesSwapped && serverSlot == 4 -> R.id.buttonPlayer4Serves
                    sidesSwapped && serverSlot == 1 -> R.id.buttonPlayer2Serves
                    sidesSwapped && serverSlot == 2 -> R.id.buttonPlayer1Serves
                    sidesSwapped && serverSlot == 3 -> R.id.buttonPlayer4Serves
                    else -> R.id.buttonPlayer3Serves
                }
            } else {
                when {
                    !sidesSwapped && serverSlot == 1 -> R.id.buttonPlayer1Serves
                    !sidesSwapped && serverSlot == 2 -> R.id.buttonPlayer2Serves
                    sidesSwapped && serverSlot == 1 -> R.id.buttonPlayer2Serves
                    else -> R.id.buttonPlayer1Serves
                }
            }
            try {
                onView(allOf(withId(buttonId), isDisplayed())).perform(click())
            } catch (_: Throwable) {
                // Already on the serve/rally/basic scoring screen.
            }
        }
    }

    private data class Scenario(
        val name: String,
        val playerIndexes: List<Int>,
        val isDoubles: Boolean,
        val isMixedDoubles: Boolean,
        val config: MatchConfig,
        val startInSuperTiebreak: Boolean,
        val firstServer: Int,
        val steps: List<Step>,
        val expectedSets: Pair<Int, Int>,
        val expectedSetScores: List<Pair<Int, Int>>,
        val expectedLastTiebreakLoserPoints: Int?,
        val expectedLastSetSuperTiebreak: Boolean
    ) {
        fun toMatchState(fixture: TournamentFixture): MatchState {
            val selected = playerIndexes.map { fixture.players[it] }
            val state = if (isDoubles) {
                MatchState(
                    player1 = selected[0].toPlayer(),
                    player2 = selected[2].toPlayer(),
                    player3 = selected[1].toPlayer(),
                    player4 = selected[3].toPlayer(),
                    courtId = fixture.nextCourtId(),
                    courtName = "E2E Court ${fixture.courtOrdinal}",
                    isDoubles = true,
                    isMixedDoubles = isMixedDoubles,
                    umpireName = "E2E Umpire ${fixture.marker}",
                    currentServer = firstServer,
                    statsMode = config.statsMode,
                    noAdvantage = config.noAdvantage,
                    matchConfig = config
                )
            } else {
                MatchState(
                    player1 = selected[0].toPlayer(),
                    player2 = selected[1].toPlayer(),
                    courtId = fixture.nextCourtId(),
                    courtName = "E2E Court ${fixture.courtOrdinal}",
                    umpireName = "E2E Umpire ${fixture.marker}",
                    currentServer = firstServer,
                    statsMode = config.statsMode,
                    noAdvantage = config.noAdvantage,
                    matchConfig = config
                )
            }
            if (startInSuperTiebreak) {
                state.isSuperTiebreak = true
            }
            return state
        }
    }

    private sealed interface Step
    private data class Game(val team1Wins: Boolean) : Step
    private data class DeuceGame(val team1Wins: Boolean, val noAdvantage: Boolean) : Step
    private data class Tiebreak(val points: List<Boolean>) : Step

    private enum class MatrixType { SINGLES, DOUBLES, MIXED }

    private data class TournamentFixture(
        val marker: String,
        val tournamentId: Int,
        val players: List<E2EPlayer>
    ) {
        var courtOrdinal: Int = 0
            private set

        fun nextCourtId(): String {
            courtOrdinal += 1
            return "t$tournamentId-$courtOrdinal"
        }
    }

    private data class E2EPlayer(
        val id: Int,
        val firstName: String,
        val lastName: String,
        val gender: String,
        val country: String
    ) {
        val fullName: String = "$firstName $lastName"

        fun toPlayer(): Player = Player(
            id = id,
            name = fullName,
            firstName = firstName,
            lastName = lastName,
            flag = country,
            group = "E2E",
            gender = gender
        )
    }

    private data class FinishedArtifacts(
        val match: JSONObject,
        val history: JSONObject,
        val statistics: JSONObject
    )

    private class E2EBackendClient {
        private val baseUrl = "https://score.vestmedia.pl"
        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        fun createTournamentFixture(marker: String): TournamentFixture {
            val today = LocalDate.now()
            try {
                val tournament = postJson(
                    "/admin/api/tournaments",
                    JSONObject()
                        .put("name", "$marker Android Emulator Open")
                        .put("start_date", today.toString())
                        .put("end_date", today.plusDays(1).toString())
                        .put("active", true)
                        .put("city", "E2E")
                        .put("country", "PL")
                        .put("court_count", 8)
                )
                val tournamentId = tournament.getInt("id")
                val players = createPlayers(marker, tournamentId)
                putJson(
                    "/admin/api/tournaments/$tournamentId/bracket/groups",
                    JSONObject().put(
                        "groups",
                        JSONArray()
                            .put(JSONObject().put("name", "Group A").put("players", JSONArray(listOf(players[0].id, players[1].id, players[2].id, players[3].id))))
                            .put(JSONObject().put("name", "Group B").put("players", JSONArray(listOf(players[4].id, players[5].id, players[6].id, players[7].id))))
                    )
                )
                putJson(
                    "/admin/api/tournaments/$tournamentId/bracket/knockout",
                    JSONObject().put(
                        "knockout",
                        JSONArray()
                            .put(JSONObject().put("phase", "semifinal").put("position", 1).put("player1_name", players[0].fullName).put("player2_name", players[4].fullName))
                            .put(JSONObject().put("phase", "semifinal").put("position", 2).put("player1_name", players[1].fullName).put("player2_name", players[5].fullName))
                            .put(JSONObject().put("phase", "final").put("position", 1))
                            .put(JSONObject().put("phase", "third_place").put("position", 1))
                    )
                )
                return TournamentFixture(marker, tournamentId, players)
            } catch (error: Throwable) {
                cleanup(marker)
                throw error
            }
        }

        fun cleanup(marker: String) {
            postJson("/admin/api/e2e/cleanup", JSONObject().put("marker", marker), allowFailure = true)
        }

        fun close() {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            client.cache?.close()
        }

        fun waitForInProgressMatch(marker: String, player1Name: String, player2Name: String) {
            waitUntil("server in-progress match for $player1Name vs $player2Name", timeoutMs = 30_000) {
                val match = findMatch(fetchArtifacts(marker).getJSONArray("matches"), player1Name, player2Name)
                match?.optString("status") == "in_progress"
            }
        }

        fun waitForFinishedArtifacts(marker: String, player1Name: String, player2Name: String): FinishedArtifacts {
            var result: FinishedArtifacts? = null
            waitUntil("finished server artifacts for $player1Name vs $player2Name", timeoutMs = 60_000) {
                val artifacts = fetchArtifacts(marker)
                val match = findMatch(artifacts.getJSONArray("matches"), player1Name, player2Name)
                if (match?.optString("status") != "finished") return@waitUntil false
                val matchId = match.getInt("id")
                val history = findByMatchId(artifacts.getJSONArray("history"), matchId) ?: return@waitUntil false
                val statistics = findByMatchId(artifacts.getJSONArray("statistics"), matchId) ?: return@waitUntil false
                result = FinishedArtifacts(match, history, statistics)
                true
            }
            return requireNotNull(result)
        }

        private fun createPlayers(marker: String, tournamentId: Int): List<E2EPlayer> {
            val definitions = listOf(
                Triple("Ana", "F", "PL"),
                Triple("Bartosz", "M", "PL"),
                Triple("Celina", "F", "CZ"),
                Triple("Dominik", "M", "DE"),
                Triple("Elena", "F", "ES"),
                Triple("Filip", "M", "FR"),
                Triple("Gaja", "F", "IT"),
                Triple("Hubert", "M", "GB")
            )
            return definitions.mapIndexed { index, definition ->
                val firstName = definition.first
                val lastName = "$marker-P${index + 1}"
                val response = postJson(
                    "/admin/api/tournaments/$tournamentId/players",
                    JSONObject()
                        .put("first_name", firstName)
                        .put("last_name", lastName)
                        .put("name", "$firstName $lastName")
                        .put("gender", definition.second)
                        .put("country", definition.third)
                        .put("category", "E2E")
                )
                E2EPlayer(response.getInt("id"), firstName, lastName, definition.second, definition.third)
            }
        }

        private fun fetchArtifacts(marker: String): JSONObject {
            return getJson("/admin/api/e2e/artifacts?marker=${URLEncoder.encode(marker, "UTF-8")}")
        }

        private fun findMatch(matches: JSONArray, player1Name: String, player2Name: String): JSONObject? {
            for (index in 0 until matches.length()) {
                val match = matches.getJSONObject(index)
                if (match.optString("player1_name") == player1Name && match.optString("player2_name") == player2Name) {
                    return match
                }
            }
            return null
        }

        private fun findByMatchId(items: JSONArray, matchId: Int): JSONObject? {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                if (item.optInt("match_id") == matchId) {
                    return item
                }
            }
            return null
        }

        private fun getJson(path: String): JSONObject = request("GET", path, null)
        private fun postJson(path: String, body: JSONObject, allowFailure: Boolean = false): JSONObject = request("POST", path, body, allowFailure)
        private fun putJson(path: String, body: JSONObject): JSONObject = request("PUT", path, body)

        private fun request(method: String, path: String, body: JSONObject?, allowFailure: Boolean = false): JSONObject {
            val request = Request.Builder()
                .url("$baseUrl$path")
                .method(method, if (method == "GET") null else (body ?: JSONObject()).toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!allowFailure && !response.isSuccessful) {
                    throw AssertionError("$method $path failed: HTTP ${response.code} $text")
                }
                return if (text.isBlank()) JSONObject() else JSONObject(text)
            }
        }
    }

    companion object {
        @JvmStatic
        @AfterClass
        fun finishInstrumentation() {
            InstrumentationRegistry.getInstrumentation().finish(Activity.RESULT_OK, Bundle())
        }

        private fun clickFirstDisplayed(vararg ids: Int) {
            waitUntil("one of ${ids.joinToString()} displayed", timeoutMs = 10_000) {
                ids.any { id ->
                    try {
                        onView(allOf(withId(id), isDisplayed())).perform(click())
                        true
                    } catch (_: Throwable) {
                        false
                    }
                }
            }
        }

        private fun dismissAnnouncementIfVisible() {
            clickIfDisplayed(R.id.buttonAnnouncementContinue, timeoutMs = 2_000)
        }

        private fun dismissDialogIfVisible() {
            try {
                onView(allOf(withText("OK"), isDisplayed())).perform(click())
            } catch (_: Throwable) {
                // No blocking dialog is currently visible.
            }
        }

        private fun waitForView(id: Int, timeoutMs: Long = 10_000) {
            waitUntil("view $id displayed", timeoutMs) {
                onView(allOf(withId(id), isDisplayed())).check(matches(isDisplayed()))
                true
            }
        }

        private fun clickIfDisplayed(id: Int, timeoutMs: Long = 1_000): Boolean {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                try {
                    onView(allOf(withId(id), isDisplayed())).perform(click())
                    return true
                } catch (_: Throwable) {
                    Thread.sleep(100)
                }
            }
            return false
        }

        private fun waitUntil(description: String, timeoutMs: Long, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMs
            var lastError: Throwable? = null
            while (System.currentTimeMillis() < deadline) {
                try {
                    if (condition()) return
                } catch (error: Throwable) {
                    lastError = error
                }
                Thread.sleep(250)
            }
            throw AssertionError("Timed out waiting for $description\n${activeWindowDump()}", lastError)
        }

        private fun activeWindowDump(): String {
            return try {
                val output = ByteArrayOutputStream()
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).dumpWindowHierarchy(output)
                output.toString(Charsets.UTF_8.name()).take(30_000)
            } catch (error: Throwable) {
                "Unable to dump active window: ${error.message}"
            }
        }

        private fun debugView(view: View): String {
            val chain = generateSequence(view as View?) { it.parent as? View }
                .joinToString(" <- ") { parent ->
                    val resourceName = runCatching { parent.resources.getResourceEntryName(parent.id) }.getOrDefault("no-id")
                    val visibility = when (parent.visibility) {
                        View.VISIBLE -> "VISIBLE"
                        View.INVISIBLE -> "INVISIBLE"
                        View.GONE -> "GONE"
                        else -> parent.visibility.toString()
                    }
                    val childCount = (parent as? ViewGroup)?.childCount?.let { " children=$it" }.orEmpty()
                    "${parent.javaClass.simpleName}#$resourceName visibility=$visibility shown=${parent.isShown} alpha=${parent.alpha} size=${parent.width}x${parent.height}$childCount"
                }
            return "Expected first server button to be shown. $chain"
        }
    }
}