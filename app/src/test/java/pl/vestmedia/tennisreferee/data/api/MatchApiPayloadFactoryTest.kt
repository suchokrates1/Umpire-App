package pl.vestmedia.tennisreferee.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatusDto
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

class MatchApiPayloadFactoryTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "DE")
    private val playerThree = Player(id = 3, name = "Lis", firstName = "Ewa", lastName = "Lis", flag = "PL")
    private val playerFour = Player(id = 4, name = "Wojcik", firstName = "Anna", lastName = "Wojcik", flag = "DE")

    @Test
    fun createsMatchPayloadForSinglesState() {
        val state = singlesState(scheduleId = 44).apply {
            matchId = 9
            matchStartTime = 100L
            player1Sets = 1
            player2Sets = 0
            player1Games = 4
            player2Games = 3
            player1Points = 2
            player2Points = 1
            setsHistory.add(SetScore(setNumber = 1, player1Games = 4, player2Games = 2))
        }

        val payload = MatchApiPayloadFactory.toMatch(state)

        assertEquals(9, payload.id)
        assertEquals("1", payload.courtId)
        assertEquals("Jan Kowalski", payload.player1Name)
        assertEquals("Adam Nowak", payload.player2Name)
        assertEquals(MatchStatusDto.IN_PROGRESS, payload.status)
        assertEquals(state.clientMatchUuid, payload.clientMatchUuid)
        assertEquals(44, payload.scheduleId)
        assertEquals(1, payload.score.player1Sets)
        assertEquals(4, payload.score.player1Games)
        assertEquals(2, payload.score.player1Points)
        assertEquals(1, payload.score.setsHistory.size)
    }

    @Test
    fun createsMatchPayloadForDoublesTeamNames() {
        val state = doublesState()

        val payload = MatchApiPayloadFactory.toMatch(state)

        assertEquals("Team A", payload.player1Name)
        assertEquals("Team B", payload.player2Name)
        assertEquals(MatchStatusDto.NOT_STARTED, payload.status)
    }

    @Test
    fun createsStatisticsPayloadOnlyForFinishedMatchWithId() {
        val state = singlesState().apply {
            matchId = 12
            isMatchFinished = true
            player1Sets = 2
            player2Sets = 1
            matchDuration = 456_000L
            player1Stats.aces = 3
            player1Stats.firstServesIn = 6
            player1Stats.firstServesTotal = 8
        }

        val payload = MatchApiPayloadFactory.toStatisticsRequest(state)

        assertEquals(12, payload?.matchId)
        assertEquals("Jan Kowalski", payload?.winner)
        assertEquals(456_000L, payload?.matchDurationMs)
        assertEquals(3, payload?.player1Stats?.aces)
        assertEquals(75.0, payload?.player1Stats?.firstServePercentage)
        assertEquals("ADVANCED", payload?.statsMode)
    }

    @Test
    fun skipsStatisticsPayloadBeforeMatchIsFinishedOrSynced() {
        assertNull(MatchApiPayloadFactory.toStatisticsRequest(singlesState().apply { matchId = 12 }))
        assertNull(MatchApiPayloadFactory.toStatisticsRequest(singlesState().apply { isMatchFinished = true }))
    }

    @Test
    fun createsFinishRequestFromStateAndSkipsTestStatistics() {
        val state = singlesState().apply {
            matchId = 12
            isMatchFinished = true
            finishReason = MatchFinishReason.TEST
            finishWinnerName = "Jan Kowalski"
            resultNote = "Mecz testowy"
        }

        val request = MatchApiPayloadFactory.toFinishRequest(state)

        assertEquals(MatchFinishReason.TEST, request.finishReason)
        assertEquals("Jan Kowalski", request.winnerName)
        assertEquals("Mecz testowy", request.resultNote)
        assertNull(MatchApiPayloadFactory.toStatisticsRequest(state))
    }

    private fun singlesState(scheduleId: Int? = null): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "1",
            courtName = "Court 1",
            scheduleId = scheduleId
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