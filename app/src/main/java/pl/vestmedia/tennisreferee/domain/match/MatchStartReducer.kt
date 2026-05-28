package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.data.model.MatchState

object MatchStartReducer {
    fun start(state: MatchState, serverNumber: Int, nowMs: Long) {
        state.currentServer = when {
            state.isDoubles -> serverNumber.coerceIn(1, 4)
            serverNumber == 2 -> 2
            else -> 1
        }
        state.isPlayer1Serving = if (state.isDoubles) {
            DoublesServeRotation.isTeamOneServing(state.currentServer)
        } else {
            state.currentServer == 1
        }
        state.matchStartTime = state.manualStartTime ?: nowMs
    }
}