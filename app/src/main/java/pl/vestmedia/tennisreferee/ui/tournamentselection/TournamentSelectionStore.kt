package pl.vestmedia.tennisreferee.ui.tournamentselection

import android.content.Context
import pl.vestmedia.tennisreferee.data.model.TournamentOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TournamentSelectionStore {
    private const val PREFS_NAME = "TennisRefereePrefs"
    private const val KEY_TOURNAMENT_ID = "selected_tournament_id"
    private const val KEY_TOURNAMENT_NAME = "selected_tournament_name"
    private const val KEY_TOURNAMENT_DAY = "selected_tournament_day"

    fun saveSelection(context: Context, tournament: TournamentOption) {
        prefs(context).edit()
            .putInt(KEY_TOURNAMENT_ID, tournament.id)
            .putString(KEY_TOURNAMENT_NAME, tournament.name)
            .putString(KEY_TOURNAMENT_DAY, todayKey())
            .apply()
    }

    fun getSelectedTournamentIdForToday(context: Context): Int? {
        if (!hasSelectionForToday(context)) {
            clearSelection(context)
            return null
        }
        return prefs(context).getInt(KEY_TOURNAMENT_ID, -1).takeIf { it > 0 }
    }

    fun getSelectedTournamentNameForToday(context: Context): String? {
        if (!hasSelectionForToday(context)) {
            clearSelection(context)
            return null
        }
        return prefs(context).getString(KEY_TOURNAMENT_NAME, null)
    }

    fun hasSelectionForToday(context: Context): Boolean {
        val prefs = prefs(context)
        return prefs.contains(KEY_TOURNAMENT_ID) && prefs.getString(KEY_TOURNAMENT_DAY, null) == todayKey()
    }

    fun clearSelection(context: Context) {
        prefs(context).edit()
            .remove(KEY_TOURNAMENT_ID)
            .remove(KEY_TOURNAMENT_NAME)
            .remove(KEY_TOURNAMENT_DAY)
            .apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}