package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

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

    private fun applyWalkoverScore(state: MatchState, winnerName: String?) {
        val teamOneName = state.getTeam1FullName()
        val teamTwoName = state.getTeam2FullName()
        val playerOneWins = winnerName == teamOneName
        val playerTwoWins = winnerName == teamTwoName
        if (!playerOneWins && !playerTwoWins) return

        state.player1Sets = if (playerOneWins) 2 else 0
        state.player2Sets = if (playerTwoWins) 2 else 0
        state.player1Games = 0
        state.player2Games = 0
        state.player1Points = 0
        state.player2Points = 0
        state.isTiebreak = false
        state.isSuperTiebreak = false
        state.setsHistory.clear()
        state.setsHistory.add(
            SetScore(
                setNumber = 1,
                player1Games = if (playerOneWins) 4 else 0,
                player2Games = if (playerTwoWins) 4 else 0
            )
        )
        state.setsHistory.add(
            SetScore(
                setNumber = 2,
                player1Games = if (playerOneWins) 4 else 0,
                player2Games = if (playerTwoWins) 4 else 0
            )
        )
    }
}