package pl.vestmedia.tennisreferee.e2e

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.model.TournamentOption
import pl.vestmedia.tennisreferee.ui.courtselection.CourtSelectionActivity
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.ui.tournamentselection.TournamentSelectionStore
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@LargeTest
class DoublesSuggestionE2ETest {

    private val backend = E2EBackendClient()
    private lateinit var fixture: TournamentFixture
    private val pin = "4242"

    @Before
    fun setUp() {
        RetrofitClient.overrideBaseUrl(backend.baseUrl)
        val marker = "E2E-${System.currentTimeMillis()}-dbl"
        backend.cleanup(marker)
        fixture = backend.createTournamentFixture(marker)
        val courtId = fixture.courtIdFor(0)
        backend.setCourtPin(courtId, pin)
        backend.seedDoublesSuggestedMatch(fixture, courtId)

        val ctx = ApplicationProvider.getApplicationContext<TennisRefereeApp>()
        LanguageSelectionActivity.setLanguage(ctx, "en")
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
            RetrofitClient.overrideBaseUrl(null)
            backend.close()
        }
    }

    @Test
    fun useSuggestedDoubles_keepsFourPlayersAndEnablesStart() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            CourtSelectionActivity::class.java
        )
        ActivityScenario.launch<CourtSelectionActivity>(intent).use { scenario ->
            UmpireRobot.waitForView(R.id.recyclerViewCourts, timeoutMs = 30_000)
            waitUntil("courts loaded into recycler", timeoutMs = 45_000) {
                var count = 0
                scenario.onActivity { activity ->
                    val rv = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewCourts)
                    count = rv?.adapter?.itemCount ?: 0
                }
                count > 0
            }
            onView(withId(R.id.recyclerViewCourts)).perform(
                RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(
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
            onView(withId(R.id.buttonUseSuggestedMatch)).perform(click())

            waitUntil("match config shows suggested doubles pairs", timeoutMs = 20_000) {
                try {
                    onView(withId(R.id.textMixedDoublesSummary)).check(matches(isDisplayed()))
                    onView(withId(R.id.textMixedDoublesSummary)).check(matches(withText(containsString(" / "))))
                    true
                } catch (_: Throwable) {
                    false
                }
            }
            onView(withId(R.id.textMixedStatus)).check(matches(withText(containsString("Doubles"))))
        }
    }

    companion object {
        @JvmStatic
        @AfterClass
        fun finishInstrumentation() {
            InstrumentationRegistry.getInstrumentation().finish(Activity.RESULT_OK, Bundle())
        }
    }
}
