package pl.vestmedia.tennisreferee.ui.match

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

class ActiveMatchStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(state: MatchState) {
        prefs.edit()
            .putString(keyFor(state.clientMatchUuid), gson.toJson(state))
            .putString(KEY_LAST_MATCH_UUID, state.clientMatchUuid)
            .apply()
    }

    fun get(matchUuid: String): MatchState? {
        val payload = prefs.getString(keyFor(matchUuid), null) ?: return null
        return try {
            gson.fromJson(payload, MatchState::class.java)
        } catch (_: JsonSyntaxException) {
            null
        }
    }

    fun clear(matchUuid: String) {
        prefs.edit()
            .remove(keyFor(matchUuid))
            .remove(KEY_LAST_MATCH_UUID)
            .apply()
    }

    private fun keyFor(matchUuid: String): String = "$KEY_MATCH_PREFIX$matchUuid"

    companion object {
        private const val PREFS_NAME = "active_match_store"
        private const val KEY_MATCH_PREFIX = "match_"
        private const val KEY_LAST_MATCH_UUID = "last_match_uuid"
    }
}