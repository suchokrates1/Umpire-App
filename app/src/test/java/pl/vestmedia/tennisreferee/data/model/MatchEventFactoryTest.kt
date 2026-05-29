package pl.vestmedia.tennisreferee.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

class MatchEventFactoryTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "DE")
    private val playerThree = Player(id = 3, name = "Lis", firstName = "Ewa", lastName = "Lis", flag = "PL")
    private val playerFour = Player(id = 4, name = "Wojcik", firstName = "Anna", lastName = "Wojcik", flag = "DE")

    @Test
    fun createsSinglesEventWithScoreStatsBatteryAndTimestamp() {
        val state = singlesState().apply {
            matchId = 42
            isPlayer1Serving = true
            player1Sets = 1
            player2Sets = 0
            player1Games = 4
            player2Games = 3
            player1Points = 2
            player2Points = 1
            isTiebreak = true
            player1Stats.aces = 2
            player1Stats.firstServesIn = 3
            player1Stats.firstServesTotal = 4
            player2Stats.doubleFaults = 1
            setsHistory.add(SetScore(setNumber = 1, player1Games = 4, player2Games = 2))
        }

        val event = MatchEventFactory.create(
            state = state,
            eventType = "point",
            batteryLevel = 87,
            isCharging = true,
            timestamp = 12_345L
        )

        assertEquals("1", event.courtId)
        assertEquals(42, event.matchId)
        assertEquals(state.clientMatchUuid, event.clientMatchUuid)
        assertEquals("point", event.eventType)
        assertEquals("Kowalski", event.player1.name)
        assertEquals("Jan Kowalski", event.player1.fullName)
        assertEquals("PL", event.player1.flag)
        assertTrue(event.player1.isServing)
        assertEquals("Nowak", event.player2.name)
        assertFalse(event.player2.isServing)
        assertEquals(1, event.score.player1Sets)
        assertEquals(4, event.score.player1Games)
        assertEquals(2, event.score.player1Points)
        assertTrue(event.score.isTiebreak)
        assertEquals(1, event.score.setsHistory.size)
        assertEquals("ADVANCED", event.score.statsMode)
        assertEquals(2, event.stats?.player1Aces)
        assertEquals(75, event.stats?.player1FirstServePct)
        assertEquals(1, event.stats?.player2DoubleFaults)
        assertEquals(87, event.batteryLevel)
        assertTrue(event.isCharging == true)
        assertEquals(12_345L, event.timestamp)
    }

    @Test
    fun createsDoublesEventWithTeamNamesAndServingSide() {
        val state = doublesState().apply {
            isPlayer1Serving = false
            currentServer = 4
        }

        val event = MatchEventFactory.create(
            state = state,
            eventType = "match_start",
            batteryLevel = null,
            isCharging = null,
            timestamp = 1L
        )

        assertEquals("Team A", event.player1.name)
        assertEquals("Team A", event.player1.fullName)
        assertFalse(event.player1.isServing)
        assertEquals("Team B", event.player2.name)
        assertEquals("Team B", event.player2.fullName)
        assertTrue(event.player2.isServing)
    }

    private fun singlesState(): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "1",
            courtName = "Court 1"
        )
    }

    private fun doublesState(): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            player3 = playerThree,
            player4 = playerFour,
            courtId = "1",
            courtName = "Court 1",
            isDoubles = true,
            team1Name = "Team A",
            team2Name = "Team B"
        )
    }
}