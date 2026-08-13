package pl.vestmedia.tennisreferee.data.api.dto

import com.google.gson.annotations.SerializedName
import pl.vestmedia.tennisreferee.data.model.AddPlayerResponse
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.model.CourtAuthResponse
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.ScheduleSuggestion
import pl.vestmedia.tennisreferee.data.model.TournamentOption

data class CourtDto(
    @SerializedName("kort_id")
    val id: String,

    @SerializedName("overlay_id")
    val overlayId: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("is_available")
    val isAvailable: Boolean = true,

    @SerializedName("current_match_id")
    val currentMatchId: Int? = null
)

data class CourtsResponseDto(
    @SerializedName("courts")
    val courts: List<CourtDto> = emptyList(),

    @SerializedName("total_count")
    val totalCount: Int = 0
)

data class PlayerDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName(value = "name", alternate = ["surname", "full_name"])
    val name: String,

    @SerializedName("first_name")
    val firstName: String = "",

    @SerializedName("last_name")
    val lastName: String = "",

    @SerializedName(value = "flag", alternate = ["country_code"])
    val flag: String? = null,

    @SerializedName(value = "flagUrl", alternate = ["flag_url"])
    val flagUrl: String? = null,

    @SerializedName(value = "group", alternate = ["category"])
    val group: String? = null,

    @SerializedName("gender")
    val gender: String? = null,

    @SerializedName("list")
    val list: String? = null,

    @SerializedName("partner")
    val partner: PlayerDto? = null
)

data class PlayersResponseDto(
    @SerializedName("players")
    val players: List<PlayerDto>,

    @SerializedName(value = "count", alternate = ["total_count"])
    val totalCount: Int? = null,

    @SerializedName("ok")
    val ok: Boolean? = null
)

data class AddPlayerResponseDto(
    @SerializedName("ok")
    val ok: Boolean,

    @SerializedName("player")
    val player: PlayerDto?,

    @SerializedName("error")
    val error: String? = null
)

data class CourtPinRequestDto(
    @SerializedName("pin")
    val pin: String
)

data class CourtAuthResponseDto(
    @SerializedName("ok")
    val ok: Boolean,

    @SerializedName("authorized")
    val authorized: Boolean,

    @SerializedName(value = "court_id", alternate = ["kort_id"])
    val courtId: String? = null,

    @SerializedName("token")
    val token: String? = null,

    @SerializedName(value = "expires_at", alternate = ["expiresAt", "expiry"])
    val expiresAt: String? = null,

    @SerializedName("error")
    val error: String? = null
)

data class TournamentOptionDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("city")
    val city: String? = null,

    @SerializedName("country")
    val country: String? = null,

    @SerializedName("location")
    val location: String? = null,

    @SerializedName("start_date")
    val startDate: String? = null,

    @SerializedName("end_date")
    val endDate: String? = null
)

data class ScheduleSuggestionResponseDto(
    @SerializedName("suggestion")
    val suggestion: ScheduleSuggestionDto? = null
)

data class ScheduleSuggestionDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("tournament_id")
    val tournamentId: Int,

    @SerializedName("day_date")
    val dayDate: String? = null,

    @SerializedName("scheduled_time")
    val scheduledTime: String? = null,

    @SerializedName("court_id")
    val courtId: String? = null,

    @SerializedName("court_label")
    val courtLabel: String? = null,

    @SerializedName("category_name")
    val categoryName: String? = null,

    @SerializedName("phase")
    val phase: String? = null,

    @SerializedName("player1_name")
    val player1Name: String,

    @SerializedName("player2_name")
    val player2Name: String,

    @SerializedName("is_doubles")
    val isDoubles: Boolean = false,

    @SerializedName("player1")
    val player1: PlayerDto? = null,

    @SerializedName("player2")
    val player2: PlayerDto? = null
)

enum class MatchFinishReasonDto {
    @SerializedName("normal")
    NORMAL,

    @SerializedName("test")
    TEST,

    @SerializedName("retirement")
    RETIREMENT,

    @SerializedName("walkover")
    WALKOVER
}

data class FinishMatchRequestDto(
    @SerializedName("finish_reason")
    val finishReason: MatchFinishReasonDto = MatchFinishReasonDto.NORMAL,

    @SerializedName("winner_name")
    val winnerName: String? = null,

    @SerializedName("injured_player_name")
    val injuredPlayerName: String? = null,

    @SerializedName("result_note")
    val resultNote: String? = null
)

data class MatchEventDto(
    @SerializedName("court_id")
    val courtId: String,

    @SerializedName("match_id")
    val matchId: Int? = null,

    @SerializedName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerializedName("event_type")
    val eventType: String,

    @SerializedName("player1")
    val player1: PlayerInfoDto,

    @SerializedName("player2")
    val player2: PlayerInfoDto,

    @SerializedName("score")
    val score: ScoreInfoDto,

    @SerializedName("stats")
    val stats: LiveStatsInfoDto? = null,

    @SerializedName("battery_level")
    val batteryLevel: Int? = null,

    @SerializedName("is_charging")
    val isCharging: Boolean? = null,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

data class PlayerInfoDto(
    @SerializedName("name")
    val name: String,

    @SerializedName("full_name")
    val fullName: String? = null,

    @SerializedName("flag")
    val flag: String?,

    @SerializedName("is_serving")
    val isServing: Boolean
)

data class ScoreInfoDto(
    @SerializedName("player1_sets")
    val player1Sets: Int,

    @SerializedName("player2_sets")
    val player2Sets: Int,

    @SerializedName("player1_games")
    val player1Games: Int,

    @SerializedName("player2_games")
    val player2Games: Int,

    @SerializedName("player1_points")
    val player1Points: Int,

    @SerializedName("player2_points")
    val player2Points: Int,

    @SerializedName("is_tiebreak")
    val isTiebreak: Boolean,

    @SerializedName("is_super_tiebreak")
    val isSuperTiebreak: Boolean,

    @SerializedName("match_finished")
    val matchFinished: Boolean,

    @SerializedName("sets_history")
    val setsHistory: List<SetScoreDto> = emptyList(),

    @SerializedName("stats_mode")
    val statsMode: String? = null
)

data class MatchEventResponseDto(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?
)

data class LiveStatsInfoDto(
    @SerializedName("player1_aces")
    val player1Aces: Int,

    @SerializedName("player1_double_faults")
    val player1DoubleFaults: Int,

    @SerializedName("player1_winners")
    val player1Winners: Int,

    @SerializedName("player1_unforced_errors")
    val player1UnforcedErrors: Int,

    @SerializedName("player1_first_serve_pct")
    val player1FirstServePct: Int,

    @SerializedName("player2_aces")
    val player2Aces: Int,

    @SerializedName("player2_double_faults")
    val player2DoubleFaults: Int,

    @SerializedName("player2_winners")
    val player2Winners: Int,

    @SerializedName("player2_unforced_errors")
    val player2UnforcedErrors: Int,

    @SerializedName("player2_first_serve_pct")
    val player2FirstServePct: Int
)

data class MatchStatisticsRequestDto(
    @SerializedName("match_id")
    val matchId: Int,

    @SerializedName("player1_name")
    val player1Name: String,

    @SerializedName("player2_name")
    val player2Name: String,

    @SerializedName("player1_stats")
    val player1Stats: PlayerStatsDto,

    @SerializedName("player2_stats")
    val player2Stats: PlayerStatsDto,

    @SerializedName("match_duration_ms")
    val matchDurationMs: Long,

    @SerializedName("winner")
    val winner: String?,

    @SerializedName("stats_mode")
    val statsMode: String? = null
)

data class PlayerStatsDto(
    @SerializedName("aces")
    val aces: Int,

    @SerializedName("double_faults")
    val doubleFaults: Int,

    @SerializedName("winners")
    val winners: Int,

    @SerializedName("forced_errors")
    val forcedErrors: Int,

    @SerializedName("unforced_errors")
    val unforcedErrors: Int,

    @SerializedName("first_serves")
    val firstServes: Int,

    @SerializedName("first_serves_in")
    val firstServesIn: Int,

    @SerializedName("first_serve_percentage")
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

fun Court.toDto(): CourtDto {
    return CourtDto(
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
        endDate = endDate
    )
}

fun TournamentOption.toDto(): TournamentOptionDto {
    return TournamentOptionDto(
        id = id,
        name = name,
        city = city,
        country = country,
        location = location,
        startDate = startDate,
        endDate = endDate
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

fun ScheduleSuggestion.toDto(): ScheduleSuggestionDto {
    return ScheduleSuggestionDto(
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
        player1 = player1?.toDto(),
        player2 = player2?.toDto()
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