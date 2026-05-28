package pl.vestmedia.tennisreferee.ui.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun singlesButtonStatesHideDoublesButtonsAndMarkSelectedVisualSide() {
        val state = matchState(sidesSwapped = true).apply {
            currentServer = 2
        }

        val buttons = ServerSelectionController.buildButtonStates(state)

        assertEquals(4, buttons.size)
        assertEquals(1, buttons[0].buttonIndex)
        assertEquals(2, buttons[0].serverNumber)
        assertEquals("• Nowak", buttons[0].label)
        assertTrue(buttons[0].visible)
        assertTrue(buttons[0].selected)
        assertEquals(ServerSelectionController.ButtonColorRole.Singles, buttons[0].colorRole)

        assertEquals(2, buttons[1].buttonIndex)
        assertEquals(1, buttons[1].serverNumber)
        assertEquals("Kowalski", buttons[1].label)
        assertTrue(buttons[1].visible)
        assertFalse(buttons[1].selected)

        assertFalse(buttons[2].visible)
        assertFalse(buttons[3].visible)
    }

    @Test
    fun doublesButtonStatesUseTeamColorsAndSwappedVisualSlots() {
        val state = matchState(isDoubles = true, sidesSwapped = true).apply {
            currentServer = 4
        }

        val buttons = ServerSelectionController.buildButtonStates(state)

        assertEquals(2, buttons[0].serverNumber)
        assertEquals("Nowak", buttons[0].label)
        assertEquals(ServerSelectionController.ButtonColorRole.Team2, buttons[0].colorRole)

        assertEquals(1, buttons[1].serverNumber)
        assertEquals("Kowalski", buttons[1].label)
        assertEquals(ServerSelectionController.ButtonColorRole.Team1, buttons[1].colorRole)

        assertEquals(4, buttons[2].serverNumber)
        assertEquals("🎾 Wojcik", buttons[2].label)
        assertTrue(buttons[2].visible)
        assertTrue(buttons[2].selected)
        assertEquals(ServerSelectionController.ButtonColorRole.Team2, buttons[2].colorRole)

        assertEquals(3, buttons[3].serverNumber)
        assertEquals("Lis", buttons[3].label)
        assertTrue(buttons[3].visible)
        assertFalse(buttons[3].selected)
        assertEquals(ServerSelectionController.ButtonColorRole.Team1, buttons[3].colorRole)
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