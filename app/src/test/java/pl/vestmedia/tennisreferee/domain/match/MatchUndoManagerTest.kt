package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.domain.match.model.ActionType
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player

class MatchUndoManagerTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "DE")

    @Test
    fun savesSnapshotAndRestoresLastAction() {
        val state = matchState().apply {
            player1Points = 2
            player2Points = 1
            player1Stats.aces = 1
        }

        MatchUndoManager.saveStateBeforeAction(state, ActionType.ACE, "Ace Jan")
        state.player1Points = 3
        state.player2Points = 2
        state.player1Stats.aces = 2

        val result = MatchUndoManager.undoLastAction(state)

        assertEquals(2, state.player1Points)
        assertEquals(1, state.player2Points)
        assertEquals(1, state.player1Stats.aces)
        assertTrue(result is MatchUndoResult.Restored)
        assertEquals("Ace Jan", (result as MatchUndoResult.Restored).description)
        assertFalse(result.canUndo)
    }

    @Test
    fun trimsOldestSnapshotsWhenHistoryLimitIsExceeded() {
        val state = matchState()

        MatchUndoManager.saveStateBeforeAction(state, ActionType.ACE, "first", maxHistory = 2)
        MatchUndoManager.saveStateBeforeAction(state, ActionType.WINNER, "second", maxHistory = 2)
        MatchUndoManager.saveStateBeforeAction(state, ActionType.FAULT, "third", maxHistory = 2)

        assertEquals(2, state.actionsHistory.size)
        assertEquals("second", state.actionsHistory[0].description)
        assertEquals("third", state.actionsHistory[1].description)
    }

    @Test
    fun returnsNoActionWhenHistoryIsEmpty() {
        val result = MatchUndoManager.undoLastAction(matchState())

        assertTrue(result is MatchUndoResult.NoAction)
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