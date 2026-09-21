package pl.vestmedia.tennisreferee.utils

import android.content.Context
import android.provider.Settings
import pl.vestmedia.tennisreferee.data.model.TournamentOption

/**
 * Google Play's pre-launch report runs the app on Firebase Test Lab devices, where a robot
 * taps through every screen. Left to itself it opened a real tournament court and finished
 * a scheduled match. On those devices only the Play-review sandbox is offered.
 */
object TestLab {
    private const val SETTING = "firebase.test.lab"

    fun isRunning(context: Context): Boolean =
        runCatching { Settings.System.getString(context.contentResolver, SETTING) == "true" }
            .getOrDefault(false)

    fun visibleTournaments(all: List<TournamentOption>, inTestLab: Boolean): List<TournamentOption> =
        if (inTestLab) all.filter { it.isSimulation } else all
}
