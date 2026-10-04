package pl.vestmedia.tennisreferee.ui.panic

import android.os.Looper
import android.view.Menu
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.startup.StartupTestApp
import pl.vestmedia.tennisreferee.ui.history.MatchHistoryActivity
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.ui.settings.SettingsActivity

/**
 * The SOS button is how an umpire calls for help from any screen. Each screen adds it
 * with one PanicPrompt.install call; this checks it is there next to the screen's own
 * menu, and that tapping it opens the help dialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = StartupTestApp::class, sdk = [34])
class PanicMenuRobolectricTest {

    @Test
    fun aScreenWithoutAMenuOfItsOwnStillHasSos() {
        assertSosOpensHelp(Robolectric.buildActivity(LanguageSelectionActivity::class.java).setup().get())
    }

    @Test
    fun aScreenWithItsOwnMenuKeepsItAndGainsSos() {
        val history = Robolectric.buildActivity(MatchHistoryActivity::class.java).setup().get()
        assertNotNull("the screen's own item stays", menuOf(history).findItem(R.id.action_delete_all))
        assertSosOpensHelp(history)
    }

    @Test
    fun settingsHasSos() {
        assertSosOpensHelp(Robolectric.buildActivity(SettingsActivity::class.java).setup().get())
    }

    private fun assertSosOpensHelp(activity: AppCompatActivity) {
        val sos = menuOf(activity).findItem(R.id.action_panic)
        assertNotNull("${activity.javaClass.simpleName} shows SOS", sos)
        assertTrue(activity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, sos))
        assertTrue("help dialog is open", ShadowDialog.getLatestDialog()?.isShowing == true)
    }

    /** A screen that sets its own Toolbar as the action bar keeps the menu on that Toolbar. */
    private fun menuOf(activity: AppCompatActivity): Menu {
        shadowOf(Looper.getMainLooper()).idle()
        return activity.findViewById<Toolbar>(R.id.toolbar)?.menu ?: shadowOf(activity).optionsMenu
    }
}
