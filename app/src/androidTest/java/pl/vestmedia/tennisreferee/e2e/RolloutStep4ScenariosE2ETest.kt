package pl.vestmedia.tennisreferee.e2e

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.containsString
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
import pl.vestmedia.tennisreferee.data.auth.CourtSession
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.model.CourtPinFailure
import pl.vestmedia.tennisreferee.domain.match.MatchResume
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode
import pl.vestmedia.tennisreferee.ui.courtselection.CourtPinDialogController
import pl.vestmedia.tennisreferee.ui.courtselection.CourtSelectionActivity
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.ui.match.ActiveMatchStore
import pl.vestmedia.tennisreferee.ui.match.MatchActivity
import pl.vestmedia.tennisreferee.ui.playerselection.PlayerSelectionActivity
import pl.vestmedia.tennisreferee.ui.tournamentselection.TournamentSelectionStore

/**
 * End-to-end verification of all Step 4 rollout scenarios on the Android emulator
 * against a live deployed backend (`test.blindtennis.app` or production).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class RolloutStep4ScenariosE2ETest {

    private val backend = E2EBackendClient()
    private lateinit var fixture: TournamentFixture
    private val app: TennisRefereeApp
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        setAirplane(false)
        overrideUmpireBackend(backend.baseUrl)
        LanguageSelectionActivity.setLanguage(app, "en")
        val marker = "E2E-${System.currentTimeMillis()}-step4"
        backend.cleanup(marker)
        fixture = backend.createTournamentFixture(marker, publicOverlay = true)
        TournamentSelectionStore.saveSelection(app, pl.vestmedia.tennisreferee.data.model.TournamentOption(id = fixture.tournamentId, name = "Step4 Open"))
        runBlocking {
            app.matchHistoryRepository.deleteAllMatches()
            clearOutbox()
        }
        clearActiveMatchPrefs()
    }

    @After
    fun tearDown() {
        try {
            setAirplane(false)
            if (::fixture.isInitialized) backend.cleanup(fixture.marker)
            runBlocking {
                app.matchHistoryRepository.deleteAllMatches()
                clearOutbox()
            }
            clearActiveMatchPrefs()
        } finally {
            overrideUmpireBackend(null)
            backend.close()
        }
    }

    /**
     * Scenarios 1 & 2:
     * 1) Match finished in airplane mode -> network returns -> score, finish and stats arrive, 1 history row.
     * 2) Match finished offline, umpire already back on PlayerSelection -> network returns -> arrives without touching tablet.
     */
    @Test
    fun offlineFinishDrainsOnMatchScreenAndFromPlayerSelectionScreen() {
        val court1 = fixture.courtIdFor(0)
        val court2 = fixture.courtIdFor(1)
        backend.seedAppCourtSession(court1)

        // --- Part 1: Finish in airplane mode, reconnect while still on MatchActivity ---
        val state1 = MatchState(
            player1 = fixture.players[0].toPlayer(),
            player2 = fixture.players[1].toPlayer(),
            courtId = court1,
            courtName = "Court 1",
            statsMode = StatsMode.BASIC,
            matchConfig = MatchConfig(gamesPerSet = 1, setsToWin = 1, statsMode = StatsMode.BASIC),
        )
        ActivityScenario.launch<MatchActivity>(MatchActivity.createStateIntent(app, state1)).use {
            UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
            onView(withId(R.id.buttonPlayer1Serves)).perform(click())
            backend.waitForInProgressMatch(fixture.marker, state1.getTeam1FullName(), state1.getTeam2FullName())

            setAirplane(true)
            try {
                repeat(4) { clickWinLeft() }
                waitUntil("Match finished view shown offline", 10_000) {
                    onView(withId(R.id.buttonNextMatchSameSetup)).check(matches(isDisplayed()))
                    true
                }
            } finally {
                setAirplane(false)
            }

            val art1 = backend.waitForFinishedArtifacts(fixture.marker, state1.getTeam1FullName(), state1.getTeam2FullName())
            assertEquals("finished", art1.match.optString("status"))
            assertEquals(1, art1.match.getJSONObject("score").optInt("player1_sets"))
            assertNotNull(art1.history)
            assertNotNull(art1.statistics)
        }

        // --- Part 2: Finish offline, navigate to PlayerSelectionActivity, then restore network ---
        backend.seedAppCourtSession(court2)
        val state2 = MatchState(
            player1 = fixture.players[2].toPlayer(),
            player2 = fixture.players[3].toPlayer(),
            courtId = court2,
            courtName = "Court 2",
            statsMode = StatsMode.BASIC,
            matchConfig = MatchConfig(gamesPerSet = 1, setsToWin = 1, statsMode = StatsMode.BASIC),
        )
        val playerSelIntent = Intent(app, PlayerSelectionActivity::class.java).apply {
            putExtra(PlayerSelectionActivity.EXTRA_COURT_ID, court2)
            putExtra(PlayerSelectionActivity.EXTRA_COURT_NAME, "2")
        }
        setAirplane(true)
        try {
            ActivityScenario.launch<MatchActivity>(MatchActivity.createStateIntent(app, state2)).use {
                UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
                onView(withId(R.id.buttonPlayer1Serves)).perform(click())
                repeat(4) { clickWinLeft() }
                waitUntil("Second match finished offline", 10_000) {
                    onView(withId(R.id.buttonNextMatchSameSetup)).check(matches(isDisplayed()))
                    true
                }
            }
            ActivityScenario.launch<PlayerSelectionActivity>(playerSelIntent).use {
                Thread.sleep(1_000)
                setAirplane(false)
                // Do not touch the tablet at all — ConnectivityCallback flushes outbox in background!
                val art2 = backend.waitForFinishedArtifacts(fixture.marker, state2.getTeam1FullName(), state2.getTeam2FullName())
                assertEquals("finished", art2.match.optString("status"))
                assertNotNull(art2.history)
                assertNotNull(art2.statistics)
            }
        } finally {
            setAirplane(false)
        }
    }

    /**
     * Scenarios 3, 5 & 7:
     * 5) Undo point -> public / overlay snapshot reverts from 15:0 back to 0:0.
     * 7) Walkover from tablet -> winner recorded with 0:0 sets and empty sets_history on server and in local history.
     * 3) Expired court session mid-match -> finish match -> re-enter PIN -> result arrives on server immediately.
     */
    @Test
    fun undoRevertsOverlayWalkoverHasNoFakeSetsAndExpiredSessionFlushesAfterPin() {
        val court1 = fixture.courtIdFor(0)
        val court2 = fixture.courtIdFor(1)
        backend.seedAppCourtSession(court1)

        // --- Undo + Walkover on Court 1 ---
        val state1 = MatchState(
            player1 = fixture.players[0].toPlayer(),
            player2 = fixture.players[1].toPlayer(),
            courtId = court1,
            courtName = "Court 1",
            statsMode = StatsMode.BASIC,
            matchConfig = MatchConfig(gamesPerSet = 4, setsToWin = 2, statsMode = StatsMode.BASIC),
        )
        ActivityScenario.launch<MatchActivity>(MatchActivity.createStateIntent(app, state1)).use {
            UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
            onView(withId(R.id.buttonPlayer1Serves)).perform(click())
            clickWinLeft()

            backend.waitForCourtSnapshot(court1, 15_000, "overlay shows 15:0") { snap ->
                snap.optJSONObject("A")?.optString("points") == "15"
            }

            // Undo point -> overlay returns to 0:0
            onView(withId(R.id.buttonUndo)).perform(click())
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            backend.waitForCourtSnapshot(court1, 15_000, "overlay reverted to 0:0 after undo") { snap ->
                snap.optJSONObject("A")?.optString("points") == "0"
            }

            // Walkover from tablet -> winner is player 2, no sets/games
            onView(withId(R.id.buttonBack)).perform(click())
            onView(withText(R.string.finish_reason_walkover)).inRoot(isDialog()).perform(click())
            onView(withText(containsString(fixture.players[1].lastName))).inRoot(isDialog()).perform(click())

            waitUntil("Walkover finished screen shown", 10_000) {
                onView(withId(R.id.buttonNextMatchSameSetup)).check(matches(isDisplayed()))
                true
            }
        }

        val woMatch = backend.waitForMatch(fixture.marker, description = "walkover finished on server") { m ->
            m.optString("player1_name") == state1.getTeam1FullName() && m.optString("status") == "finished"
        }
        assertEquals("walkover", woMatch.optString("finish_reason"))
        assertEquals(state1.getTeam2FullName(), woMatch.optString("winner_name"))
        val woScore = woMatch.getJSONObject("score")
        assertEquals(0, woScore.optInt("player1_sets"))
        assertEquals(0, woScore.optInt("player2_sets"))
        assertEquals(0, woScore.getJSONArray("sets_history").length())

        val localHistory = runBlocking { app.matchHistoryRepository.allMatches.first() }
        val localWo = localHistory.first { it.player1.lastName == fixture.players[0].lastName }
        assertEquals(fixture.players[1].id, localWo.winnerId)
        assertEquals(0, localWo.player1Sets)
        assertEquals(0, localWo.player2Sets)
        assertTrue(localWo.setsHistory.isEmpty())

        // --- Expired court session mid-match on Court 2 -> finish -> re-enter PIN -> flushes ---
        backend.seedAppCourtSession(court2, pin = "2468")
        val state2 = MatchState(
            player1 = fixture.players[2].toPlayer(),
            player2 = fixture.players[3].toPlayer(),
            courtId = court2,
            courtName = "Court 2",
            statsMode = StatsMode.BASIC,
            matchConfig = MatchConfig(gamesPerSet = 1, setsToWin = 1, statsMode = StatsMode.BASIC),
        )
        ActivityScenario.launch<MatchActivity>(MatchActivity.createStateIntent(app, state2)).use {
            UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
            onView(withId(R.id.buttonPlayer1Serves)).perform(click())
            backend.waitForInProgressMatch(fixture.marker, state2.getTeam1FullName(), state2.getTeam2FullName())

            // Corrupt the court token to simulate session expiry mid-match while Wi-Fi stays up
            app.container.sessionStore.save(
                CourtSession(courtId = court2, token = "expired-invalid-token", expiresAtMillis = System.currentTimeMillis() + 3_600_000L)
            )
            repeat(4) { clickWinLeft() }
            waitUntil("Match finished with expired session", 10_000) {
                onView(withId(R.id.buttonNextMatchSameSetup)).check(matches(isDisplayed()))
                true
            }
            waitUntil("Outbox holds FAILED_AUTH mutations", 10_000) {
                runBlocking { app.database.outboxMutationDao().getPending().isNotEmpty() }
            }
        }

        // Re-authorize Court 2 via CourtPinDialogController -> flushes immediately!
        ActivityScenario.launch(CourtSelectionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                CourtPinDialogController(activity, app.container.repository()).show(
                    Court(id = court2, name = "2")
                )
            }
            waitUntil("PIN dialog visible", 10_000) {
                onView(withId(R.id.pinDigit1)).check(matches(isDisplayed()))
                true
            }
            onView(withId(R.id.pinDigit1)).perform(replaceText("2"))
            onView(withId(R.id.pinDigit2)).perform(replaceText("4"))
            onView(withId(R.id.pinDigit3)).perform(replaceText("6"))
            onView(withId(R.id.pinDigit4)).perform(replaceText("8"))

            val art2 = backend.waitForFinishedArtifacts(fixture.marker, state2.getTeam1FullName(), state2.getTeam2FullName())
            assertEquals("finished", art2.match.optString("status"))
        }
    }

    /**
     * Scenarios 4, 6 & 8:
     * 6) Wrong format -> Back before first point -> new format takes effect; after first point -> notice that format stays.
     * 8) Director corrects score mid-set (to 2:0 games, 40:0 points) -> next point reaches 3:0 (odd) -> side change triggers!
     * 4) 11 wrong PIN attempts -> localized throttle message with retry minutes!
     */
    @Test
    fun formatResumeNoticeDirectorSideChangeAndPinThrottleToast() {
        val court1 = fixture.courtIdFor(0)
        val court3 = fixture.courtIdFor(2)
        backend.setCourtPin(court3, "9876")
        backend.seedAppCourtSession(court1)

        // --- Scenario 6: Resume format before & after first point ---
        val store = ActiveMatchStore(app)
        val initial = MatchState(
            player1 = fixture.players[4].toPlayer(),
            player2 = fixture.players[5].toPlayer(),
            courtId = court1,
            courtName = "1",
            statsMode = StatsMode.BASIC,
            matchConfig = MatchConfig(gamesPerSet = 4, setsToWin = 2, statsMode = StatsMode.BASIC),
        )
        store.save(initial)

        val reopenedBefore = MatchResume.withChosenConfig(
            store.getLast()!!,
            MatchConfig(gamesPerSet = 6, setsToWin = 2, statsMode = StatsMode.BASIC),
        )
        assertEquals(6, reopenedBefore.matchConfig.gamesPerSet)
        assertEquals(initial.clientMatchUuid, reopenedBefore.clientMatchUuid)
        store.save(reopenedBefore)

        ActivityScenario.launch<MatchActivity>(MatchActivity.createIntent(app, reopenedBefore.clientMatchUuid, false)).use {
            UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
            onView(withId(R.id.buttonPlayer1Serves)).perform(click())
            clickWinLeft()

            val live = backend.waitForMatch(fixture.marker, description = "live match for director score correction") { m ->
                m.optString("player1_name") == reopenedBefore.getTeam1FullName() && m.optString("status") == "in_progress"
            }

            // --- Scenario 8: Director corrects score to 2:0 games, 40:0 points -> next point wins game 3 (odd!) -> side change announcement! ---
            backend.directorControl(
                live.getInt("id"),
                JSONObject().put(
                    "score",
                    JSONObject()
                        .put("player1_sets", 0)
                        .put("player2_sets", 0)
                        .put("player1_games", 2)
                        .put("player2_games", 0)
                        .put("player1_points", 3)
                        .put("player2_points", 0)
                )
            )
            waitUntil("Director score 2 games and 40 points applied", 20_000) {
                onView(withId(R.id.textPlayer1Points)).check(matches(withText("40")))
                onView(withId(R.id.textPlayer1Set1)).check(matches(withText("2")))
                true
            }
            clickWinLeft()
            waitUntil("Side change announcement appears at 3:0 after director score correction", 10_000) {
                onView(withId(R.id.buttonAnnouncementContinue)).check(matches(isDisplayed()))
                onView(withId(R.id.buttonAnnouncementSkipSideChange)).check(matches(isDisplayed()))
                true
            }
        }

        val freshStore = ActiveMatchStore(app)
        waitUntil("SharedPreferences persisted played match state", 5_000) {
            val loaded = freshStore.get(reopenedBefore.clientMatchUuid)
            loaded != null && MatchResume.keepsPlayedFormat(
                loaded,
                MatchConfig(gamesPerSet = 4, setsToWin = 2, statsMode = StatsMode.BASIC)
            )
        }

        // --- Scenario 4: 11 wrong PIN attempts on Court 3 -> CourtPinFailure.Throttled ---
        val repo = app.container.repository()
        var eleventhFailure: Throwable? = null
        runBlocking {
            repeat(10) {
                val res = repo.verifyCourtPin(court3, "0001")
                assertTrue("Attempt ${it + 1} should fail with InvalidPin", res.exceptionOrNull() is CourtPinFailure.InvalidPin)
            }
            eleventhFailure = repo.verifyCourtPin(court3, "0001").exceptionOrNull()
        }
        assertTrue(
            "11th wrong PIN must return CourtPinFailure.Throttled, got $eleventhFailure",
            eleventhFailure is CourtPinFailure.Throttled
        )
        val throttled = eleventhFailure as CourtPinFailure.Throttled
        val formattedToast = app.getString(R.string.pin_throttled, throttled.retryAfterMinutes)
        assertTrue("Localized throttle message includes minutes: $formattedToast", formattedToast.contains("${throttled.retryAfterMinutes}"))
    }

    private fun clickWinLeft() {
        waitUntil("Basic left WIN button displayed", 10_000) {
            onView(
                allOf(
                    anyOf(withId(R.id.buttonWinServerLeft), withId(R.id.buttonWinReceiverLeft)),
                    isDisplayed(),
                )
            ).perform(click())
            true
        }
    }

    private suspend fun clearOutbox() {
        app.openHelperSafeClearOutbox()
    }

    private fun clearActiveMatchPrefs() {
        app.getSharedPreferences("active_match_store", android.content.Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun setAirplane(enabled: Boolean) {
        val command = if (enabled) "cmd connectivity airplane-mode enable" else "cmd connectivity airplane-mode disable"
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }

    companion object {
        @JvmStatic
        @AfterClass
        fun finishInstrumentation() {
            InstrumentationRegistry.getInstrumentation().finish(Activity.RESULT_OK, Bundle())
        }
    }
}

private fun TennisRefereeApp.openHelperSafeClearOutbox() {
    database.openHelper.writableDatabase.execSQL("DELETE FROM outbox_mutations")
}
