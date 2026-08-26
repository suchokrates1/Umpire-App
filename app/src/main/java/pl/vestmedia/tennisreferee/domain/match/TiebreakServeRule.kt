package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.domain.match.model.MatchState

/**
 * ITF Rules of Tennis, tie-break order of service:
 * the player/team that served the first point of the tie-break receives
 * in the first game of the following set (or match tie-break).
 *
 * During the TB the serve rotates 1 point, then 2 points each. That is one
 * service game, not a sequence of game-end rotations. After the TB we must
 * therefore restart from the opening server, not from whoever served last.
 */
object TiebreakServeRule {
    fun captureOpeningServer(state: MatchState) {
        state.tiebreakOpeningServer = currentServerSlot(state)
    }

    fun assignFirstGameOfNextSet(state: MatchState) {
        val opening = state.tiebreakOpeningServer
        if (state.isDoubles) {
            state.currentServer = DoublesServeRotation.nextServer(opening)
            state.isPlayer1Serving = DoublesServeRotation.isTeamOneServing(state.currentServer)
        } else {
            state.isPlayer1Serving = opening != 1
            state.currentServer = if (state.isPlayer1Serving) 1 else 2
        }
    }

    fun rotate(state: MatchState) {
        if (state.isDoubles) {
            DoublesServeRotation.rotate(state)
        } else {
            state.isPlayer1Serving = !state.isPlayer1Serving
            state.currentServer = if (state.isPlayer1Serving) 1 else 2
        }
    }

    private fun currentServerSlot(state: MatchState): Int {
        return if (state.isDoubles) {
            state.currentServer
        } else if (state.isPlayer1Serving) {
            1
        } else {
            2
        }
    }
}
