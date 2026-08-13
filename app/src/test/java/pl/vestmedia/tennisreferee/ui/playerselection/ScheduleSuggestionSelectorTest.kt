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

    @Test
    fun selectsFourPlayersForDoublesSuggestion() {
        val players = listOf(
            Player(id = 1, name = "Anna Kowalska", firstName = "Anna", lastName = "Kowalska"),
            Player(id = 2, name = "Ewa Nowak", firstName = "Ewa", lastName = "Nowak"),
            Player(id = 3, name = "Jan Lewandowski", firstName = "Jan", lastName = "Lewandowski"),
            Player(id = 4, name = "Piotr Wiśniewski", firstName = "Piotr", lastName = "Wiśniewski"),
        )
        val suggestion = ScheduleSuggestion(
            id = 20,
            tournamentId = 2,
            player1Name = "Anna Kowalska / Ewa Nowak",
            player2Name = "Jan Lewandowski / Piotr Wiśniewski",
            isDoubles = true,
            player1 = players[0].copy(partner = players[1]),
            player2 = players[2].copy(partner = players[3]),
        )

        val selected = ScheduleSuggestionSelector.selectPlayers(players, suggestion)

        assertEquals(listOf(players[0], players[1], players[2], players[3]), selected)
    }

    @Test
    fun selectsDoublesPlayersFromPairLabelsWhenIdsAreMissing() {
        val players = listOf(
            Player(id = 1, name = "Anna Kowalska", firstName = "Anna", lastName = "Kowalska"),
            Player(id = 2, name = "Ewa Nowak", firstName = "Ewa", lastName = "Nowak"),
            Player(id = 3, name = "Jan Lewandowski", firstName = "Jan", lastName = "Lewandowski"),
            Player(id = 4, name = "Piotr Wiśniewski", firstName = "Piotr", lastName = "Wiśniewski"),
        )
        val suggestion = ScheduleSuggestion(
            id = 21,
            tournamentId = 2,
            player1Name = "Anna Kowalska / Ewa Nowak",
            player2Name = "Jan Lewandowski / Piotr Wiśniewski",
            isDoubles = true,
        )

        val selected = ScheduleSuggestionSelector.selectPlayers(players, suggestion)

        assertEquals(listOf(players[0], players[1], players[2], players[3]), selected)
    }

    @Test
    fun returnsNullWhenDoublesPartnerIsMissing() {
        val suggestion = ScheduleSuggestion(
            id = 22,
            tournamentId = 2,
            player1Name = "Anna Kowalska / Ewa Nowak",
            player2Name = "Jan Lewandowski / Piotr Wiśniewski",
            isDoubles = true,
        )

        val selected = ScheduleSuggestionSelector.selectPlayers(
            listOf(
                Player(id = 1, name = "Anna Kowalska", firstName = "Anna", lastName = "Kowalska"),
                Player(id = 3, name = "Jan Lewandowski", firstName = "Jan", lastName = "Lewandowski"),
            ),
            suggestion,
        )

        assertNull(selected)
    }

    @Test
    fun keepsScheduleIdWhenDoublesToggleMatchesAppliedSuggestion() {
        val suggestion = ScheduleSuggestion(
            id = 23,
            tournamentId = 2,
            player1Name = "Anna Kowalska / Ewa Nowak",
            player2Name = "Jan Lewandowski / Piotr Wiśniewski",
            isDoubles = true,
        )

        assertEquals(
            true,
            ScheduleSuggestionSelector.shouldKeepScheduleIdOnDoublesToggle(suggestion, true)
        )
        assertEquals(
            false,
            ScheduleSuggestionSelector.shouldKeepScheduleIdOnDoublesToggle(suggestion, false)
        )
        assertEquals(
            false,
            ScheduleSuggestionSelector.shouldKeepScheduleIdOnDoublesToggle(null, true)
        )
    }
}