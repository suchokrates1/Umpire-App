package pl.vestmedia.tennisreferee.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchStateTest {

    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak")
    private val playerThree = Player(id = 3, name = "Lis", firstName = "Ewa", lastName = "Lis")
    private val playerFour = Player(id = 4, name = "Wojcik", firstName = "Anna", lastName = "Wojcik")

    @Test
    fun normalGameRequiresTwoPointAdvantageAfterDeuce() {
        val state = matchState().apply {
            player1Points = 4
            player2Points = 3
        }

        assertFalse(state.isGameWon())

        state.player1Points = 5
        assertTrue(state.isGameWon())
    }

    @Test
    fun noAdvantageGameEndsOnFourthPoint() {
        val state = matchState(noAdvantage = true).apply {
            player1Points = 4
            player2Points = 3
        }

        assertTrue(state.isGameWon())
        assertEquals("40", state.getPlayer1PointsDisplay())
        assertEquals("40", state.getPlayer2PointsDisplay())
    }

    @Test
    fun shortSetStartsTiebreakAtTwoAll() {
        val state = matchState(matchConfig = MatchConfig(gamesPerSet = 3)).apply {
            player1Games = 2
            player2Games = 2
        }

        assertTrue(state.shouldStartTiebreak())
    }

    @Test
    fun standardSetStartsTiebreakAtSixAll() {
        val state = matchState(matchConfig = MatchConfig(gamesPerSet = 6)).apply {
            player1Games = 6
            player2Games = 6
        }

        assertTrue(state.shouldStartTiebreak())
    }

    @Test
    fun tiebreakRequiresTwoPointAdvantage() {
        val state = matchState(matchConfig = MatchConfig(tiebreakPoints = 7)).apply {
            isTiebreak = true
            player1Points = 7
            player2Points = 6
        }

        assertFalse(state.isGameWon())

        state.player1Points = 8
        assertTrue(state.isGameWon())
    }

    @Test
    fun doublesTeamNamesIncludeBothPartnersAndServerMarker() {
        val state = matchState(isDoubles = true).apply {
            currentServer = 3
        }

        assertEquals("Kowalski / Lis", state.getTeam1DisplayName())
        assertEquals("Kowalski / 🎾 Lis", state.getTeam1ServerAwareDisplayName())
        assertEquals("Nowak / Wojcik", state.getTeam2ServerAwareDisplayName())
    }

    private fun matchState(
        isDoubles: Boolean = false,
        noAdvantage: Boolean = false,
        matchConfig: MatchConfig = MatchConfig()
    ): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            player3 = if (isDoubles) playerThree else null,
            player4 = if (isDoubles) playerFour else null,
            courtId = "1",
            courtName = "Court 1",
            isDoubles = isDoubles,
            noAdvantage = noAdvantage,
            matchConfig = matchConfig
        )
    }
}
