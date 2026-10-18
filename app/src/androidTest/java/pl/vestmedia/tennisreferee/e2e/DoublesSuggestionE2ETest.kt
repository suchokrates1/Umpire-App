package pl.vestmedia.tennisreferee.e2e

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.model.TournamentOption
import pl.vestmedia.tennisreferee.ui.courtselection.CourtSelectionActivity
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.ui.tutorial.TutorialPrefs
import pl.vestmedia.tennisreferee.ui.tournamentselection.TournamentSelectionStore

@RunWith(AndroidJUnit4::class)
@LargeTest
class DoublesSuggestionE2ETest {

    private val backend = E2EBackendClient()
    private lateinit var fixture: TournamentFixture
    private val pin = "4242"
    private var scheduleId = 0

    @Before
    fun setUp() {
        overrideUmpireBackend(backend.baseUrl)
        val marker = "E2E-${System.currentTimeMillis()}-dbl"
        backend.cleanup(marker)
        fixture = backend.createTournamentFixture(marker)
        val courtId = fixture.courtIdFor(0)
        backend.setCourtPin(courtId, pin)
        scheduleId = backend.seedDoublesSuggestedMatch(fixture, courtId)

        val ctx = ApplicationProvider.getApplicationContext<TennisRefereeApp>()
        LanguageSelectionActivity.setLanguage(ctx, "en")
        TutorialPrefs.markPrompted(ctx)
        TournamentSelectionStore.saveSelection(
            ctx,
            TournamentOption(
                id = fixture.tournamentId,
                name = "${fixture.marker} Android Emulator Open",
                startDate = LocalDate.now().toString(),
                endDate = LocalDate.now().plusDays(1).toString(),
                city = "E2E",
                country = "PL",
            )
        )
        runBlocking { ctx.matchHistoryRepository.deleteAllMatches() }
    }

    @After
    fun tearDown() {
        try {
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
    fun useSuggestedDoubles_keepsFourPlayersAndEnablesStart() {
        openPlayerSelection {
            onView(withId(R.id.buttonUseSuggestedMatch)).perform(click())
            waitForDoublesConfig()
            onView(withId(R.id.textMixedStatus)).inRoot(isDialog()).check(matches(withText(containsString("Doubles"))))
        }
    }

    /** The suggested doubles is umpired on its slot: the slot goes in progress, then completed with the match. */
    @Test
    fun suggestedDoubles_runsOnItsScheduleSlotToTheEnd() {
        openPlayerSelection {
            onView(withId(R.id.buttonUseSuggestedMatch)).perform(click())
            waitForDoublesConfig()
            startTiebreakOnlyMatch()
            backend.waitForScheduleEntry(fixture.tournamentId, scheduleId, "slot in progress") {
                it.optString("status") == "in_progress"
            }
            playTiebreakToSeven()
        }
        val slot = backend.waitForScheduleEntry(fixture.tournamentId, scheduleId, "slot completed with its match", 60_000) {
            it.optString("status") == "completed" && it.optInt("match_id") > 0
        }
        val match = backend.waitForMatch(fixture.marker, 60_000, "finished match of the slot") {
            it.optInt("id") == slot.getInt("match_id") && it.optString("status") == "finished"
        }
        assertEquals(slot.getString("player1_name"), match.getString("player1_name"))
        assertEquals(slot.getString("player2_name"), match.getString("player2_name"))
    }

    /**
     * Doubles chosen by hand with other pairs than the slot's: umpired to the end, the slot stays
     * planned. (The slot's own pairs picked by hand are linked to it by name, on purpose.)
     */
    @Test
    fun manualDoubles_isUmpiredWithoutTouchingTheSlot() {
        openPlayerSelection {
            onView(withId(R.id.buttonManualPlayers)).perform(click())
            onView(withId(R.id.checkboxDoubles)).perform(click())
            (5..8).forEach { ordinal ->
                onView(withId(R.id.recyclerViewPlayers)).perform(
                    RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                        hasDescendant(withText(containsString("${fixture.marker}-P$ordinal"))),
                        click()
                    )
                )
            }
            onView(withId(R.id.buttonNext)).perform(click())
            waitForDoublesConfig()
            startTiebreakOnlyMatch()
            // pairs from no group: the app says the match is recorded as a friendly
            waitUntil("friendly match notice", timeoutMs = 15_000) {
                try {
                    onView(withText(R.string.bracket_warning_title)).inRoot(isDialog()).check(matches(isDisplayed()))
                    true
                } catch (_: Throwable) {
                    false
                }
            }
            onView(withText("OK")).inRoot(isDialog()).perform(click())
            UmpireRobot.clickIfDisplayed(R.id.buttonPlayer1Serves, timeoutMs = 2_000)
            playTiebreakToSeven()
        }
        val match = backend.waitForMatch(fixture.marker, 60_000, "finished manual doubles") {
            it.optString("status") == "finished" && it.optString("player1_name").contains(" / ")
        }
        assertTrue(match.optString("player2_name").contains(" / "))
        val slot = requireNotNull(backend.scheduleEntry(fixture.tournamentId, scheduleId))
        assertEquals("planned", slot.optString("status"))
        assertTrue("slot keeps no match: $slot", slot.isNull("match_id") || slot.optInt("match_id") == 0)
    }

    /** Court list -> court 1 -> PIN -> the player screen with the suggestion card, then [onPlayers]. */
    private fun openPlayerSelection(onPlayers: () -> Unit) {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            CourtSelectionActivity::class.java
        )
        ActivityScenario.launch<CourtSelectionActivity>(intent).use { scenario ->
            UmpireRobot.waitForView(R.id.recyclerViewCourts, timeoutMs = 30_000)
            waitUntil("courts loaded into recycler", timeoutMs = 45_000) {
                var count = 0
                scenario.onActivity { activity ->
                    val rv = activity.findViewById<RecyclerView>(R.id.recyclerViewCourts)
                    count = rv?.adapter?.itemCount ?: 0
                }
                count > 0
            }
            onView(withId(R.id.recyclerViewCourts)).perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0,
                    click()
                )
            )

            waitUntil("PIN dialog digits", timeoutMs = 15_000) {
                try {
                    onView(withId(R.id.pinDigit1)).check(matches(isDisplayed()))
                    true
                } catch (_: Throwable) {
                    false
                }
            }
            onView(withId(R.id.pinDigit1)).perform(replaceText(pin[0].toString()))
            onView(withId(R.id.pinDigit2)).perform(replaceText(pin[1].toString()))
            onView(withId(R.id.pinDigit3)).perform(replaceText(pin[2].toString()))
            onView(withId(R.id.pinDigit4)).perform(replaceText(pin[3].toString()))

            UmpireRobot.waitForView(R.id.recyclerViewPlayers, timeoutMs = 30_000)
            UmpireRobot.waitForView(R.id.cardSuggestedMatch, timeoutMs = 30_000)
            onPlayers()
        }
    }

    private fun waitForDoublesConfig() {
        waitUntil("match config shows the doubles pairs", timeoutMs = 20_000) {
            try {
                onView(withId(R.id.textMixedDoublesSummary)).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withId(R.id.textMixedDoublesSummary)).inRoot(isDialog()).check(matches(withText(containsString(" / "))))
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    /** The shortest match the app has: one tie-break to 7 (Advanced: the robot scores with ace and fault). */
    private fun startTiebreakOnlyMatch() {
        onView(withId(R.id.switchTiebreakOnly)).inRoot(isDialog()).perform(scrollTo(), click())
        onView(withId(R.id.btnTbOnly7)).inRoot(isDialog()).perform(scrollTo(), click())
        onView(withId(R.id.cardAdvancedMode)).inRoot(isDialog()).perform(scrollTo(), click())
        UmpireRobot.waitForView(R.id.buttonPlayer1Serves, timeoutMs = 30_000)
        UmpireRobot.clickServerButton(1)
    }

    private fun playTiebreakToSeven() {
        UmpireRobot(doubles = true, firstServer = 1).playTiebreak(List(7) { true })
        UmpireRobot.waitForView(R.id.textWinner, timeoutMs = 30_000)
    }

    companion object {
        @JvmStatic
        @AfterClass
        fun finishInstrumentation() {
            InstrumentationRegistry.getInstrumentation().finish(Activity.RESULT_OK, Bundle())
        }
    }
}
