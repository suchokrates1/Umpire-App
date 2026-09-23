@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package pl.vestmedia.tennisreferee.data.api.dto

import com.google.gson.annotations.SerializedName
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
    @SerializedName("kort_id")
    @SerialName("kort_id")
    val id: String,

    @SerializedName("overlay_id")
    @SerialName("overlay_id")
    val overlayId: String? = null,

    @SerializedName("name")
    @SerialName("name")
    val name: String? = null,

    @SerializedName("is_available")
    @SerialName("is_available")
    val isAvailable: Boolean = true,

    @SerializedName("current_match_id")
    @SerialName("current_match_id")
    val currentMatchId: Int? = null
)

@Serializable
data class CourtsResponseDto(
    @SerializedName("courts")
    @SerialName("courts")
    val courts: List<CourtDto> = emptyList(),

    @SerializedName("total_count")
    @SerialName("total_count")
    val totalCount: Int = 0
)

@Serializable
data class PlayerDto(
    @SerializedName("id")
    @SerialName("id")
    val id: Int,

    @SerializedName(value = "name", alternate = ["surname", "full_name"])
    @SerialName("name")
    @JsonNames("surname", "full_name")
    val name: String,

    @SerializedName("first_name")
    @SerialName("first_name")
    val firstName: String = "",

    @SerializedName("last_name")
    @SerialName("last_name")
    val lastName: String = "",

    @SerializedName(value = "flag", alternate = ["country_code"])
    @SerialName("flag")
    @JsonNames("country_code")
    val flag: String? = null,

    @SerializedName(value = "flagUrl", alternate = ["flag_url"])
    @SerialName("flagUrl")
    @JsonNames("flag_url")
    val flagUrl: String? = null,

    @SerializedName(value = "group", alternate = ["category"])
    @SerialName("group")
    @JsonNames("category")
    val group: String? = null,

    @SerializedName("gender")
    @SerialName("gender")
    val gender: String? = null,

    @SerializedName("list")
    @SerialName("list")
    val list: String? = null,

    @SerializedName("partner")
    @SerialName("partner")
    val partner: PlayerDto? = null
)

@Serializable
data class PlayersResponseDto(
    @SerializedName("players")
    @SerialName("players")
    val players: List<PlayerDto>,

    @SerializedName(value = "count", alternate = ["total_count"])
    @SerialName("count")
    @JsonNames("total_count")
    val totalCount: Int? = null,

    @SerializedName("ok")
    @SerialName("ok")
    val ok: Boolean? = null
)

@Serializable
data class AddPlayerResponseDto(
    @SerializedName("ok")
    @SerialName("ok")
    val ok: Boolean,

    @SerializedName("player")
    @SerialName("player")
    val player: PlayerDto? = null,

    @SerializedName("error")
    @SerialName("error")
    val error: String? = null
)

@Serializable
data class CourtPinRequestDto(
    @SerializedName("pin")
    @SerialName("pin")
    val pin: String
)

@Serializable
data class CourtAuthResponseDto(
    @SerializedName("ok")
    @SerialName("ok")
    val ok: Boolean,

    @SerializedName("authorized")
    @SerialName("authorized")
    val authorized: Boolean,

    @SerializedName(value = "court_id", alternate = ["kort_id"])
    @SerialName("court_id")
    @JsonNames("kort_id")
    val courtId: String? = null,

    @SerializedName("token")
    @SerialName("token")
    val token: String? = null,

    @SerializedName(value = "expires_at", alternate = ["expiresAt", "expiry"])
    @SerialName("expires_at")
    @JsonNames("expiresAt", "expiry")
    val expiresAt: String? = null,

    @SerializedName("error")
    @SerialName("error")
    val error: String? = null
)

@Serializable
data class TournamentOptionDto(
    @SerializedName("id")
    @SerialName("id")
    val id: Int,

    @SerializedName("name")
    @SerialName("name")
    val name: String,

    @SerializedName("city")
    @SerialName("city")
    val city: String? = null,

    @SerializedName("country")
    @SerialName("country")
    val country: String? = null,

    @SerializedName("location")
    @SerialName("location")
    val location: String? = null,

    @SerializedName("start_date")
    @SerialName("start_date")
    val startDate: String? = null,

    @SerializedName("end_date")
    @SerialName("end_date")
    val endDate: String? = null,

    @SerializedName("is_simulation")
    @SerialName("is_simulation")
    val isSimulation: Int? = null
)

@Serializable
data class ScheduleSuggestionResponseDto(
    @SerializedName("suggestion")
    @SerialName("suggestion")
    val suggestion: ScheduleSuggestionDto? = null
)

@Serializable
data class ScheduleSuggestionDto(
    @SerializedName("id")
    @SerialName("id")
    val id: Int,

    @SerializedName("tournament_id")
    @SerialName("tournament_id")
    val tournamentId: Int,

    @SerializedName("day_date")
    @SerialName("day_date")
    val dayDate: String? = null,

    @SerializedName("scheduled_time")
    @SerialName("scheduled_time")
    val scheduledTime: String? = null,

    @SerializedName("court_id")
    @SerialName("court_id")
    val courtId: String? = null,

    @SerializedName("court_label")
    @SerialName("court_label")
    val courtLabel: String? = null,

    @SerializedName("category_name")
    @SerialName("category_name")
    val categoryName: String? = null,

    @SerializedName("phase")
    @SerialName("phase")
    val phase: String? = null,

    @SerializedName("player1_name")
    @SerialName("player1_name")
    val player1Name: String,

    @SerializedName("player2_name")
    @SerialName("player2_name")
    val player2Name: String,

    @SerializedName("is_doubles")
    @SerialName("is_doubles")
    val isDoubles: Boolean = false,

    @SerializedName("player1")
    @SerialName("player1")
    val player1: PlayerDto? = null,

    @SerializedName("player2")
    @SerialName("player2")
    val player2: PlayerDto? = null
)

@Serializable
enum class MatchFinishReasonDto {
    @SerializedName("normal")
    @SerialName("normal")
    NORMAL,

    @SerializedName("test")
    @SerialName("test")
    TEST,

    @SerializedName("retirement")
    @SerialName("retirement")
    RETIREMENT,

    @SerializedName("walkover")
    @SerialName("walkover")
    WALKOVER
}

@Serializable
data class FinishMatchRequestDto(
    @SerializedName("finish_reason")
    @SerialName("finish_reason")
    val finishReason: MatchFinishReasonDto = MatchFinishReasonDto.NORMAL,

    @SerializedName("winner_name")
    @SerialName("winner_name")
    val winnerName: String? = null,

    @SerializedName("injured_player_name")
    @SerialName("injured_player_name")
    val injuredPlayerName: String? = null,

    @SerializedName("result_note")
    @SerialName("result_note")
    val resultNote: String? = null
)

@Serializable
data class MatchEventDto(
    @SerializedName("court_id")
    @SerialName("court_id")
    val courtId: String,

    @SerializedName("match_id")
    @SerialName("match_id")
    val matchId: Int? = null,

    @SerializedName("client_match_uuid")
    @SerialName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerializedName("event_type")
    @SerialName("event_type")
    val eventType: String,

    @SerializedName("player1")
    @SerialName("player1")
    val player1: PlayerInfoDto,

    @SerializedName("player2")
    @SerialName("player2")
    val player2: PlayerInfoDto,

    @SerializedName("score")
    @SerialName("score")
    val score: ScoreInfoDto,

    @SerializedName("stats")
    @SerialName("stats")
    val stats: LiveStatsInfoDto? = null,

    @SerializedName("battery_level")
    @SerialName("battery_level")
    val batteryLevel: Int? = null,

    @SerializedName("is_charging")
    @SerialName("is_charging")
    val isCharging: Boolean? = null,

    @SerializedName("timestamp")
    @SerialName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PlayerInfoDto(
    @SerializedName("name")
    @SerialName("name")
    val name: String,

    @SerializedName("full_name")
    @SerialName("full_name")
    val fullName: String? = null,

    @SerializedName("flag")
    @SerialName("flag")
    val flag: String? = null,

    @SerializedName("is_serving")
    @SerialName("is_serving")
    val isServing: Boolean
)

@Serializable
data class ScoreInfoDto(
    @SerializedName("player1_sets")
    @SerialName("player1_sets")
    val player1Sets: Int,

    @SerializedName("player2_sets")
    @SerialName("player2_sets")
    val player2Sets: Int,

    @SerializedName("player1_games")
    @SerialName("player1_games")
    val player1Games: Int,

    @SerializedName("player2_games")
    @SerialName("player2_games")
    val player2Games: Int,

    @SerializedName("player1_points")
    @SerialName("player1_points")
    val player1Points: Int,

    @SerializedName("player2_points")
    @SerialName("player2_points")
    val player2Points: Int,

    @SerializedName("is_tiebreak")
    @SerialName("is_tiebreak")
    val isTiebreak: Boolean,

    @SerializedName("is_super_tiebreak")
    @SerialName("is_super_tiebreak")
    val isSuperTiebreak: Boolean,

    @SerializedName("match_finished")
    @SerialName("match_finished")
    val matchFinished: Boolean,

    @SerializedName("sets_history")
    @SerialName("sets_history")
    val setsHistory: List<SetScoreDto> = emptyList(),

    @SerializedName("stats_mode")
    @SerialName("stats_mode")
    val statsMode: String? = null
)

@Serializable
data class MatchEventResponseDto(
    @SerializedName("success")
    @SerialName("success")
    val success: Boolean,

    @SerializedName("message")
    @SerialName("message")
    val message: String? = null
)

@Serializable
data class LiveStatsInfoDto(
    @SerializedName("player1_aces")
    @SerialName("player1_aces")
    val player1Aces: Int,

    @SerializedName("player1_double_faults")
    @SerialName("player1_double_faults")
    val player1DoubleFaults: Int,

    @SerializedName("player1_winners")
    @SerialName("player1_winners")
    val player1Winners: Int,

    @SerializedName("player1_unforced_errors")
    @SerialName("player1_unforced_errors")
    val player1UnforcedErrors: Int,

    @SerializedName("player1_first_serve_pct")
    @SerialName("player1_first_serve_pct")
    val player1FirstServePct: Int,

    @SerializedName("player2_aces")
    @SerialName("player2_aces")
    val player2Aces: Int,

    @SerializedName("player2_double_faults")
    @SerialName("player2_double_faults")
    val player2DoubleFaults: Int,

    @SerializedName("player2_winners")
    @SerialName("player2_winners")
    val player2Winners: Int,

    @SerializedName("player2_unforced_errors")
    @SerialName("player2_unforced_errors")
    val player2UnforcedErrors: Int,

    @SerializedName("player2_first_serve_pct")
    @SerialName("player2_first_serve_pct")
    val player2FirstServePct: Int
)

@Serializable
data class MatchStatisticsRequestDto(
    @SerializedName("match_id")
    @SerialName("match_id")
    val matchId: Int,

    @SerializedName("player1_name")
    @SerialName("player1_name")
    val player1Name: String,

    @SerializedName("player2_name")
    @SerialName("player2_name")
    val player2Name: String,

    @SerializedName("player1_stats")
    @SerialName("player1_stats")
    val player1Stats: PlayerStatsDto,

    @SerializedName("player2_stats")
    @SerialName("player2_stats")
    val player2Stats: PlayerStatsDto,

    @SerializedName("match_duration_ms")
    @SerialName("match_duration_ms")
    val matchDurationMs: Long,

    @SerializedName("winner")
    @SerialName("winner")
    val winner: String? = null,

    @SerializedName("stats_mode")
    @SerialName("stats_mode")
    val statsMode: String? = null
)

@Serializable
data class PlayerStatsDto(
    @SerializedName("aces")
    @SerialName("aces")
    val aces: Int,

    @SerializedName("double_faults")
    @SerialName("double_faults")
    val doubleFaults: Int,

    @SerializedName("winners")
    @SerialName("winners")
    val winners: Int,

    @SerializedName("forced_errors")
    @SerialName("forced_errors")
    val forcedErrors: Int,

    @SerializedName("unforced_errors")
    @SerialName("unforced_errors")
    val unforcedErrors: Int,

    @SerializedName("first_serves")
    @SerialName("first_serves")
    val firstServes: Int,

    @SerializedName("first_serves_in")
    @SerialName("first_serves_in")
    val firstServesIn: Int,

    @SerializedName("first_serve_percentage")
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