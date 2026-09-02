package pl.vestmedia.tennisreferee.ui.tutorial

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.ui.match.MatchView

data class TutorialSnapshot(
    val id: String,
    val view: MatchView,
    val pendingAnnouncementType: String?,
    val canUndo: Boolean,
    val state: MatchState,
)

object TutorialSnapshots {
    private val gson = Gson()

    fun load(context: Context, id: String): TutorialSnapshot? {
        val raw = runCatching {
            context.assets.open("tutorial/snapshots/$id.json").bufferedReader().use { it.readText() }
        }.getOrNull() ?: return null
        val root = gson.fromJson(raw, JsonObject::class.java)
        val state = gson.fromJson(root.get("state"), MatchState::class.java) ?: return null
        val players = TutorialCatalog.players(context)
        val localized = state.copy(
            player1 = players[0],
            player2 = players[1],
            courtId = TutorialCatalog.COURT_1,
            clientMatchUuid = TutorialCatalog.MATCH_UUID,
        )
        val viewName = root.get("view")?.asString ?: MatchView.BASIC_SCORING.name
        val view = runCatching { MatchView.valueOf(viewName) }.getOrDefault(MatchView.BASIC_SCORING)
        val pending = root.get("pendingAnnouncementType")?.takeUnless { it.isJsonNull }?.asString
        val canUndo = root.get("canUndo")?.asBoolean == true
        return TutorialSnapshot(
            id = root.get("id")?.asString ?: id,
            view = view,
            pendingAnnouncementType = pending,
            canUndo = canUndo,
            state = localized,
        )
    }
}
