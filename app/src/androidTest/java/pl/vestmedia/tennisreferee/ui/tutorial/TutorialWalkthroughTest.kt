package pl.vestmedia.tennisreferee.ui.tutorial

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.ui.settings.SettingsActivity

@RunWith(AndroidJUnit4::class)
@LargeTest
class TutorialWalkthroughTest {
    @Test
    fun settingsStartsTutorialCourtOverlay() {
        ActivityScenario.launch(SettingsActivity::class.java)
        onView(withId(R.id.cardTutorial)).perform(click())
        onView(withText(R.string.tutorial_court_title)).check(matches(isDisplayed()))
    }
}
