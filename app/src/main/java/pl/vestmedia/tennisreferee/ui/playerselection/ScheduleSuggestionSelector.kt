package pl.vestmedia.tennisreferee.ui.playerselection

import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.ScheduleSuggestion

object ScheduleSuggestionSelector {
    private const val PAIR_SEPARATOR = " / "

    fun selectPlayers(players: List<Player>, suggestion: ScheduleSuggestion): List<Player>? {
        return if (suggestion.isDoubles) {
            selectDoubles(players, suggestion)
        } else {
            selectSingles(players, suggestion)
        }
    }

    fun shouldKeepScheduleIdOnDoublesToggle(
        appliedSuggestion: ScheduleSuggestion?,
        newIsDoubles: Boolean
    ): Boolean {
        return appliedSuggestion != null && appliedSuggestion.isDoubles == newIsDoubles
    }

    private fun selectSingles(players: List<Player>, suggestion: ScheduleSuggestion): List<Player>? {
        val player1 = findPlayer(players, suggestion.player1?.id, suggestion.player1Name)
        val player2 = findPlayer(players, suggestion.player2?.id, suggestion.player2Name)
        if (player1 == null || player2 == null || player1.id == player2.id) {
            return null
        }
        return listOf(player1, player2)
    }

    private fun selectDoubles(players: List<Player>, suggestion: ScheduleSuggestion): List<Player>? {
        val team1 = splitPairNames(suggestion.player1Name)
        val team2 = splitPairNames(suggestion.player2Name)
        val first = findPlayer(
            players,
            suggestion.player1?.id,
            suggestion.player1?.getFullName() ?: team1?.first.orEmpty()
        )
        val firstPartner = findPlayer(
            players,
            suggestion.player1?.partner?.id,
            suggestion.player1?.partner?.getFullName() ?: team1?.second.orEmpty()
        )
        val second = findPlayer(
            players,
            suggestion.player2?.id,
            suggestion.player2?.getFullName() ?: team2?.first.orEmpty()
        )
        val secondPartner = findPlayer(
            players,
            suggestion.player2?.partner?.id,
            suggestion.player2?.partner?.getFullName() ?: team2?.second.orEmpty()
        )
        val selected = listOfNotNull(first, firstPartner, second, secondPartner)
        if (selected.size != 4 || selected.map { it.id }.toSet().size != 4) {
            return null
        }
        return selected
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

    private fun splitPairNames(label: String): Pair<String, String>? {
        if (!label.contains(PAIR_SEPARATOR)) return null
        val left = label.substringBefore(PAIR_SEPARATOR).trim()
        val right = label.substringAfter(PAIR_SEPARATOR).trim()
        if (left.isEmpty() || right.isEmpty()) return null
        return left to right
    }

    private fun normalizeName(value: String): String {
        return value.trim().lowercase()
    }
}
