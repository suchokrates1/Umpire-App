package pl.vestmedia.tennisreferee.ui.tutorial

import android.content.Context
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import pl.vestmedia.tennisreferee.data.localJson
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
    fun load(context: Context, id: String): TutorialSnapshot? {
        val raw = runCatching {
            context.assets.open("tutorial/snapshots/$id.json").bufferedReader().use { it.readText() }
        }.getOrNull() ?: return null
        val root = localJson.parseToJsonElement(raw).jsonObject
        val state = root["state"]?.let { localJson.decodeFromJsonElement<MatchState>(it) } ?: return null
        val players = TutorialCatalog.players(context)
        val localized = state.copy(
            player1 = players[0],
            player2 = players[1],
            courtId = TutorialCatalog.COURT_1,
            clientMatchUuid = TutorialCatalog.MATCH_UUID,
        )
        val viewName = root.text("view") ?: MatchView.BASIC_SCORING.name
        val view = runCatching { MatchView.valueOf(viewName) }.getOrDefault(MatchView.BASIC_SCORING)
        val pending = root.text("pendingAnnouncementType")
        val canUndo = (root["canUndo"] as? JsonPrimitive)?.booleanOrNull == true
        return TutorialSnapshot(
            id = root.text("id") ?: id,
            view = view,
            pendingAnnouncementType = pending,
            canUndo = canUndo,
            state = localized,
        )
    }
}

private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
