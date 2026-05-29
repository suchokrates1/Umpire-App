package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player

class MatchPointReducerTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak")
    private val playerThree = Player(id = 3, name = "Lis", firstName = "Ewa", lastName = "Lis")
    private val playerFour = Player(id = 4, name = "Wojcik", firstName = "Anna", lastName = "Wojcik")

    @Test
    fun regularPointOnlyIncrementsWinnerAndLogsPointEvent() {
        val state = matchState()

        val result = MatchPointReducer.addPoint(state, isPlayer1 = true)

        assertEquals(1, state.player1Points)
        assertEquals(0, state.player2Points)
        assertEquals(listOf(MatchPointEvent.Point), result.events)
        assertNull(result.announcementType)
        assertFalse(result.showAnnouncementImmediately)
    }

    @Test
    fun singlesTiebreakChangesServerAfterOddTotalPoints() {
        val state = matchState().apply {
            isTiebreak = true
            isPlayer1Serving = true
        }

        val result = MatchPointReducer.addPoint(state, isPlayer1 = false)

        assertEquals(0, state.player1Points)
        assertEquals(1, state.player2Points)
        assertFalse(state.isPlayer1Serving)
        assertEquals(listOf(MatchPointEvent.Point, MatchPointEvent.ServeChange), result.events)
    }

    @Test
    fun doublesTiebreakRotatesCurrentServerAfterOddTotalPoints() {
        val state = matchState(isDoubles = true).apply {
            isTiebreak = true
            currentServer = 1
            isPlayer1Serving = true
        }

        val result = MatchPointReducer.addPoint(state, isPlayer1 = false)

        assertEquals(2, state.currentServer)
        assertFalse(state.isPlayer1Serving)
        assertEquals(listOf(MatchPointEvent.Point, MatchPointEvent.ServeChange), result.events)
    }

    @Test
    fun tiebreakChangesSidesEverySixPointsWhenGameContinues() {
        val state = matchState().apply {
            isTiebreak = true
            player1Points = 3
            player2Points = 2
            sidesSwapped = false
        }

        val result = MatchPointReducer.addPoint(state, isPlayer1 = false)

        assertEquals(3, state.player1Points)
        assertEquals(3, state.player2Points)
        assertTrue(state.sidesSwapped)
        assertEquals(listOf(MatchPointEvent.Point, MatchPointEvent.SideChange), result.events)
        assertEquals(MatchPointReducer.ANNOUNCEMENT_SIDE_CHANGE, result.announcementType)
        assertTrue(result.showAnnouncementImmediately)
    }

    @Test
    fun tiebreakDoesNotChangeSidesWhenPointEndsTheGame() {
        val state = matchState().apply {
            isTiebreak = true
            player1Points = 6
            player2Points = 5
            sidesSwapped = false
        }

        val result = MatchPointReducer.addPoint(state, isPlayer1 = true)

        assertEquals(7, state.player1Points)
        assertEquals(5, state.player2Points)
        assertFalse(state.sidesSwapped)
        assertEquals(listOf(MatchPointEvent.Point), result.events)
        assertNull(result.announcementType)
        assertFalse(result.showAnnouncementImmediately)
    }

    private fun matchState(isDoubles: Boolean = false): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            player3 = if (isDoubles) playerThree else null,
            player4 = if (isDoubles) playerFour else null,
            courtId = "1",
            courtName = "Court 1",
            isDoubles = isDoubles
        )
    }
}