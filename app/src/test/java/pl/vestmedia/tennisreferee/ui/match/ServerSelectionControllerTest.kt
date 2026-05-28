package pl.vestmedia.tennisreferee.ui.match

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player

class ServerSelectionControllerTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak")
    private val playerThree = Player(id = 3, name = "Lis", firstName = "Ewa", lastName = "Lis")
    private val playerFour = Player(id = 4, name = "Wojcik", firstName = "Anna", lastName = "Wojcik")

    @Test
    fun singlesUsesButtonOrderWhenSidesAreNotSwapped() {
        val state = matchState()

        assertEquals(1, ServerSelectionController.resolveServerNumber(1, state))
        assertEquals(2, ServerSelectionController.resolveServerNumber(2, state))
    }

    @Test
    fun singlesMapsVisualButtonsToRealPlayersWhenSidesAreSwapped() {
        val state = matchState(sidesSwapped = true)

        assertEquals(2, ServerSelectionController.resolveServerNumber(1, state))
        assertEquals(1, ServerSelectionController.resolveServerNumber(2, state))
    }

    @Test
    fun doublesUsesButtonOrderWhenSidesAreNotSwapped() {
        val state = matchState(isDoubles = true)

        assertEquals(1, ServerSelectionController.resolveServerNumber(1, state))
        assertEquals(2, ServerSelectionController.resolveServerNumber(2, state))
        assertEquals(3, ServerSelectionController.resolveServerNumber(3, state))
        assertEquals(4, ServerSelectionController.resolveServerNumber(4, state))
    }

    @Test
    fun doublesMapsVisualButtonsToRealPlayersWhenSidesAreSwapped() {
        val state = matchState(isDoubles = true, sidesSwapped = true)

        assertEquals(2, ServerSelectionController.resolveServerNumber(1, state))
        assertEquals(1, ServerSelectionController.resolveServerNumber(2, state))
        assertEquals(4, ServerSelectionController.resolveServerNumber(3, state))
        assertEquals(3, ServerSelectionController.resolveServerNumber(4, state))
    }

    @Test
    fun invalidButtonFallsBackToFirstPlayer() {
        val singlesState = matchState()
        val doublesState = matchState(isDoubles = true, sidesSwapped = true)

        assertEquals(1, ServerSelectionController.resolveServerNumber(99, singlesState))
        assertEquals(1, ServerSelectionController.resolveServerNumber(99, doublesState))
    }

    private fun matchState(isDoubles: Boolean = false, sidesSwapped: Boolean = false): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            player3 = if (isDoubles) playerThree else null,
            player4 = if (isDoubles) playerFour else null,
            courtId = "1",
            courtName = "Court 1",
            isDoubles = isDoubles,
            sidesSwapped = sidesSwapped
        )
    }
}