package pl.vestmedia.tennisreferee.data.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.dto.CourtAuthResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchFinishReasonDto
import pl.vestmedia.tennisreferee.data.api.dto.PlayerDto
import pl.vestmedia.tennisreferee.data.api.dto.ScheduleSuggestionDto
import pl.vestmedia.tennisreferee.data.api.dto.toDto
import pl.vestmedia.tennisreferee.data.api.dto.toModel
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.Match
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.domain.match.model.MatchStatus
import pl.vestmedia.tennisreferee.domain.match.model.Score
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

class ApiDtoMappingTest {
    @Test
    fun matchDtoRoundTripPreservesDomainFields() {
        val match = Match(
            id = 42,
            courtId = "T1-1",
            player1Name = "Anna Kowalska",
            player2Name = "Ewa Nowak",
            score = Score(
                player1Sets = 1,
                player2Sets = 0,
                player1Games = 4,
                player2Games = 3,
                player1Points = 2,
                player2Points = 1,
                setsHistory = listOf(SetScore(1, 4, 3, tiebreakLoserPoints = 5))
            ),
            status = MatchStatus.FINISHED,
            createdAt = "2026-05-29T10:00:00Z",
            updatedAt = "2026-05-29T11:00:00Z",
            bracketWarning = "knockout",
            phase = "semifinal",
            scheduleId = 7,
            clientMatchUuid = "uuid-1",
            finishReason = MatchFinishReason.RETIREMENT,
            winnerName = "Anna Kowalska",
            injuredPlayerName = "Ewa Nowak",
            resultNote = "Retirement"
        )

        val dto = match.toDto()
        val roundTrip = dto.toModel()

        assertEquals(MatchFinishReasonDto.RETIREMENT, dto.finishReason)
        assertEquals(match, roundTrip)
    }

    @Test
    fun scheduleSuggestionDtoMapsNestedPlayers() {
        val dto = ScheduleSuggestionDto(
            id = 9,
            tournamentId = 3,
            dayDate = "2026-05-29",
            scheduledTime = "14:30",
            courtId = "1",
            courtLabel = "Kort 1",
            categoryName = "Kobiety B1",
            phase = "Final",
            player1Name = "Anna Kowalska",
            player2Name = "Ewa Nowak",
            player1 = Player(id = 1, name = "Kowalska", firstName = "Anna", lastName = "Kowalska", flag = "PL").toDto(),
            player2 = null
        )

        val suggestion = dto.toModel()

        assertEquals("Anna Kowalska", suggestion.player1Name)
        assertEquals("Kowalska", suggestion.player1?.lastName)
        assertNull(suggestion.player2)
    }

    @Test
    fun playerDtoMapsFirstAndLastNameForSelectionFullName() {
        val player = PlayerDto(
            id = 10,
            name = "Anna Kowalska",
            firstName = "Anna",
            lastName = "Kowalska",
            flag = "PL",
            gender = "F"
        ).toModel()

        assertEquals("Anna", player.firstName)
        assertEquals("Kowalska", player.lastName)
        assertEquals("Anna Kowalska", player.getFullName())
    }

    @Test
    fun finishRequestDtoUsesApiEnumWithoutLeakingSerializedNameToDomain() {
        val request = FinishMatchRequest(
            finishReason = MatchFinishReason.WALKOVER,
            winnerName = "Anna Kowalska",
            resultNote = "WO"
        )

        val dto = request.toDto()

        assertEquals(MatchFinishReasonDto.WALKOVER, dto.finishReason)
        assertEquals("Anna Kowalska", dto.winnerName)
        assertEquals("WO", dto.resultNote)
    }

    @Test
    fun courtAuthResponseDtoMapsToRepositoryModel() {
        val model = CourtAuthResponseDto(
            ok = true,
            authorized = true,
            courtId = "1",
            token = "court-token",
            expiresAt = "2026-07-11T16:30:00Z",
            error = null
        ).toModel()

        assertEquals(true, model.ok)
        assertEquals(true, model.authorized)
        assertEquals("1", model.courtId)
        assertEquals("court-token", model.token)
        assertEquals("2026-07-11T16:30:00Z", model.expiresAt)
    }

    @Test
    fun courtAuthResponseAcceptsCourtIdAndLegacyKortId() {
        val gson = Gson()

        val current = gson.fromJson(
            """{"ok":true,"authorized":true,"court_id":"court-1"}""",
            CourtAuthResponseDto::class.java
        )
        val legacy = gson.fromJson(
            """{"ok":true,"authorized":true,"kort_id":"court-2"}""",
            CourtAuthResponseDto::class.java
        )

        assertEquals("court-1", current.courtId)
        assertEquals("court-2", legacy.courtId)
    }
}