package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.data.model.MatchStatistics

object MatchActionReducer {
    fun reduce(state: MatchState, command: MatchCommand): MatchActionResult {
        return when (command) {
            MatchCommand.Ace -> ace(state)
            MatchCommand.Fault -> fault(state)
            MatchCommand.FootFault -> fault(state)
            MatchCommand.BallInPlay -> ballInPlay(state)
            is MatchCommand.Winner -> winner(state, command.isPlayer1)
            is MatchCommand.ForcedError -> forcedError(state, command.isPlayer1)
            is MatchCommand.UnforcedError -> unforcedError(state, command.isPlayer1)
            is MatchCommand.BasicWin -> basicWin(state, command.isPlayer1)
            MatchCommand.BasicFault -> fault(state)
        }
    }

    private fun ace(state: MatchState): MatchActionResult {
        val stats = servingStats(state)
        stats.aces++
        stats.firstServesIn++
        stats.firstServesTotal++
        val pointWinner = state.isPlayer1Serving
        state.isFirstServe = true
        return MatchActionResult(pointWinner = pointWinner)
    }

    private fun fault(state: MatchState): MatchActionResult {
        val stats = servingStats(state)
        if (state.isFirstServe) {
            stats.firstServesTotal++
            state.isFirstServe = false
            return MatchActionResult()
        }

        stats.doubleFaults++
        stats.secondServesTotal++
        val pointWinner = !state.isPlayer1Serving
        state.isFirstServe = true
        return MatchActionResult(pointWinner = pointWinner)
    }

    private fun ballInPlay(state: MatchState): MatchActionResult {
        recordServeIn(state)
        state.isFirstServe = true
        return MatchActionResult(transitionToRally = true)
    }

    private fun winner(state: MatchState, isPlayer1: Boolean): MatchActionResult {
        playerStats(state, isPlayer1).winners++
        return MatchActionResult(pointWinner = isPlayer1)
    }

    private fun forcedError(state: MatchState, isPlayer1: Boolean): MatchActionResult {
        playerStats(state, isPlayer1).forcedErrors++
        return MatchActionResult(pointWinner = !isPlayer1)
    }

    private fun unforcedError(state: MatchState, isPlayer1: Boolean): MatchActionResult {
        playerStats(state, isPlayer1).unforcedErrors++
        return MatchActionResult(pointWinner = !isPlayer1)
    }

    private fun basicWin(state: MatchState, isPlayer1: Boolean): MatchActionResult {
        playerStats(state, isPlayer1).winners++
        recordServeIn(state)
        state.isFirstServe = true
        return MatchActionResult(pointWinner = isPlayer1)
    }

    private fun recordServeIn(state: MatchState) {
        val stats = servingStats(state)
        if (state.isFirstServe) {
            stats.firstServesIn++
            stats.firstServesTotal++
        } else {
            stats.secondServesIn++
            stats.secondServesTotal++
        }
    }

    private fun servingStats(state: MatchState): MatchStatistics {
        return playerStats(state, state.isPlayer1Serving)
    }

    private fun playerStats(state: MatchState, isPlayer1: Boolean): MatchStatistics {
        return if (isPlayer1) state.player1Stats else state.player2Stats
    }
}

sealed class MatchCommand {
    object Ace : MatchCommand()
    object Fault : MatchCommand()
    object FootFault : MatchCommand()
    object BallInPlay : MatchCommand()
    data class Winner(val isPlayer1: Boolean) : MatchCommand()
    data class ForcedError(val isPlayer1: Boolean) : MatchCommand()
    data class UnforcedError(val isPlayer1: Boolean) : MatchCommand()
    data class BasicWin(val isPlayer1: Boolean) : MatchCommand()
    object BasicFault : MatchCommand()
}

data class MatchActionResult(
    val pointWinner: Boolean? = null,
    val transitionToRally: Boolean = false
)