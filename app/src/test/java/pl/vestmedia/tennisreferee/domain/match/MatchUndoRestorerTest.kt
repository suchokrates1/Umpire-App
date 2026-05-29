package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.domain.match.model.ActionType
import pl.vestmedia.tennisreferee.domain.match.model.MatchAction
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.MatchStatistics
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

class MatchUndoRestorerTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak")

    @Test
    fun restoreRevertsScoreServeFlagsSetsHistoryAndStatistics() {
        val previousPlayer1Stats = MatchStatistics(
            aces = 1,
            doubleFaults = 2,
            winners = 3,
            forcedErrors = 4,
            unforcedErrors = 5,
            firstServesIn = 6,
            firstServesTotal = 7,
            secondServesIn = 8,
            secondServesTotal = 9
        )
        val previousPlayer2Stats = MatchStatistics(
            aces = 9,
            doubleFaults = 8,
            winners = 7,
            forcedErrors = 6,
            unforcedErrors = 5,
            firstServesIn = 4,
            firstServesTotal = 3,
            secondServesIn = 2,
            secondServesTotal = 1
        )
        val state = matchState().apply {
            player1Points = 5
            player2Points = 4
            player1Games = 6
            player2Games = 5
            player1Sets = 1
            player2Sets = 1
            isPlayer1Serving = false
            isFirstServe = true
            isTiebreak = true
            isSuperTiebreak = false
            sidesSwapped = true
            totalGamesPlayed = 11
            currentServer = 4
            isMatchFinished = true
            setsHistory.add(SetScore(setNumber = 1, player1Games = 4, player2Games = 2))
            setsHistory.add(SetScore(setNumber = 2, player1Games = 2, player2Games = 4))
            player1Stats.aces = 99
            player2Stats.doubleFaults = 99
        }
        val action = MatchAction(
            actionType = ActionType.WINNER,
            previousPlayer1Points = 2,
            previousPlayer2Points = 3,
            previousPlayer1Games = 4,
            previousPlayer2Games = 3,
            previousPlayer1Sets = 1,
            previousPlayer2Sets = 0,
            previousIsPlayer1Serving = true,
            previousIsFirstServe = false,
            previousIsTiebreak = false,
            previousIsSuperTiebreak = true,
            previousSetsHistorySize = 1,
            previousSidesSwapped = false,
            previousTotalGamesPlayed = 7,
            previousCurrentServer = 3,
            previousIsMatchFinished = false,
            previousPlayer1Stats = previousPlayer1Stats,
            previousPlayer2Stats = previousPlayer2Stats,
            description = "winner"
        )

        MatchUndoRestorer.restore(state, action)

        assertEquals(2, state.player1Points)
        assertEquals(3, state.player2Points)
        assertEquals(4, state.player1Games)
        assertEquals(3, state.player2Games)
        assertEquals(1, state.player1Sets)
        assertEquals(0, state.player2Sets)
        assertTrue(state.isPlayer1Serving)
        assertFalse(state.isFirstServe)
        assertFalse(state.isTiebreak)
        assertTrue(state.isSuperTiebreak)
        assertFalse(state.sidesSwapped)
        assertEquals(7, state.totalGamesPlayed)
        assertEquals(3, state.currentServer)
        assertFalse(state.isMatchFinished)
        assertEquals(1, state.setsHistory.size)
        assertEquals(previousPlayer1Stats, state.player1Stats)
        assertEquals(previousPlayer2Stats, state.player2Stats)
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