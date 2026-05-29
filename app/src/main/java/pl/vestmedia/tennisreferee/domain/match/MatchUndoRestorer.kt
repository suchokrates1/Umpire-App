package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.domain.match.model.MatchAction
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.MatchStatistics

object MatchUndoRestorer {
    fun restore(state: MatchState, action: MatchAction) {
        state.player1Points = action.previousPlayer1Points
        state.player2Points = action.previousPlayer2Points
        state.player1Games = action.previousPlayer1Games
        state.player2Games = action.previousPlayer2Games
        state.player1Sets = action.previousPlayer1Sets
        state.player2Sets = action.previousPlayer2Sets
        state.isPlayer1Serving = action.previousIsPlayer1Serving
        state.isFirstServe = action.previousIsFirstServe
        state.isTiebreak = action.previousIsTiebreak
        state.isSuperTiebreak = action.previousIsSuperTiebreak
        state.sidesSwapped = action.previousSidesSwapped
        state.totalGamesPlayed = action.previousTotalGamesPlayed
        state.currentServer = action.previousCurrentServer
        state.isMatchFinished = action.previousIsMatchFinished

        while (state.setsHistory.size > action.previousSetsHistorySize) {
            state.setsHistory.removeAt(state.setsHistory.size - 1)
        }

        restoreStatistics(state.player1Stats, action.previousPlayer1Stats)
        restoreStatistics(state.player2Stats, action.previousPlayer2Stats)
    }

    private fun restoreStatistics(target: MatchStatistics, snapshot: MatchStatistics) {
        target.aces = snapshot.aces
        target.doubleFaults = snapshot.doubleFaults
        target.winners = snapshot.winners
        target.forcedErrors = snapshot.forcedErrors
        target.unforcedErrors = snapshot.unforcedErrors
        target.firstServesIn = snapshot.firstServesIn
        target.firstServesTotal = snapshot.firstServesTotal
        target.secondServesIn = snapshot.secondServesIn
        target.secondServesTotal = snapshot.secondServesTotal
    }
}