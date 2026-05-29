package pl.vestmedia.tennisreferee.ui.playerselection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.ScheduleSuggestion

class ScheduleSuggestionSelectorTest {
    @Test
    fun selectsPlayersBySuggestedIds() {
        val players = listOf(
            Player(id = 1, name = "Jan Kowalski", firstName = "Jan", lastName = "Kowalski"),
            Player(id = 2, name = "Adam Nowak", firstName = "Adam", lastName = "Nowak"),
        )
        val suggestion = ScheduleSuggestion(
            id = 10,
            tournamentId = 2,
            player1Name = "Someone Else",
            player2Name = "Another Name",
            player1 = players[0],
            player2 = players[1],
        )

        val selected = ScheduleSuggestionSelector.selectPlayers(players, suggestion)

        assertEquals(listOf(players[0], players[1]), selected)
    }

    @Test
    fun fallsBackToFullNameWhenIdsAreMissing() {
        val players = listOf(
            Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski"),
            Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak"),
        )
        val suggestion = ScheduleSuggestion(
            id = 11,
            tournamentId = 2,
            player1Name = "Jan Kowalski",
            player2Name = "Adam Nowak",
        )

        val selected = ScheduleSuggestionSelector.selectPlayers(players, suggestion)

        assertEquals(listOf(players[0], players[1]), selected)
    }

    @Test
    fun returnsNullWhenSuggestedPlayerIsMissing() {
        val suggestion = ScheduleSuggestion(
            id = 12,
            tournamentId = 2,
            player1Name = "Jan Kowalski",
            player2Name = "Missing Player",
        )

        val selected = ScheduleSuggestionSelector.selectPlayers(
            listOf(Player(id = 1, name = "Jan Kowalski", firstName = "Jan", lastName = "Kowalski")),
            suggestion,
        )

        assertNull(selected)
    }
}