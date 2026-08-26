package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player

class MatchProgressReducerTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak")
    private val playerThree = Player(id = 3, name = "Lis", firstName = "Ewa", lastName = "Lis")
    private val playerFour = Player(id = 4, name = "Wojcik", firstName = "Anna", lastName = "Wojcik")

    @Test
    fun normalGameAddsGameResetsPointsChangesServerAndRequestsSync() {
        val state = matchState().apply {
            matchStartTime = 1_000L
            isPlayer1Serving = true
            player1Points = 4
            player2Points = 1
        }

        val result = MatchProgressReducer.reduceAfterPoint(state, currentAnnouncementType = null, nowMs = 9_000L)

        assertEquals(1, state.player1Games)
        assertEquals(0, state.player1Points)
        assertEquals(0, state.player2Points)
        assertFalse(state.isPlayer1Serving)
        assertTrue(state.sidesSwapped)
        assertEquals(MatchProgressReducer.ANNOUNCEMENT_SIDE_CHANGE, result.announcementType)
        assertEquals(MatchProgressScreen.Announcement, result.nextScreen)
        assertEquals(listOf(MatchProgressEvent.Game), result.events)
        assertTrue(result.publishState)
        assertTrue(result.syncMatch)
        assertFalse(result.finalizeMatch)
    }

    @Test
    fun noAdvantageDeuceShowsDecidingPointWithoutSync() {
        val state = matchState(noAdvantage = true).apply {
            player1Points = 3
            player2Points = 3
        }

        val result = MatchProgressReducer.reduceAfterPoint(state, currentAnnouncementType = null, nowMs = 9_000L)

        assertEquals(MatchProgressReducer.ANNOUNCEMENT_DECIDING_POINT, result.announcementType)
        assertEquals(MatchProgressScreen.Announcement, result.nextScreen)
        assertTrue(result.publishState)
        assertFalse(result.syncMatch)
        assertFalse(result.finalizeMatch)
    }

    @Test
    fun setWinAddsSetHistoryAndStartsNextSet() {
        val state = matchState(matchConfig = MatchConfig(gamesPerSet = 6, setsToWin = 2)).apply {
            isPlayer1Serving = true
            player1Games = 5
            player2Games = 4
            player1Points = 4
            player2Points = 0
        }

        val result = MatchProgressReducer.reduceAfterPoint(state, currentAnnouncementType = null, nowMs = 9_000L)

        assertEquals(1, state.player1Sets)
        assertEquals(0, state.player2Sets)
        assertEquals(0, state.player1Games)
        assertEquals(0, state.player2Games)
        assertEquals(1, state.setsHistory.size)
        assertEquals(6, state.setsHistory[0].player1Games)
        assertEquals(4, state.setsHistory[0].player2Games)
        assertEquals(listOf(MatchProgressEvent.Game, MatchProgressEvent.Set), result.events)
        assertTrue(result.syncMatch)
        assertFalse(result.finalizeMatch)
    }

    @Test
    fun gameAtTiebreakBoundaryStartsTiebreakAnnouncement() {
        val state = matchState(matchConfig = MatchConfig(gamesPerSet = 6, setsToWin = 2)).apply {
            player1Games = 5
            player2Games = 6
            player1Points = 4
            player2Points = 0
        }

        val result = MatchProgressReducer.reduceAfterPoint(state, currentAnnouncementType = null, nowMs = 9_000L)

        assertTrue(state.isTiebreak)
        assertEquals(6, state.player1Games)
        assertEquals(6, state.player2Games)
        assertEquals(2, state.tiebreakOpeningServer)
        assertFalse(state.isPlayer1Serving)
        assertEquals(MatchProgressReducer.ANNOUNCEMENT_TIEBREAK, result.announcementType)
        assertEquals(MatchProgressScreen.Announcement, result.nextScreen)
    }

    @Test
    fun splitSetsStartSuperTiebreak() {
        val state = matchState(matchConfig = MatchConfig(gamesPerSet = 6, setsToWin = 2)).apply {
            player1Sets = 0
            player2Sets = 1
            player1Games = 5
            player2Games = 4
            player1Points = 4
            player2Points = 0
        }

        val result = MatchProgressReducer.reduceAfterPoint(state, currentAnnouncementType = null, nowMs = 9_000L)

        assertEquals(1, state.player1Sets)
        assertEquals(1, state.player2Sets)
        assertTrue(state.isSuperTiebreak)
        assertEquals(2, state.tiebreakOpeningServer)
        assertFalse(state.isPlayer1Serving)
        assertEquals(MatchProgressReducer.ANNOUNCEMENT_SUPER_TIEBREAK, result.announcementType)
        assertEquals(MatchProgressScreen.Announcement, result.nextScreen)
    }

    @Test
    fun matchWinMarksFinishedAndRequestsFinalizeWithoutSync() {
        val state = matchState(matchConfig = MatchConfig(gamesPerSet = 6, setsToWin = 2)).apply {
            matchStartTime = 1_000L
            player1Sets = 1
            player2Sets = 0
            player1Games = 5
            player2Games = 4
            player1Points = 4
            player2Points = 0
        }

        val result = MatchProgressReducer.reduceAfterPoint(state, currentAnnouncementType = null, nowMs = 9_000L)

        assertTrue(state.isMatchFinished)
        assertEquals(8_000L, state.matchDuration)
        assertEquals(2, state.player1Sets)
        assertEquals(6, state.player1Games)
        assertEquals(4, state.player2Games)
        assertEquals(MatchProgressScreen.MatchFinished, result.nextScreen)
        assertEquals(listOf(MatchProgressEvent.Game, MatchProgressEvent.Set), result.events)
        assertTrue(result.publishState)
        assertFalse(result.syncMatch)
        assertTrue(result.finalizeMatch)
    }

    @Test
    fun afterSetTiebreakOpeningServerReceivesTheNextSet() {
        // ITF: 7-3 ends on an even total. The old extra rotate from the last
        // TB server wrongly gave the next set back to the opening server.
        val state = startSetTiebreak(player1Opens = true)

        playTiebreak(state, listOf(true, false, true, false, true, false, true, true, true, true))

        assertEquals(1, state.player1Sets)
        assertEquals(0, state.player2Sets)
        assertFalse(state.isTiebreak)
        assertFalse(state.isPlayer1Serving)
        assertEquals(2, state.currentServer)
    }

    @Test
    fun afterSetTiebreakPlayerTwoOpeningServerReceivesTheNextSet() {
        val state = startSetTiebreak(player1Opens = false)

        playTiebreak(state, listOf(false, true, false, true, false, true, false, false, false, false))

        assertEquals(0, state.player1Sets)
        assertEquals(1, state.player2Sets)
        assertTrue(state.isPlayer1Serving)
        assertEquals(1, state.currentServer)
    }

    @Test
    fun afterSetTiebreakAtSevenFourOpeningServerStillReceives() {
        val state = startSetTiebreak(player1Opens = true)

        playTiebreak(state, listOf(true, false, true, false, true, false, true, false, true, true, true))

        assertFalse(state.isPlayer1Serving)
        assertEquals(2, state.currentServer)
    }

    @Test
    fun doublesAfterSetTiebreakNextServerIsPartnerOfOpeningTeamOpponent() {
        val state = startSetTiebreak(player1Opens = true, isDoubles = true).apply {
            currentServer = 1
            tiebreakOpeningServer = 1
        }

        playTiebreak(state, listOf(true, false, true, false, true, false, true, true, true, true))

        assertEquals(2, state.currentServer)
        assertFalse(state.isPlayer1Serving)
    }

    private fun startSetTiebreak(player1Opens: Boolean, isDoubles: Boolean = false): MatchState {
        return matchState(
            matchConfig = MatchConfig(gamesPerSet = 6, setsToWin = 2),
            isDoubles = isDoubles
        ).apply {
            isTiebreak = true
            isPlayer1Serving = player1Opens
            currentServer = if (player1Opens) 1 else 2
            tiebreakOpeningServer = currentServer
            player1Games = 6
            player2Games = 6
        }
    }

    private fun playTiebreak(state: MatchState, pointWinnersArePlayer1: List<Boolean>) {
        pointWinnersArePlayer1.forEach { player1Won ->
            MatchPointReducer.addPoint(state, player1Won)
            MatchProgressReducer.reduceAfterPoint(state, currentAnnouncementType = null, nowMs = 9_000L)
        }
    }

    private fun matchState(
        noAdvantage: Boolean = false,
        matchConfig: MatchConfig = MatchConfig(),
        isDoubles: Boolean = false
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