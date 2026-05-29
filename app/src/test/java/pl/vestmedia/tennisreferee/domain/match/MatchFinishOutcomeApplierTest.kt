package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player

class MatchFinishOutcomeApplierTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "DE")

    @Test
    fun appliesWalkoverScoreForSelectedWinner() {
        val state = matchState().apply {
            matchStartTime = 1_000L
            player1Games = 3
            player2Games = 2
        }

        MatchFinishOutcomeApplier.apply(
            state,
            FinishMatchRequest(
                finishReason = MatchFinishReason.WALKOVER,
                winnerName = "Adam Nowak"
            ),
            nowMs = 11_000L
        )

        assertTrue(state.isMatchFinished)
        assertEquals(MatchFinishReason.WALKOVER, state.finishReason)
        assertEquals("Adam Nowak", state.finishWinnerName)
        assertEquals(0, state.player1Sets)
        assertEquals(2, state.player2Sets)
        assertEquals(2, state.setsHistory.size)
        assertEquals(0, state.setsHistory[0].player1Games)
        assertEquals(4, state.setsHistory[0].player2Games)
        assertEquals(10_000L, state.matchDuration)
    }

    @Test
    fun appliesRetirementWithoutOverwritingCurrentScore() {
        val state = matchState().apply {
            player1Sets = 1
            player2Sets = 0
            player1Games = 2
            player2Games = 1
        }

        MatchFinishOutcomeApplier.apply(
            state,
            FinishMatchRequest(
                finishReason = MatchFinishReason.RETIREMENT,
                winnerName = "Jan Kowalski",
                injuredPlayerName = "Adam Nowak"
            ),
            nowMs = 5_000L
        )

        assertTrue(state.isMatchFinished)
        assertEquals(MatchFinishReason.RETIREMENT, state.finishReason)
        assertEquals("Jan Kowalski", state.finishWinnerName)
        assertEquals("Adam Nowak", state.injuredPlayerName)
        assertEquals(1, state.player1Sets)
        assertEquals(0, state.player2Sets)
        assertEquals(2, state.player1Games)
        assertEquals(1, state.player2Games)
    }

    @Test
    fun appliesTestFinishWithoutChangingScore() {
        val state = matchState().apply {
            player1Games = 1
            player2Games = 0
        }

        MatchFinishOutcomeApplier.apply(
            state,
            FinishMatchRequest(finishReason = MatchFinishReason.TEST),
            nowMs = 5_000L
        )

        assertTrue(state.isMatchFinished)
        assertEquals(MatchFinishReason.TEST, state.finishReason)
        assertEquals(1, state.player1Games)
        assertEquals(0, state.player2Games)
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