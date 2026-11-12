package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

object MatchFinishOutcomeApplier {
    fun apply(state: MatchState, request: FinishMatchRequest, nowMs: Long) {
        state.finishReason = request.finishReason
        state.finishWinnerName = request.winnerName
        state.injuredPlayerName = request.injuredPlayerName
        state.resultNote = request.resultNote

        if (request.finishReason == MatchFinishReason.WALKOVER) {
            applyWalkoverScore(state, request.winnerName)
        }

        state.isMatchFinished = true
        state.matchDuration = if (state.matchStartTime > 0L) {
            nowMs - state.matchStartTime
        } else {
            state.matchDuration
        }
    }

    /**
     * A walkover is a win with no score: nothing was played, so there are no sets and no
     * games. The server stores it the same way (ATP Rulebook 2026, chapter IV: a walkover
     * does not count towards the percentage of sets or games), and the winner is the
     * name the umpire picked, kept in finishWinnerName.
     */
    private fun applyWalkoverScore(state: MatchState, winnerName: String?) {
        if (winnerName != state.getTeam1FullName() && winnerName != state.getTeam2FullName()) return

        state.player1Sets = 0
        state.player2Sets = 0
        state.player1Games = 0
        state.player2Games = 0
        state.player1Points = 0
        state.player2Points = 0
        state.isTiebreak = false
        state.isSuperTiebreak = false
        state.setsHistory.clear()
    }
}
