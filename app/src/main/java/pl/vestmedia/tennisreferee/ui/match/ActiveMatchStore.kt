package pl.vestmedia.tennisreferee.ui.match

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.data.localJson
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import java.util.concurrent.ConcurrentHashMap

class ActiveMatchStore(
    context: Context,
    dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val writeScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val inMemory = ConcurrentHashMap<String, MatchState>()

    @Volatile
    private var lastMatchUuid: String? = prefs.getString(KEY_LAST_MATCH_UUID, null)

    internal fun resetInMemoryForTests() {
        inMemory.clear()
        lastMatchUuid = prefs.getString(KEY_LAST_MATCH_UUID, null)
    }

    /**
     * Captures a detached snapshot on the caller's thread and encodes + writes JSON on IO.
     * Any older match entries left behind by abandoned matches are pruned in the same edit,
     * so SharedPreferences only ever holds the current active match.
     */
    fun save(state: MatchState) {
        val snapshot = state.detachedCopy(includeActionsHistory = true)
        val uuid = snapshot.clientMatchUuid
        inMemory.keys.retainAll(setOf(uuid))
        inMemory[uuid] = snapshot
        lastMatchUuid = uuid
        writeScope.launch {
            persistSnapshot(snapshot)
        }
    }

    fun getLast(): MatchState? {
        val matchUuid = lastMatchUuid ?: prefs.getString(KEY_LAST_MATCH_UUID, null) ?: return null
        return get(matchUuid)
    }

    fun get(matchUuid: String): MatchState? {
        inMemory[matchUuid]?.let { return it.detachedCopy(includeActionsHistory = true) }
        val payload = prefs.getString(keyFor(matchUuid), null) ?: return null
        return decode(payload)?.also { inMemory[matchUuid] = it }?.detachedCopy(includeActionsHistory = true)
    }

    fun clear(matchUuid: String) {
        inMemory.remove(matchUuid)
        if (lastMatchUuid == matchUuid) {
            lastMatchUuid = null
        }
        writeScope.launch {
            val editor = prefs.edit().remove(keyFor(matchUuid))
            if (prefs.getString(KEY_LAST_MATCH_UUID, null) == matchUuid) {
                editor.remove(KEY_LAST_MATCH_UUID)
            }
            // Also sweep any orphaned match_* keys left behind by earlier sessions.
            val keepKey = lastMatchUuid?.let { keyFor(it) }
            for (existingKey in prefs.all.keys) {
                if (existingKey.startsWith(KEY_MATCH_PREFIX) && existingKey != keepKey) {
                    editor.remove(existingKey)
                }
            }
            editor.apply()
        }
    }

    internal fun persistSnapshot(snapshot: MatchState) {
        val keepKey = keyFor(snapshot.clientMatchUuid)
        val encoded = localJson.encodeToString(snapshot)
        val editor = prefs.edit()
        for (existingKey in prefs.all.keys) {
            if (existingKey.startsWith(KEY_MATCH_PREFIX) && existingKey != keepKey) {
                editor.remove(existingKey)
            }
        }
        editor
            .putString(keepKey, encoded)
            .putString(KEY_LAST_MATCH_UUID, snapshot.clientMatchUuid)
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
                Gson().fromJson(payload, MatchState::class.java)?.takeIf { it.isUsable() }
            } catch (_: JsonSyntaxException) {
                null
            }
        }

        /**
         * Gson builds the object without running the constructor, so a payload that is
         * not a match at all comes back with nulls in fields Kotlin says cannot be null.
         * Such a match would crash the scoreboard on the first point, so it is not one.
         */
        @Suppress("SENSELESS_COMPARISON")
        fun MatchState.isUsable(): Boolean =
            clientMatchUuid != null && clientMatchUuid.isNotBlank() &&
                player1 != null && player2 != null && courtId != null
    }
}
