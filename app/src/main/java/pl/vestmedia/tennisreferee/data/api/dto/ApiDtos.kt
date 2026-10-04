@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package pl.vestmedia.tennisreferee.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import pl.vestmedia.tennisreferee.data.model.AddPlayerResponse
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.model.CourtAuthResponse
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.ScheduleSuggestion
import pl.vestmedia.tennisreferee.data.model.TournamentOption

@Serializable
data class CourtDto(
    @SerialName("kort_id")
    val id: String,

    @SerialName("overlay_id")
    val overlayId: String? = null,

    @SerialName("name")
    val name: String? = null,

    @SerialName("is_available")
    val isAvailable: Boolean = true,

    @SerialName("current_match_id")
    val currentMatchId: Int? = null
)

@Serializable
data class CourtsResponseDto(
    @SerialName("courts")
    val courts: List<CourtDto> = emptyList(),

    @SerialName("total_count")
    val totalCount: Int = 0
)

@Serializable
data class PlayerDto(
    @SerialName("id")
    val id: Int,

    @SerialName("name")
    @JsonNames("surname", "full_name")
    val name: String,

    @SerialName("first_name")
    val firstName: String = "",

    @SerialName("last_name")
    val lastName: String = "",

    @SerialName("flag")
    @JsonNames("country_code")
    val flag: String? = null,

    @SerialName("flagUrl")
    @JsonNames("flag_url")
    val flagUrl: String? = null,

    @SerialName("group")
    @JsonNames("category")
    val group: String? = null,

    @SerialName("gender")
    val gender: String? = null,

    @SerialName("list")
    val list: String? = null,

    @SerialName("partner")
    val partner: PlayerDto? = null
)

@Serializable
data class PlayersResponseDto(
    @SerialName("players")
    val players: List<PlayerDto>,

    @SerialName("count")
    @JsonNames("total_count")
    val totalCount: Int? = null,

    @SerialName("ok")
    val ok: Boolean? = null
)

@Serializable
data class AddPlayerResponseDto(
    @SerialName("ok")
    val ok: Boolean,

    @SerialName("player")
    val player: PlayerDto? = null,

    @SerialName("error")
    val error: String? = null
)

@Serializable
data class CourtPinRequestDto(
    @SerialName("pin")
    val pin: String
)

@Serializable
data class CourtAuthResponseDto(
    @SerialName("ok")
    val ok: Boolean,

    @SerialName("authorized")
    val authorized: Boolean,

    @SerialName("court_id")
    @JsonNames("kort_id")
    val courtId: String? = null,

    @SerialName("token")
    val token: String? = null,

    @SerialName("expires_at")
    @JsonNames("expiresAt", "expiry")
    val expiresAt: String? = null,

    @SerialName("error")
    val error: String? = null
)

@Serializable
data class TournamentOptionDto(
    @SerialName("id")
    val id: Int,

    @SerialName("name")
    val name: String,

    @SerialName("city")
    val city: String? = null,

    @SerialName("country")
    val country: String? = null,

    @SerialName("location")
    val location: String? = null,

    @SerialName("start_date")
    val startDate: String? = null,

    @SerialName("end_date")
    val endDate: String? = null,

    @SerialName("is_simulation")
    val isSimulation: Int? = null
)

@Serializable
data class ScheduleSuggestionResponseDto(
    @SerialName("suggestion")
    val suggestion: ScheduleSuggestionDto? = null
)

@Serializable
data class ScheduleSuggestionDto(
    @SerialName("id")
    val id: Int,

    @SerialName("tournament_id")
    val tournamentId: Int,

    @SerialName("day_date")
    val dayDate: String? = null,

    @SerialName("scheduled_time")
    val scheduledTime: String? = null,

    @SerialName("court_id")
    val courtId: String? = null,

    @SerialName("court_label")
    val courtLabel: String? = null,

    @SerialName("category_name")
    val categoryName: String? = null,

    @SerialName("phase")
    val phase: String? = null,

    @SerialName("player1_name")
    val player1Name: String,

    @SerialName("player2_name")
    val player2Name: String,

    @SerialName("is_doubles")
    val isDoubles: Boolean = false,

    @SerialName("player1")
    val player1: PlayerDto? = null,

    @SerialName("player2")
    val player2: PlayerDto? = null
)

@Serializable
enum class MatchFinishReasonDto {
    @SerialName("normal")
    NORMAL,

    @SerialName("test")
    TEST,

    @SerialName("retirement")
    RETIREMENT,

    @SerialName("walkover")
    WALKOVER
}

@Serializable
data class FinishMatchRequestDto(
    @SerialName("finish_reason")
    val finishReason: MatchFinishReasonDto = MatchFinishReasonDto.NORMAL,

    @SerialName("winner_name")
    val winnerName: String? = null,

    @SerialName("injured_player_name")
    val injuredPlayerName: String? = null,

    @SerialName("result_note")
    val resultNote: String? = null
)

@Serializable
data class MatchEventDto(
    @SerialName("court_id")
    val courtId: String,

    @SerialName("match_id")
    val matchId: Int? = null,

    @SerialName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerialName("event_type")
    val eventType: String,

    @SerialName("player1")
    val player1: PlayerInfoDto,

    @SerialName("player2")
    val player2: PlayerInfoDto,

    @SerialName("score")
    val score: ScoreInfoDto,

    @SerialName("stats")
    val stats: LiveStatsInfoDto? = null,

    @SerialName("battery_level")
    val batteryLevel: Int? = null,

    @SerialName("is_charging")
    val isCharging: Boolean? = null,

    @SerialName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PlayerInfoDto(
    @SerialName("name")
    val name: String,

    @SerialName("full_name")
    val fullName: String? = null,

    @SerialName("flag")
    val flag: String? = null,

    @SerialName("is_serving")
    val isServing: Boolean
)

@Serializable
data class ScoreInfoDto(
    @SerialName("player1_sets")
    val player1Sets: Int,

    @SerialName("player2_sets")
    val player2Sets: Int,

    @SerialName("player1_games")
    val player1Games: Int,

    @SerialName("player2_games")
    val player2Games: Int,

    @SerialName("player1_points")
    val player1Points: Int,

    @SerialName("player2_points")
    val player2Points: Int,

    @SerialName("is_tiebreak")
    val isTiebreak: Boolean,

    @SerialName("is_super_tiebreak")
    val isSuperTiebreak: Boolean,

    @SerialName("match_finished")
    val matchFinished: Boolean,

    @SerialName("sets_history")
    val setsHistory: List<SetScoreDto> = emptyList(),

    @SerialName("stats_mode")
    val statsMode: String? = null
)

@Serializable
data class MatchEventResponseDto(
    @SerialName("success")
    val success: Boolean,

    @SerialName("message")
    val message: String? = null
)

@Serializable
data class LiveStatsInfoDto(
    @SerialName("player1_aces")
    val player1Aces: Int,

    @SerialName("player1_double_faults")
    val player1DoubleFaults: Int,

    @SerialName("player1_winners")
    val player1Winners: Int,

    @SerialName("player1_unforced_errors")
    val player1UnforcedErrors: Int,

    @SerialName("player1_first_serve_pct")
    val player1FirstServePct: Int,

    @SerialName("player2_aces")
    val player2Aces: Int,

    @SerialName("player2_double_faults")
    val player2DoubleFaults: Int,

    @SerialName("player2_winners")
    val player2Winners: Int,

    @SerialName("player2_unforced_errors")
    val player2UnforcedErrors: Int,

    @SerialName("player2_first_serve_pct")
    val player2FirstServePct: Int
)

@Serializable
data class MatchStatisticsRequestDto(
    @SerialName("match_id")
    val matchId: Int,

    @SerialName("player1_name")
    val player1Name: String,

    @SerialName("player2_name")
    val player2Name: String,

    @SerialName("player1_stats")
    val player1Stats: PlayerStatsDto,

    @SerialName("player2_stats")
    val player2Stats: PlayerStatsDto,

    @SerialName("match_duration_ms")
    val matchDurationMs: Long,

    @SerialName("winner")
    val winner: String? = null,

    @SerialName("stats_mode")
    val statsMode: String? = null
)

@Serializable
data class PlayerStatsDto(
    @SerialName("aces")
    val aces: Int,

    @SerialName("double_faults")
    val doubleFaults: Int,

    @SerialName("winners")
    val winners: Int,

    @SerialName("forced_errors")
    val forcedErrors: Int,

    @SerialName("unforced_errors")
    val unforcedErrors: Int,

    @SerialName("first_serves")
    val firstServes: Int,

    @SerialName("first_serves_in")
    val firstServesIn: Int,

    @SerialName("first_serve_percentage")
    val firstServePercentage: Double
)

fun CourtDto.toModel(): Court {
    return Court(
        id = id,
        overlayId = overlayId,
        name = name,
        isAvailable = isAvailable,
        currentMatchId = currentMatchId
    )
}

fun PlayerDto.toModel(): Player {
    return Player(
        id = id,
        name = name,
        firstName = firstName,
        lastName = lastName,
        flag = flag,
        flagUrl = flagUrl,
        group = group,
        gender = gender,
        list = list,
        partner = partner?.copy(partner = null)?.toModel()
    )
}

fun Player.toDto(): PlayerDto {
    return PlayerDto(
        id = id,
        name = name,
        firstName = firstName,
        lastName = lastName,
        flag = flag,
        flagUrl = flagUrl,
        group = group,
        gender = gender,
        list = list,
        partner = partner?.copy(partner = null)?.toDto()
    )
}

fun AddPlayerResponseDto.toModel(): AddPlayerResponse {
    return AddPlayerResponse(
        ok = ok,
        player = player?.toModel(),
        error = error
    )
}

fun CourtAuthResponseDto.toModel(): CourtAuthResponse {
    return CourtAuthResponse(
        ok = ok,
        authorized = authorized,
        courtId = courtId,
        token = token,
        expiresAt = expiresAt,
        error = error
    )
}

fun TournamentOptionDto.toModel(): TournamentOption {
    return TournamentOption(
        id = id,
        name = name,
        city = city,
        country = country,
        location = location,
        startDate = startDate,
        endDate = endDate,
        isSimulation = isSimulation == 1
    )
}

fun ScheduleSuggestionDto.toModel(): ScheduleSuggestion {
    return ScheduleSuggestion(
        id = id,
        tournamentId = tournamentId,
        dayDate = dayDate,
        scheduledTime = scheduledTime,
        courtId = courtId,
        courtLabel = courtLabel,
        categoryName = categoryName,
        phase = phase,
        player1Name = player1Name,
        player2Name = player2Name,
        isDoubles = isDoubles,
        player1 = player1?.toModel(),
        player2 = player2?.toModel()
    )
}

fun MatchFinishReasonDto.toModel(): MatchFinishReason {
    return when (this) {
        MatchFinishReasonDto.NORMAL -> MatchFinishReason.NORMAL
        MatchFinishReasonDto.TEST -> MatchFinishReason.TEST
        MatchFinishReasonDto.RETIREMENT -> MatchFinishReason.RETIREMENT
        MatchFinishReasonDto.WALKOVER -> MatchFinishReason.WALKOVER
    }
}

fun MatchFinishReason.toDto(): MatchFinishReasonDto {
    return when (this) {
        MatchFinishReason.NORMAL -> MatchFinishReasonDto.NORMAL
        MatchFinishReason.TEST -> MatchFinishReasonDto.TEST
        MatchFinishReason.RETIREMENT -> MatchFinishReasonDto.RETIREMENT
        MatchFinishReason.WALKOVER -> MatchFinishReasonDto.WALKOVER
    }
}

fun FinishMatchRequest.toDto(): FinishMatchRequestDto {
    return FinishMatchRequestDto(
        finishReason = finishReason.toDto(),
        winnerName = winnerName,
        injuredPlayerName = injuredPlayerName,
        resultNote = resultNote
    )
}