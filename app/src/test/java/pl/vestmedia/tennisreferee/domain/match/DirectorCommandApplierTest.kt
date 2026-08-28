package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.dto.DirectorCommandDto
import pl.vestmedia.tennisreferee.data.api.dto.DirectorScoreDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchConfigDto
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

class DirectorCommandApplierTest {
    private val playerOne = Player(id = 1, name = "Emil Stopierzyński", firstName = "Emil", lastName = "Stopierzyński", flag = "PL")
    private val playerTwo = Player(id = 2, name = "Courtney Webeck", firstName = "Courtney", lastName = "Webeck", flag = "AU")

    @Test
    fun appliesCourtNamesScoreAndRulesToLiveState() {
        val state = MatchState(
            matchId = 671,
            clientMatchUuid = "uuid-gonzalez",
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "t31-2",
            courtName = "Kort 2"
        ).apply {
            player1Games = 0
            player2Games = 3
        }

        val next = DirectorCommandApplier.apply(
            state,
            DirectorCommandDto(
                id = "cmd-1",
                matchId = 671,
                clientMatchUuid = "uuid-gonzalez",
                courtId = "t31-8",
                courtName = "Kort 8",
                player1Name = "Jessica González",
                player2Name = "Daniela Schmidt",
                score = DirectorScoreDto(player1Games = 0, player2Games = 4, player2Sets = 1),
                matchConfig = MatchConfigDto(gamesPerSet = 4, setsToWin = 2, noAdvantage = true)
            )
        )

        assertEquals("t31-8", next.courtId)
        assertEquals("Kort 8", next.courtName)
        assertEquals("Jessica González", next.player1.getFullName())
        assertEquals("Daniela Schmidt", next.player2.getFullName())
        assertEquals(1, next.player2Sets)
        assertEquals(4, next.player2Games)
        assertTrue(next.matchConfig.noAdvantage)
        assertTrue(next.noAdvantage)
    }

    @Test
    fun ignoresCommandForAnotherMatch() {
        val state = MatchState(
            matchId = 667,
            clientMatchUuid = "uuid-justyna",
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "t31-2",
            courtName = "Kort 2"
        )
        val command = DirectorCommandDto(matchId = 671, clientMatchUuid = "uuid-gonzalez", courtId = "t31-8")
        assertFalse(DirectorCommandApplier.appliesTo(state, command))
    }

    @Test
    fun doublesRenameUpdatesTeamDisplayNames() {
        val state = MatchState(
            matchId = 10,
            clientMatchUuid = "uuid-doubles",
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "t31-2",
            courtName = "Kort 2",
            isDoubles = true,
            team1Name = "Old Pair A",
            team2Name = "Old Pair B"
        )
        val next = DirectorCommandApplier.apply(
            state,
            DirectorCommandDto(
                matchId = 10,
                clientMatchUuid = "uuid-doubles",
                player1Name = "New Pair A",
                player2Name = "New Pair B"
            )
        )
        assertEquals("New Pair A", next.getTeam1FullName())
        assertEquals("New Pair B", next.getTeam2FullName())
    }
}
