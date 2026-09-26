package pl.vestmedia.tennisreferee.data.api

import com.google.gson.Gson
import kotlinx.serialization.KSerializer
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.dto.DirectorDeviceSnapshotDto
import pl.vestmedia.tennisreferee.data.api.dto.FinishMatchRequestDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatisticsRequestDto
import pl.vestmedia.tennisreferee.data.api.dto.apiJson
import pl.vestmedia.tennisreferee.data.api.dto.toDto
import pl.vestmedia.tennisreferee.data.model.MatchEventFactory
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.SetScore
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode

/**
 * The JSON wyniki-live actually receives, pinned per request type.
 *
 * Payloads are built through the same factories the app uses, so the mapping and the
 * serializer are both covered: a renamed property, a different JSON library or a changed
 * default shows up here instead of on a court during a tournament.
 */
class WireFormatTest {

    private val gson = Gson()
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "DE")

    private fun state(): MatchState = MatchState(
        player1 = playerOne,
        player2 = playerTwo,
        courtId = "1",
        courtName = "Court 1",
        scheduleId = 44,
        clientMatchUuid = "uuid-1",
    ).apply {
        matchId = 9
        matchStartTime = 100L
        statsMode = StatsMode.ADVANCED
        player1Sets = 1
        player2Sets = 0
        player1Games = 4
        player2Games = 3
        player1Points = 2
        player2Points = 1
        isPlayer1Serving = true
        setsHistory.add(SetScore(setNumber = 1, player1Games = 4, player2Games = 2, tiebreakLoserPoints = 5))
        player1Stats.aces = 3
        player1Stats.doubleFaults = 1
        player1Stats.winners = 5
        player2Stats.aces = 1
        player2Stats.doubleFaults = 2
    }

    @Test
    fun createAndUpdateMatchBody() {
        assertWire(
            MatchDto.serializer(),
            MatchApiPayloadFactory.toMatch(state()),
            """{"id":9,"court_id":"1","player1_name":"Jan Kowalski","player2_name":"Adam Nowak","score":{"player1_sets":1,"player2_sets":0,"player1_games":4,"player2_games":3,"player1_points":2,"player2_points":1,"sets_history":[{"set_number":1,"player1_games":4,"player2_games":2,"tiebreak_loser_points":5,"is_super_tiebreak":false}]},"status":"in_progress","schedule_id":44,"client_match_uuid":"uuid-1","finish_reason":"normal","match_config":{"games_per_set":4,"sets_to_win":2,"tiebreak_points":7,"super_tiebreak_points":10,"tiebreak_at_games":4,"no_advantage":false,"tiebreak_only":false,"stats_mode":"ADVANCED"},"match_start_time_ms":100,"serve":"A"}""",
        )
    }

    @Test
    fun matchConfigCarriesAnEarlyTiebreak() {
        val early = state().let { it.copy(matchConfig = it.matchConfig.copy(tiebreakAtGames = 3)) }
        val body = gson.toJson(MatchApiPayloadFactory.toMatch(early))
        assertEquals(true, body.contains(""""tiebreak_at_games":3"""))
    }

    @Test
    fun finishMatchBody() {
        val finished = state().apply {
            finishReason = MatchFinishReason.RETIREMENT
            finishWinnerName = "Jan Kowalski"
            injuredPlayerName = "Adam Nowak"
            resultNote = "Krecz: Adam Nowak"
            isMatchFinished = true
        }
        assertWire(
            FinishMatchRequestDto.serializer(),
            MatchApiPayloadFactory.toFinishRequest(finished).toDto(),
            """{"finish_reason":"retirement","winner_name":"Jan Kowalski","injured_player_name":"Adam Nowak","result_note":"Krecz: Adam Nowak"}""",
        )
    }

    @Test
    fun matchEventBody() {
        val event = MatchEventFactory.create(state(), "point", batteryLevel = 64, isCharging = true, timestamp = 1790000000000L)
        assertWire(
            MatchEventDto.serializer(),
            event,
            """{"court_id":"1","match_id":9,"client_match_uuid":"uuid-1","event_type":"point","player1":{"name":"Kowalski","full_name":"Jan Kowalski","flag":"PL","is_serving":true},"player2":{"name":"Nowak","full_name":"Adam Nowak","flag":"DE","is_serving":false},"score":{"player1_sets":1,"player2_sets":0,"player1_games":4,"player2_games":3,"player1_points":2,"player2_points":1,"is_tiebreak":false,"is_super_tiebreak":false,"match_finished":false,"sets_history":[{"set_number":1,"player1_games":4,"player2_games":2,"tiebreak_loser_points":5,"is_super_tiebreak":false}],"stats_mode":"ADVANCED"},"stats":{"player1_aces":3,"player1_double_faults":1,"player1_winners":5,"player1_unforced_errors":0,"player1_first_serve_pct":0,"player2_aces":1,"player2_double_faults":2,"player2_winners":0,"player2_unforced_errors":0,"player2_first_serve_pct":0},"battery_level":64,"is_charging":true,"timestamp":1790000000000}""",
        )
    }

    @Test
    fun statisticsBody() {
        val finished = state().apply { isMatchFinished = true }
        assertWire(
            MatchStatisticsRequestDto.serializer(),
            MatchApiPayloadFactory.toStatisticsRequest(finished)!!,
            """{"match_id":9,"player1_name":"Jan Kowalski","player2_name":"Adam Nowak","player1_stats":{"aces":3,"double_faults":1,"winners":5,"forced_errors":0,"unforced_errors":0,"first_serves":0,"first_serves_in":0,"first_serve_percentage":0.0},"player2_stats":{"aces":1,"double_faults":2,"winners":0,"forced_errors":0,"unforced_errors":0,"first_serves":0,"first_serves_in":0,"first_serve_percentage":0.0},"match_duration_ms":0,"winner":"Jan Kowalski","stats_mode":"ADVANCED"}""",
        )
    }

    @Test
    fun directorSnapshotBody() {
        assertWire(
            DirectorDeviceSnapshotDto.serializer(),
            MatchApiPayloadFactory.toDirectorSnapshot(state()),
            """{"court_id":"1","court_name":"Court 1","player1_name":"Jan Kowalski","player2_name":"Adam Nowak","is_doubles":false,"player1_sets":1,"player2_sets":0,"player1_games":4,"player2_games":3,"player1_points":2,"player2_points":1,"sets_history":[{"set_number":1,"player1_games":4,"player2_games":2,"tiebreak_loser_points":5,"is_super_tiebreak":false}],"is_player1_serving":true,"is_tiebreak":false,"is_super_tiebreak":false,"match_start_time_ms":100,"match_duration_ms":0,"games_per_set":4,"sets_to_win":2,"tiebreak_at_games":4,"no_advantage":false,"tiebreak_only":false,"stats_mode":"ADVANCED"}""",
        )
    }

    private fun <T> assertWire(serializer: KSerializer<T>, value: T, golden: String) {
        assertEquals(golden, gson.toJson(value))
        assertEquals(golden, apiJson.encodeToString(serializer, value))
        assertEquals(golden, apiJson.encodeToString(serializer, apiJson.decodeFromString(serializer, golden)))
    }
}
