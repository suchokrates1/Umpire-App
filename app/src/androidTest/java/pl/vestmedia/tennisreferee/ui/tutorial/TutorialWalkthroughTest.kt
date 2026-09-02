package pl.vestmedia.tennisreferee.ui.tutorial

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.e2e.UmpireRobot
import pl.vestmedia.tennisreferee.e2e.waitUntil
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.ui.settings.SettingsActivity

@RunWith(AndroidJUnit4::class)
@LargeTest
class TutorialWalkthroughTest {
    private val matchWrites = mutableListOf<String>()
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        LanguageSelectionActivity.setLanguage(ctx, "en")
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                if (path.contains("/matches") && request.method in listOf("POST", "PUT")) {
                    matchWrites += "${request.method} $path"
                }
                return MockResponse().setBody("{}")
            }
        }
        server.start()
        RetrofitClient.overrideBaseUrl(server.url("/").toString())
    }

    @After
    fun tearDown() {
        TutorialSession.stop(InstrumentationRegistry.getInstrumentation().targetContext)
        RetrofitClient.overrideBaseUrl(null)
        if (::server.isInitialized) server.shutdown()
    }

    @Test
    fun settingsWalksEveryTutorialStepWithoutPostingMatches() {
        ActivityScenario.launch(SettingsActivity::class.java)
        onView(withId(R.id.cardTutorial)).perform(click())
        see(R.string.tutorial_court_title)
        onView(withId(R.id.recyclerViewCourts)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()),
        )
        see(R.string.tutorial_pin_title)
        onView(withId(R.id.pinDigit1)).perform(replaceText("1"))
        onView(withId(R.id.pinDigit2)).perform(replaceText("2"))
        onView(withId(R.id.pinDigit3)).perform(replaceText("3"))
        onView(withId(R.id.pinDigit4)).perform(replaceText("4"))
        see(R.string.tutorial_players_title)
        tapPlayer("Costa")
        tapPlayer("Nowak")
        waitUntil("match setup dialog", 15_000) {
            onView(withText(R.string.match_config_title)).check(matches(isDisplayed()))
            true
        }
        onView(withId(R.id.cardBasicMode)).perform(scrollTo(), click())
        see(R.string.tutorial_serve_swap_title)
        tapId(R.id.buttonSwapSides)
        see(R.string.tutorial_serve_pick_title)
        tapId(R.id.buttonPlayer1Serves)
        see(R.string.tutorial_basic_title)
        tapId(R.id.buttonWinServerLeft)
        see(R.string.tutorial_second_serve_title)
        tapId(R.id.buttonFaultServerLeft)
        see(R.string.tutorial_double_fault_title)
        tapId(R.id.buttonFaultServerLeft)
        see(R.string.tutorial_undo_title)
        tapId(R.id.buttonUndo)
        see(R.string.tutorial_server_change_title)
        tapGuideNext()
        see(R.string.tutorial_side_change_title)
        tapGuideNext()
        see(R.string.tutorial_set_break_title)
        tapGuideNext()
        see(R.string.tutorial_tiebreak_title)
        tapGuideNext()
        see(R.string.tutorial_finish_title)
        tapId(R.id.buttonBack)
        waitUntil("finish reason dialog", 15_000) {
            onView(withText(R.string.finish_reason_retirement)).inRoot(isDialog()).check(matches(isDisplayed()))
            true
        }
        onView(withText(R.string.finish_reason_retirement)).inRoot(isDialog()).perform(click())
        waitUntil("retirement player dialog", 15_000) {
            onView(withText(containsString("Costa"))).inRoot(isDialog()).check(matches(isDisplayed()))
            true
        }
        onView(withText(containsString("Costa"))).inRoot(isDialog()).perform(click())
        see(R.string.tutorial_complete_title)
        onView(allOf(withText(R.string.tutorial_exit), isDisplayed())).perform(click())
        UmpireRobot.waitForView(R.id.cardTutorial)
        assertTrue("tutorial must not write matches: $matchWrites", matchWrites.isEmpty())
    }

    private fun see(title: Int) {
        waitUntil("tutorial title ${InstrumentationRegistry.getInstrumentation().targetContext.getString(title)}", 15_000) {
            onView(allOf(withText(title), not(withId(R.id.textTitle)))).check(matches(isDisplayed()))
            true
        }
    }

    private fun tapId(id: Int) {
        UmpireRobot.waitForView(id)
        onView(allOf(withId(id), isDisplayed())).perform(object : ViewAction {
            override fun getConstraints() = isDisplayed()
            override fun getDescription() = "performClick on $id"
            override fun perform(uiController: UiController, view: View) {
                view.performClick()
            }
        })
    }

    private fun tapGuideNext() {
        waitUntil("tutorial Next visible", 15_000) {
            onView(allOf(withText(R.string.tutorial_next), isDisplayed())).check(matches(isDisplayed()))
            true
        }
        onView(allOf(withText(R.string.tutorial_next), isDisplayed())).perform(click())
    }

    private fun tapPlayer(name: String) {
        waitUntil("player $name visible", 15_000) {
            onView(allOf(withText(containsString(name)), isDisplayed())).check(matches(isDisplayed()))
            true
        }
        onView(allOf(withText(containsString(name)), isDisplayed())).perform(click())
    }
}
