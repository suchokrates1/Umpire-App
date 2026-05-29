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

    private fun matchState(
        noAdvantage: Boolean = false,
        matchConfig: MatchConfig = MatchConfig()
    ): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "1",
            courtName = "Court 1",
            noAdvantage = noAdvantage,
            matchConfig = matchConfig
        )
    }
}