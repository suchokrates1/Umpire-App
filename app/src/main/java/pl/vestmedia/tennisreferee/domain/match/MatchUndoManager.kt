package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.data.model.ActionType
import pl.vestmedia.tennisreferee.data.model.MatchAction
import pl.vestmedia.tennisreferee.data.model.MatchState

object MatchUndoManager {
    private const val DEFAULT_MAX_HISTORY = 100

    fun saveStateBeforeAction(
        state: MatchState,
        actionType: ActionType,
        description: String,
        maxHistory: Int = DEFAULT_MAX_HISTORY
    ) {
        val action = MatchAction(
            actionType = actionType,
            previousPlayer1Points = state.player1Points,
            previousPlayer2Points = state.player2Points,
            previousPlayer1Games = state.player1Games,
            previousPlayer2Games = state.player2Games,
            previousPlayer1Sets = state.player1Sets,
            previousPlayer2Sets = state.player2Sets,
            previousIsPlayer1Serving = state.isPlayer1Serving,
            previousIsFirstServe = state.isFirstServe,
            previousIsTiebreak = state.isTiebreak,
            previousIsSuperTiebreak = state.isSuperTiebreak,
            previousSetsHistorySize = state.setsHistory.size,
            previousSidesSwapped = state.sidesSwapped,
            previousTotalGamesPlayed = state.totalGamesPlayed,
            previousCurrentServer = state.currentServer,
            previousIsMatchFinished = state.isMatchFinished,
            previousPlayer1Stats = state.player1Stats.copy(),
            previousPlayer2Stats = state.player2Stats.copy(),
            description = description
        )

        state.actionsHistory.add(action)
        while (state.actionsHistory.size > maxHistory) {
            state.actionsHistory.removeAt(0)
        }
    }

    fun undoLastAction(state: MatchState): MatchUndoResult {
        if (state.actionsHistory.isEmpty()) {
            return MatchUndoResult.NoAction
        }

        val lastAction = state.actionsHistory.removeAt(state.actionsHistory.size - 1)
        MatchUndoRestorer.restore(state, lastAction)
        return MatchUndoResult.Restored(
            description = lastAction.description,
            canUndo = state.actionsHistory.isNotEmpty()
        )
    }
}

sealed class MatchUndoResult {
    data object NoAction : MatchUndoResult()

    data class Restored(
        val description: String,
        val canUndo: Boolean
    ) : MatchUndoResult()
}