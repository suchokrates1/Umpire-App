package pl.vestmedia.tennisreferee.ui.tutorial

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.vestmedia.tennisreferee.startup.StartupTestApp

@RunWith(RobolectricTestRunner::class)
@Config(application = StartupTestApp::class, sdk = [34])
class TutorialScriptRobolectricTest {
    @Test
    fun scriptLoadsUniqueStepsAndServeSnapshot() {
        val app = ApplicationProvider.getApplicationContext<StartupTestApp>()
        val script = TutorialScript.load(app)
        assertTrue(script.steps.size >= 14)
        assertEquals(script.steps.size, script.steps.map { it.id }.toSet().size)
        val snapshot = TutorialSnapshots.load(app, "serve")
        assertNotNull(snapshot)
        assertTrue(snapshot!!.state.clientMatchUuid.startsWith("tutorial-"))
        for (id in listOf("serve", "basic", "double-fault", "side-change", "set-break", "tiebreak", "undo", "finished")) {
            val loaded = TutorialSnapshots.load(app, id)
            assertNotNull(id, loaded)
            assertTrue(id, loaded!!.state.clientMatchUuid.startsWith("tutorial-"))
        }
    }
}
