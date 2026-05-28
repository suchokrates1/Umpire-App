package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.data.model.MatchState

object DoublesServeRotation {
    fun nextServer(currentServer: Int): Int {
        return when (currentServer) {
            1 -> 2
            2 -> 3
            3 -> 4
            4 -> 1
            else -> 1
        }
    }

    fun isTeamOneServing(currentServer: Int): Boolean {
        return currentServer == 1 || currentServer == 3
    }

    fun rotate(state: MatchState) {
        state.currentServer = nextServer(state.currentServer)
        state.isPlayer1Serving = isTeamOneServing(state.currentServer)
    }
}