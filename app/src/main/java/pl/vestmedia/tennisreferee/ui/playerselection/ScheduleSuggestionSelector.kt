package pl.vestmedia.tennisreferee.ui.playerselection

import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.ScheduleSuggestion

object ScheduleSuggestionSelector {
    fun selectPlayers(players: List<Player>, suggestion: ScheduleSuggestion): List<Player>? {
        val player1 = findPlayer(players, suggestion.player1?.id, suggestion.player1Name)
        val player2 = findPlayer(players, suggestion.player2?.id, suggestion.player2Name)
        if (player1 == null || player2 == null || player1.id == player2.id) {
            return null
        }
        return listOf(player1, player2)
    }

    private fun findPlayer(players: List<Player>, suggestedId: Int?, suggestedName: String): Player? {
        if (suggestedId != null && suggestedId > 0) {
            players.firstOrNull { it.id == suggestedId }?.let { return it }
        }
        val normalizedName = normalizeName(suggestedName)
        if (normalizedName.isBlank()) return null
        return players.firstOrNull { player ->
            normalizeName(player.getFullName()) == normalizedName || normalizeName(player.name) == normalizedName
        }
    }

    private fun normalizeName(value: String): String {
        return value.trim().lowercase()
    }
}