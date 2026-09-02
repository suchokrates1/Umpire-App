package pl.vestmedia.tennisreferee.ui.tutorial

import android.content.Context

object TutorialPrefs {
    private const val PREFS = "TennisRefereeTutorial"
    private const val KEY_DONE = "tutorial_done"
    private const val KEY_PROMPTED = "tutorial_prompted"

    fun isDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DONE, false)

    fun markDone(context: Context) {
        prefs(context).edit().putBoolean(KEY_DONE, true).apply()
    }

    fun isPrompted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PROMPTED, false)

    fun markPrompted(context: Context) {
        prefs(context).edit().putBoolean(KEY_PROMPTED, true).apply()
    }

    fun shouldShowBanner(context: Context): Boolean =
        !isDone(context) && !isPrompted(context)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
