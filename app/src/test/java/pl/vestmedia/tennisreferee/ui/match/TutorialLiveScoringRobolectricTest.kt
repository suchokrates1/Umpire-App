package pl.vestmedia.tennisreferee.ui.match

import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.startup.StartupTestApp
import pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession
import pl.vestmedia.tennisreferee.ui.tutorial.TutorialSnapshots

@RunWith(RobolectricTestRunner::class)
@Config(application = StartupTestApp::class, sdk = [34])
class TutorialLiveScoringRobolectricTest {
    @After
    fun tearDown() {
        TutorialSession.stop(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun tutorialWinFaultAndUndoChangeTheLiveScore() {
        val app = ApplicationProvider.getApplicationContext<StartupTestApp>()
        TutorialSession.start(fromSettings = true)
        val snapshot = TutorialSnapshots.load(app, "basic")!!
        val intent = MatchActivity.createTutorialIntent(app, snapshot)

        ActivityScenario.launch<MatchActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[MatchViewModel::class.java]
                val before = viewModel.matchState.value!!
                assertEquals(2, before.player1Points)
                assertEquals(1, before.player2Points)
                assertTrue(before.isFirstServe)

                activity.findViewById<View>(R.id.buttonWinServerLeft).performClick()
                val afterWin = viewModel.matchState.value!!
                assertEquals(3, afterWin.player1Points)
                assertEquals(1, afterWin.player2Points)
                assertTrue(afterWin.isFirstServe)

                activity.findViewById<View>(R.id.buttonFaultServerLeft).performClick()
                val afterSecondServe = viewModel.matchState.value!!
                assertEquals(3, afterSecondServe.player1Points)
                assertFalse(afterSecondServe.isFirstServe)

                activity.findViewById<View>(R.id.buttonFaultServerLeft).performClick()
                val afterDoubleFault = viewModel.matchState.value!!
                assertEquals(3, afterDoubleFault.player1Points)
                assertEquals(2, afterDoubleFault.player2Points)
                assertEquals(1, afterDoubleFault.player1Stats.doubleFaults)
                assertTrue(afterDoubleFault.isFirstServe)

                activity.findViewById<View>(R.id.buttonUndo).performClick()
                val afterUndo = viewModel.matchState.value!!
                assertEquals(3, afterUndo.player1Points)
                assertEquals(1, afterUndo.player2Points)
                assertEquals(0, afterUndo.player1Stats.doubleFaults)
                assertFalse(afterUndo.isFirstServe)
            }
        }
    }
}
