package pl.vestmedia.tennisreferee.ui.tutorial

import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
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
        for (id in listOf(
            "serve",
            "basic",
            "double-fault",
            "server-change",
            "side-change",
            "set-break",
            "tiebreak",
            "undo",
            "finished",
        )) {
            val loaded = TutorialSnapshots.load(app, id)
            assertNotNull(id, loaded)
            assertTrue(id, loaded!!.state.clientMatchUuid.startsWith("tutorial-"))
        }
        val byId = script.steps.associateBy { it.id }
        assertEquals("awardPoint", byId.getValue("basic-scoring").requireAction)
        assertEquals("basic", byId.getValue("basic-scoring").snapshot)
        assertEquals("secondServe", byId.getValue("second-serve").requireAction)
        assertEquals(null, byId.getValue("second-serve").snapshot)
        assertEquals("doubleFault", byId.getValue("double-fault").requireAction)
        assertEquals(null, byId.getValue("double-fault").snapshot)
        assertEquals("undo", byId.getValue("undo").requireAction)
        assertEquals(null, byId.getValue("undo").snapshot)
        assertEquals("server-change", byId.getValue("server-change").snapshot)
        assertEquals(R.id.buttonFaultServerLeft, TutorialTargets.viewId("secondServe"))
        assertEquals(R.id.buttonFaultServerLeft, TutorialTargets.viewId("doubleFault"))
        assertEquals(R.id.buttonUndo, TutorialTargets.viewId("undo"))
        for (step in script.steps) {
            val resName = TutorialScript.titleRes(step.titleKey)
            val resId = app.resources.getIdentifier(resName, "string", app.packageName)
            assertTrue("${step.id} missing string $resName", resId != 0)
            assertTrue(step.id, app.getString(resId).isNotBlank())
        }
    }

    @Test
    fun tutorialSnapshotsSurviveIntentParcel() {
        val app = ApplicationProvider.getApplicationContext<StartupTestApp>()
        for (id in listOf("serve", "basic", "server-change", "side-change", "set-break", "tiebreak", "finished")) {
            val snapshot = TutorialSnapshots.load(app, id)!!
            val parcel = Parcel.obtain()
            parcel.writeParcelable(snapshot.state, 0)
            parcel.setDataPosition(0)
            @Suppress("DEPRECATION")
            val restored = parcel.readParcelable<MatchState>(MatchState::class.java.classLoader)!!
            parcel.recycle()
            assertEquals(id, snapshot.state.setsHistory.size, restored.setsHistory.size)
            if (snapshot.state.setsHistory.isNotEmpty()) {
                assertEquals(id, snapshot.state.setsHistory[0].player1Games, restored.setsHistory[0].player1Games)
            }
        }
    }
}
