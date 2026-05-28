package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player

class MatchStartReducerTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak")
    private val playerThree = Player(id = 3, name = "Lis", firstName = "Ewa", lastName = "Lis")
    private val playerFour = Player(id = 4, name = "Wojcik", firstName = "Anna", lastName = "Wojcik")

    @Test
    fun singlesStartsWithFirstPlayerByDefault() {
        val state = matchState()

        MatchStartReducer.start(state, serverNumber = 99, nowMs = 10_000L)

        assertEquals(1, state.currentServer)
        assertTrue(state.isPlayer1Serving)
        assertEquals(10_000L, state.matchStartTime)
    }

    @Test
    fun singlesCanStartWithSecondPlayer() {
        val state = matchState()

        MatchStartReducer.start(state, serverNumber = 2, nowMs = 10_000L)

        assertEquals(2, state.currentServer)
        assertFalse(state.isPlayer1Serving)
    }

    @Test
    fun doublesClampsServerNumberAndSetsServingTeam() {
        val state = matchState(isDoubles = true)

        MatchStartReducer.start(state, serverNumber = 3, nowMs = 10_000L)

        assertEquals(3, state.currentServer)
        assertTrue(state.isPlayer1Serving)

        MatchStartReducer.start(state, serverNumber = 9, nowMs = 20_000L)

        assertEquals(4, state.currentServer)
        assertFalse(state.isPlayer1Serving)
    }

    @Test
    fun manualStartTimeOverridesClock() {
        val state = matchState(manualStartTime = 5_000L)

        MatchStartReducer.start(state, serverNumber = 1, nowMs = 10_000L)

        assertEquals(5_000L, state.matchStartTime)
    }

    private fun matchState(
        isDoubles: Boolean = false,
        manualStartTime: Long? = null
    ): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            player3 = if (isDoubles) playerThree else null,
            player4 = if (isDoubles) playerFour else null,
            courtId = "1",
            courtName = "Court 1",
            isDoubles = isDoubles,
            manualStartTime = manualStartTime
        )
    }
}