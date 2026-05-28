package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player

class MatchActionReducerTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak")

    @Test
    fun aceRecordsFirstServeStatsAndAwardsPointToServer() {
        val state = matchState().apply {
            isPlayer1Serving = true
            isFirstServe = true
        }

        val result = MatchActionReducer.reduce(state, MatchCommand.Ace)

        assertEquals(1, state.player1Stats.aces)
        assertEquals(1, state.player1Stats.firstServesIn)
        assertEquals(1, state.player1Stats.firstServesTotal)
        assertTrue(state.isFirstServe)
        assertEquals(true, result.pointWinner)
    }

    @Test
    fun firstFaultMovesToSecondServeWithoutPoint() {
        val state = matchState().apply {
            isPlayer1Serving = false
            isFirstServe = true
        }

        val result = MatchActionReducer.reduce(state, MatchCommand.Fault)

        assertEquals(1, state.player2Stats.firstServesTotal)
        assertFalse(state.isFirstServe)
        assertNull(result.pointWinner)
    }

    @Test
    fun secondFaultRecordsDoubleFaultAndAwardsPointToReceiver() {
        val state = matchState().apply {
            isPlayer1Serving = false
            isFirstServe = false
        }

        val result = MatchActionReducer.reduce(state, MatchCommand.Fault)

        assertEquals(1, state.player2Stats.doubleFaults)
        assertEquals(1, state.player2Stats.secondServesTotal)
        assertTrue(state.isFirstServe)
        assertEquals(true, result.pointWinner)
    }

    @Test
    fun footFaultUsesSameScoringAsFault() {
        val state = matchState().apply {
            isPlayer1Serving = true
            isFirstServe = false
        }

        val result = MatchActionReducer.reduce(state, MatchCommand.FootFault)

        assertEquals(1, state.player1Stats.doubleFaults)
        assertEquals(1, state.player1Stats.secondServesTotal)
        assertEquals(false, result.pointWinner)
    }

    @Test
    fun ballInPlayRecordsServeInAndTransitionsToRally() {
        val state = matchState().apply {
            isPlayer1Serving = true
            isFirstServe = false
        }

        val result = MatchActionReducer.reduce(state, MatchCommand.BallInPlay)

        assertEquals(1, state.player1Stats.secondServesIn)
        assertEquals(1, state.player1Stats.secondServesTotal)
        assertTrue(state.isFirstServe)
        assertTrue(result.transitionToRally)
        assertNull(result.pointWinner)
    }

    @Test
    fun winnerAwardsPointToWinner() {
        val state = matchState()

        val result = MatchActionReducer.reduce(state, MatchCommand.Winner(isPlayer1 = false))

        assertEquals(1, state.player2Stats.winners)
        assertEquals(false, result.pointWinner)
    }

    @Test
    fun errorsAwardPointToOpponent() {
        val forcedState = matchState()
        val unforcedState = matchState()

        val forcedResult = MatchActionReducer.reduce(forcedState, MatchCommand.ForcedError(isPlayer1 = true))
        val unforcedResult = MatchActionReducer.reduce(unforcedState, MatchCommand.UnforcedError(isPlayer1 = false))

        assertEquals(1, forcedState.player1Stats.forcedErrors)
        assertEquals(false, forcedResult.pointWinner)
        assertEquals(1, unforcedState.player2Stats.unforcedErrors)
        assertEquals(true, unforcedResult.pointWinner)
    }

    @Test
    fun basicWinRecordsWinnerAndServeIn() {
        val state = matchState().apply {
            isPlayer1Serving = false
            isFirstServe = true
        }

        val result = MatchActionReducer.reduce(state, MatchCommand.BasicWin(isPlayer1 = true))

        assertEquals(1, state.player1Stats.winners)
        assertEquals(1, state.player2Stats.firstServesIn)
        assertEquals(1, state.player2Stats.firstServesTotal)
        assertTrue(state.isFirstServe)
        assertEquals(true, result.pointWinner)
    }

    @Test
    fun basicFaultUsesSameScoringAsFault() {
        val state = matchState().apply {
            isPlayer1Serving = true
            isFirstServe = true
        }

        val result = MatchActionReducer.reduce(state, MatchCommand.BasicFault)

        assertEquals(1, state.player1Stats.firstServesTotal)
        assertFalse(state.isFirstServe)
        assertNull(result.pointWinner)
    }

    private fun matchState(): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "1",
            courtName = "Court 1"
        )
    }
}