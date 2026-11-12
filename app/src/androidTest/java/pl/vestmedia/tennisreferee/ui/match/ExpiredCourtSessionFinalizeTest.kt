package pl.vestmedia.tennisreferee.ui.match

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.time.Instant
import java.util.Collections
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.auth.CourtSession
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.TournamentOption
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode
import pl.vestmedia.tennisreferee.e2e.UmpireRobot
import pl.vestmedia.tennisreferee.e2e.overrideUmpireBackend
import pl.vestmedia.tennisreferee.e2e.waitUntil
import pl.vestmedia.tennisreferee.ui.courtselection.CourtSelectionActivity
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.ui.tournamentselection.TournamentSelectionStore
import pl.vestmedia.tennisreferee.ui.tutorial.TutorialPrefs

/**
 * End-to-end check on an Android device/emulator:
 * court session expires mid-match -> umpire finishes the match (401 queues UPDATE + FINISH + STATS)
 * -> umpire re-enters the court PIN -> result arrives on the server immediately without starting
 * another match or toggling Wi-Fi.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ExpiredCourtSessionFinalizeTest {

    private lateinit var server: MockWebServer
    private val authorizedCalls = Collections.synchronizedList(mutableListOf<String>())

    @Volatile
    private var sessionExpired = false

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<TennisRefereeApp>()
        LanguageSelectionActivity.setLanguage(app, "en")
        TutorialPrefs.markPrompted(app)
        TournamentSelectionStore.saveSelection(
            app,
            TournamentOption(id = 1, name = "Session Expiry Open", city = "Warsaw", country = "PL")
        )
        runBlocking {
            app.database.clearAllTables()
        }

        val futureIso = Instant.now().plusSeconds(3600).toString()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty().substringBefore('?')
                val method = request.method.orEmpty()
                val auth = request.getHeader("Authorization").orEmpty()

                if (path == "/api/courts" && method == "GET") {
                    return jsonResponse(
                        200,
                        """{"courts":[{"kort_id":"t1-1","name":"Court 1","is_available":true}],"total_count":1}"""
                    )
                }
                if (path == "/api/courts/t1-1/authorize" && method == "POST") {
                    return jsonResponse(
                        200,
                        """{"ok":true,"authorized":true,"court_id":"t1-1","token":"tok-fresh","expires_at":"$futureIso"}"""
                    )
                }
                if (path == "/api/players" && method == "GET") {
                    return jsonResponse(200, """{"players":[],"count":0,"ok":true}""")
                }
                if (path.endsWith("/suggested-match")) {
                    return jsonResponse(200, """{"suggestion":null}""")
                }
                if (path == "/api/umpire-heartbeat" || path == "/api/umpire/commands") {
                    return jsonResponse(200, """{"status":"ok","commands":[]}""")
                }

                val hasFreshToken = auth == "Bearer tok-fresh"
                val hasInitialToken = auth == "Bearer tok-initial" && !sessionExpired
                if (!hasFreshToken && !hasInitialToken) {
                    return jsonResponse(401, """{"error":"Court session expired"}""")
                }

                authorizedCalls += "$method $path ($auth)"
                return when {
                    path == "/api/matches" && method == "POST" ->
                        jsonResponse(201, matchJson(901, "in_progress"))
                    path == "/api/matches/901" && method == "PUT" ->
                        jsonResponse(200, matchJson(901, "in_progress"))
                    path == "/api/matches/901/finish" && method == "POST" ->
                        jsonResponse(200, matchJson(901, "finished"))
                    path == "/api/match-statistics" && method == "POST" ->
                        jsonResponse(200, """{"message":"ok"}""")
                    path == "/api/match-events" && method == "POST" ->
                        jsonResponse(200, """{"success":true}""")
                    else ->
                        jsonResponse(200, "{}")
                }
            }
        }
        server.start()
        overrideUmpireBackend(server.url("/").toString())
        app.container.sessionStore.save(
            CourtSession(
                courtId = "t1-1",
                token = "tok-initial",
                expiresAtMillis = System.currentTimeMillis() + 3_600_000L
            )
        )
    }

    @After
    fun tearDown() {
        val app = ApplicationProvider.getApplicationContext<TennisRefereeApp>()
        overrideUmpireBackend(null)
        app.container.sessionStore.clear()
        runBlocking { app.database.clearAllTables() }
        if (::server.isInitialized) server.shutdown()
    }

    @Test
    fun finishAfterSessionExpiresQueuesResultAndFlushesImmediatelyAfterReenteringPin() {
        val state = MatchState(
            clientMatchUuid = "exp-session-uuid-1",
            player1 = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL"),
            player2 = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "PL"),
            courtId = "t1-1",
            courtName = "Court 1",
            statsMode = StatsMode.BASIC,
            matchConfig = MatchConfig(gamesPerSet = 4, setsToWin = 2, statsMode = StatsMode.BASIC),
        )
        val matchIntent = MatchActivity.createStateIntent(ApplicationProvider.getApplicationContext(), state)

        ActivityScenario.launch<MatchActivity>(matchIntent).use {
            UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
            onView(withId(R.id.buttonPlayer1Serves)).perform(click())

            waitUntil("initial POST /api/matches succeeded", 15_000) {
                authorizedCalls.any { it.startsWith("POST /api/matches ") }
            }

            // Expire the court session mid-match while Wi-Fi remains connected.
            sessionExpired = true

            UmpireRobot.waitForView(R.id.buttonWinServerLeft)
            onView(withId(R.id.buttonWinServerLeft)).perform(click())
            onView(withId(R.id.buttonBack)).perform(click())

            waitUntil("finish reason dialog", 10_000) {
                onView(withText(R.string.finish_reason_retirement)).inRoot(isDialog()).check(matches(isDisplayed()))
                true
            }
            onView(withText(R.string.finish_reason_retirement)).inRoot(isDialog()).perform(click())

            waitUntil("injured player dialog", 10_000) {
                onView(withText(containsString("Kowalski"))).inRoot(isDialog()).check(matches(isDisplayed()))
                true
            }
            onView(withText(containsString("Kowalski"))).inRoot(isDialog()).perform(click())

            val app = ApplicationProvider.getApplicationContext<TennisRefereeApp>()
            waitUntil("UPDATE, FINISH and STATS queued in Room outbox after 401", 15_000) {
                val types = runBlocking { app.database.outboxMutationDao().getPending().map { it.type } }
                types.containsAll(listOf("UPDATE", "FINISH", "STATS"))
            }
        }

        // Umpire goes to CourtSelection and re-enters the PIN — without starting a new match.
        ActivityScenario.launch(CourtSelectionActivity::class.java).use { scenario ->
            UmpireRobot.waitForView(R.id.recyclerViewCourts, timeoutMs = 15_000)
            waitUntil("court item loaded", 15_000) {
                var count = 0
                scenario.onActivity { activity ->
                    count = activity.findViewById<RecyclerView>(R.id.recyclerViewCourts)?.adapter?.itemCount ?: 0
                }
                count > 0
            }
            onView(withId(R.id.recyclerViewCourts)).perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
            )
            waitUntil("PIN dialog shown", 10_000) {
                onView(withId(R.id.pinDigit1)).check(matches(isDisplayed()))
                true
            }
            onView(withId(R.id.pinDigit1)).perform(replaceText("2"))
            onView(withId(R.id.pinDigit2)).perform(replaceText("4"))
            onView(withId(R.id.pinDigit3)).perform(replaceText("6"))
            onView(withId(R.id.pinDigit4)).perform(replaceText("8"))

            waitUntil("queued UPDATE, FINISH and STATS delivered with fresh token", 15_000) {
                val snapshot = synchronized(authorizedCalls) { authorizedCalls.toList() }
                snapshot.any { it == "PUT /api/matches/901 (Bearer tok-fresh)" } &&
                    snapshot.any { it == "POST /api/matches/901/finish (Bearer tok-fresh)" } &&
                    snapshot.any { it == "POST /api/match-statistics (Bearer tok-fresh)" }
            }

            val app = ApplicationProvider.getApplicationContext<TennisRefereeApp>()
            waitUntil("outbox rows deleted after HTTP response completes", 5_000) {
                runBlocking { app.database.outboxMutationDao().getPending().isEmpty() }
            }
            val remaining = runBlocking { app.database.outboxMutationDao().getPending() }
            assertTrue("outbox should be empty after PIN re-auth, got $remaining", remaining.isEmpty())
        }
    }

    private fun jsonResponse(code: Int, body: String): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .addHeader("Content-Type", "application/json")
            .setBody(body)

    private fun matchJson(id: Int, status: String): String =
        """{"id":$id,"court_id":"t1-1","player1_name":"Jan Kowalski","player2_name":"Adam Nowak","status":"$status","score":{"player1_sets":0,"player2_sets":0,"player1_games":0,"player2_games":0,"player1_points":0,"player2_points":0,"sets_history":[]}}"""
}
