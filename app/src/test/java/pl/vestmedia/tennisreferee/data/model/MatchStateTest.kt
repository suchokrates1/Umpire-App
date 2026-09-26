package pl.vestmedia.tennisreferee.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

class MatchStateTest {

    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak")
    private val playerThree = Player(id = 3, name = "Lis", firstName = "Ewa", lastName = "Lis")
    private val playerFour = Player(id = 4, name = "Wojcik", firstName = "Anna", lastName = "Wojcik")

    @Test
    fun classicGameEndsOnFourthPointWithTwoPointMargin() {
        val state = matchState().apply {
            player1Points = 4
            player2Points = 2
        }

        assertTrue(state.isGameWon())

        state.player2Points = 3
        assertFalse(state.isGameWon())
    }

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
    fun advantageDisplayOnlyMarksPlayerWithLeadAfterDeuce() {
        val state = matchState().apply {
            player1Points = 4
            player2Points = 3
        }

        assertEquals("ADV", state.getPlayer1PointsDisplay())
        assertEquals("40", state.getPlayer2PointsDisplay())

        state.player2Points = 4
        assertEquals("40", state.getPlayer1PointsDisplay())
        assertEquals("40", state.getPlayer2PointsDisplay())
    }

    @Test
    fun fourGameSetCanStartTheTiebreakAtThreeAll() {
        val config = MatchConfig(gamesPerSet = 4, tiebreakAtGames = 3)
        assertEquals(3, config.tiebreakAt)

        val state = matchState(matchConfig = config).apply {
            player1Games = 3
            player2Games = 3
        }
        assertTrue(state.shouldStartTiebreak())
        assertFalse(state.isSetWon())

        // 4:0, 4:1 and 4:2 still take the set outright.
        listOf(0, 1, 2).forEach { games ->
            state.player1Games = 4
            state.player2Games = games
            assertTrue(state.isSetWon())
        }

        // 4:3 only exists as the game won in the tiebreak, which the reducer awards itself.
        state.player1Games = 4
        state.player2Games = 3
        assertFalse(state.isSetWon())
    }

    @Test
    fun tiebreakAtGamesOutsideTheFormatFallsBackToTheDefault() {
        assertEquals(4, MatchConfig(gamesPerSet = 4).tiebreakAt)
        assertEquals(2, MatchConfig(gamesPerSet = 3).tiebreakAt)
        assertEquals(6, MatchConfig(gamesPerSet = 6).tiebreakAt)
        assertEquals(4, MatchConfig(gamesPerSet = 4, tiebreakAtGames = 9).tiebreakAt)
        assertEquals(5, MatchConfig(gamesPerSet = 6, tiebreakAtGames = 5).tiebreakAt)
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
    fun standardSetRequiresTwoGameMarginBeforeTiebreak() {
        val state = matchState(matchConfig = MatchConfig(gamesPerSet = 6)).apply {
            player1Games = 6
            player2Games = 5
        }

        assertFalse(state.isSetWon())

        state.player1Games = 7
        assertTrue(state.isSetWon())
    }

    @Test
    fun shortSetEndsAtConfiguredGames() {
        val state = matchState(matchConfig = MatchConfig(gamesPerSet = 3)).apply {
            player1Games = 3
            player2Games = 2
        }

        assertTrue(state.isSetWon())
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
    fun tiebreakPointsDisplayAsRawNumbers() {
        val state = matchState().apply {
            isTiebreak = true
            player1Points = 6
            player2Points = 5
        }

        assertEquals("6", state.getPlayer1PointsDisplay())
        assertEquals("5", state.getPlayer2PointsDisplay())
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
    fun superTiebreakRequiresConfiguredPointsAndTwoPointAdvantage() {
        val state = matchState(matchConfig = MatchConfig(superTiebreakPoints = 10)).apply {
            isSuperTiebreak = true
            player1Points = 10
            player2Points = 9
        }

        assertFalse(state.isGameWon())

        state.player1Points = 11
        assertTrue(state.isGameWon())
    }

    @Test
    fun matchEndsWhenEitherPlayerReachesSetsToWin() {
        val state = matchState(matchConfig = MatchConfig(setsToWin = 2)).apply {
            player1Sets = 1
            player2Sets = 1
        }

        assertFalse(state.shouldEndMatch())

        state.player2Sets = 2
        assertTrue(state.shouldEndMatch())
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
