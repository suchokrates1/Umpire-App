package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

/**
 * Dalej after leaving the match screen must reopen the same match.
 * A new MatchState would mint another client UUID and leave the 0:0 row on the schedule slot.
 */
object MatchResume {
    fun canResume(
        saved: MatchState?,
        courtId: String,
        scheduleId: Int?,
        selectedPlayers: List<Player>,
        isDoubles: Boolean,
    ): Boolean {
        if (saved == null || saved.isMatchFinished) return false
        if (saved.courtId != courtId) return false
        if (saved.isDoubles != isDoubles) return false
        if (saved.scheduleId != null && scheduleId != null && saved.scheduleId != scheduleId) return false
        return competitorNames(saved) == selectedNames(selectedPlayers, isDoubles)
    }

    private fun competitorNames(state: MatchState): Set<String> {
        val names = mutableListOf(state.player1.getFullName(), state.player2.getFullName())
        if (state.isDoubles) {
            state.player3?.let { names += it.getFullName() }
            state.player4?.let { names += it.getFullName() }
        }
        return names.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun selectedNames(selectedPlayers: List<Player>, isDoubles: Boolean): Set<String> {
        val players = if (isDoubles && selectedPlayers.size == 4) {
            listOf(selectedPlayers[0], selectedPlayers[2], selectedPlayers[1], selectedPlayers[3])
        } else {
            selectedPlayers
        }
        return players.map { it.getFullName().trim() }.filter { it.isNotEmpty() }.toSet()
    }
}
