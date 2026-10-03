package pl.vestmedia.tennisreferee.ui.match

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import pl.vestmedia.tennisreferee.data.localJson
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

class ActiveMatchStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(state: MatchState) {
        prefs.edit()
            .putString(keyFor(state.clientMatchUuid), localJson.encodeToString(state))
            .putString(KEY_LAST_MATCH_UUID, state.clientMatchUuid)
            .apply()
    }

    fun getLast(): MatchState? {
        val matchUuid = prefs.getString(KEY_LAST_MATCH_UUID, null) ?: return null
        return get(matchUuid)
    }

    fun get(matchUuid: String): MatchState? {
        val payload = prefs.getString(keyFor(matchUuid), null) ?: return null
        return decode(payload)
    }

    fun clear(matchUuid: String) {
        prefs.edit()
            .remove(keyFor(matchUuid))
            .remove(KEY_LAST_MATCH_UUID)
            .apply()
    }

    private fun keyFor(matchUuid: String): String = "$KEY_MATCH_PREFIX$matchUuid"

    private companion object {
        private const val PREFS_NAME = "active_match_store"
        private const val KEY_MATCH_PREFIX = "match_"
        private const val KEY_LAST_MATCH_UUID = "last_match_uuid"

        /** The match the referee is in the middle of; losing it costs them the set. */
        fun decode(payload: String): MatchState? = try {
            localJson.decodeFromString<MatchState>(payload)
        } catch (_: Exception) {
            // Written by a build that used Gson. Read it once; the next save is kotlinx.
            try {
                Gson().fromJson(payload, MatchState::class.java)
            } catch (_: JsonSyntaxException) {
                null
            }
        }
    }
}
