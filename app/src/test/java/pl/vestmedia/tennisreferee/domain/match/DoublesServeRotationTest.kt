package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player

class DoublesServeRotationTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak")
    private val playerThree = Player(id = 3, name = "Lis", firstName = "Ewa", lastName = "Lis")
    private val playerFour = Player(id = 4, name = "Wojcik", firstName = "Anna", lastName = "Wojcik")

    @Test
    fun nextServerUsesStandardDoublesOrder() {
        assertEquals(2, DoublesServeRotation.nextServer(1))
        assertEquals(3, DoublesServeRotation.nextServer(2))
        assertEquals(4, DoublesServeRotation.nextServer(3))
        assertEquals(1, DoublesServeRotation.nextServer(4))
    }

    @Test
    fun invalidServerFallsBackToFirstServer() {
        assertEquals(1, DoublesServeRotation.nextServer(0))
        assertEquals(1, DoublesServeRotation.nextServer(99))
    }

    @Test
    fun teamOneServesForSlotsOneAndThree() {
        assertTrue(DoublesServeRotation.isTeamOneServing(1))
        assertFalse(DoublesServeRotation.isTeamOneServing(2))
        assertTrue(DoublesServeRotation.isTeamOneServing(3))
        assertFalse(DoublesServeRotation.isTeamOneServing(4))
    }

    @Test
    fun rotateUpdatesServerAndServingTeamTogether() {
        val state = matchState().apply {
            currentServer = 1
            isPlayer1Serving = true
        }

        DoublesServeRotation.rotate(state)
        assertEquals(2, state.currentServer)
        assertFalse(state.isPlayer1Serving)

        DoublesServeRotation.rotate(state)
        assertEquals(3, state.currentServer)
        assertTrue(state.isPlayer1Serving)

        DoublesServeRotation.rotate(state)
        assertEquals(4, state.currentServer)
        assertFalse(state.isPlayer1Serving)

        DoublesServeRotation.rotate(state)
        assertEquals(1, state.currentServer)
        assertTrue(state.isPlayer1Serving)
    }

    private fun matchState(): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            player3 = playerThree,
            player4 = playerFour,
            courtId = "1",
            courtName = "Court 1",
            isDoubles = true
        )
    }
}