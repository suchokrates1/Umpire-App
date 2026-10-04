package pl.vestmedia.tennisreferee.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DirectorCommandsResponseDto(
    @SerialName("commands")
    val commands: List<DirectorCommandDto> = emptyList()
)

@Serializable
data class HeartbeatResponseDto(
    @SerialName("status")
    val status: String? = null,

    @SerialName("commands")
    val commands: List<DirectorCommandDto> = emptyList()
)

@Serializable
data class HeartbeatRequestDto(
    @SerialName("court_id")
    val courtId: String = "",

    @SerialName("screen")
    val screen: String = "",

    @SerialName("app_version")
    val appVersion: String = "",

    @SerialName("timestamp")
    val timestamp: String = "",

    @SerialName("match_id")
    val matchId: String? = null,

    @SerialName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerialName("battery_level")
    val batteryLevel: String? = null,

    @SerialName("is_charging")
    val isCharging: String? = null,

    @SerialName("snapshot")
    val snapshot: DirectorDeviceSnapshotDto? = null
)

@Serializable
data class DirectorDeviceSnapshotDto(
    @SerialName("court_id")
    val courtId: String? = null,

    @SerialName("court_name")
    val courtName: String? = null,

    @SerialName("player1_name")
    val player1Name: String? = null,

    @SerialName("player2_name")
    val player2Name: String? = null,

    @SerialName("is_doubles")
    val isDoubles: Boolean? = null,

    @SerialName("player1_sets")
    val player1Sets: Int? = null,

    @SerialName("player2_sets")
    val player2Sets: Int? = null,

    @SerialName("player1_games")
    val player1Games: Int? = null,

    @SerialName("player2_games")
    val player2Games: Int? = null,

    @SerialName("player1_points")
    val player1Points: Int? = null,

    @SerialName("player2_points")
    val player2Points: Int? = null,

    @SerialName("sets_history")
    val setsHistory: List<SetScoreDto>? = null,

    @SerialName("is_player1_serving")
    val isPlayer1Serving: Boolean? = null,

    @SerialName("is_tiebreak")
    val isTiebreak: Boolean? = null,

    @SerialName("is_super_tiebreak")
    val isSuperTiebreak: Boolean? = null,

    @SerialName("match_start_time_ms")
    val matchStartTimeMs: Long? = null,

    @SerialName("match_duration_ms")
    val matchDurationMs: Long? = null,

    @SerialName("games_per_set")
    val gamesPerSet: Int? = null,

    @SerialName("sets_to_win")
    val setsToWin: Int? = null,

    @SerialName("tiebreak_at_games")
    val tiebreakAtGames: Int? = null,

    @SerialName("no_advantage")
    val noAdvantage: Boolean? = null,

    @SerialName("tiebreak_only")
    val tiebreakOnly: Boolean? = null,

    @SerialName("stats_mode")
    val statsMode: String? = null
)

@Serializable
data class DirectorAckResponseDto(
    @SerialName("ok")
    val ok: Boolean = false,

    @SerialName("acked")
    val acked: Boolean = false
)

@Serializable
data class DirectorCommandDto(
    @SerialName("id")
    val id: String? = null,

    @SerialName("seq")
    val seq: Int? = null,

    @SerialName("type")
    val type: String? = null,

    @SerialName("match_id")
    val matchId: Int? = null,

    @SerialName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerialName("court_id")
    val courtId: String? = null,

    @SerialName("court_name")
    val courtName: String? = null,

    @SerialName("court_token")
    val courtToken: String? = null,

    @SerialName("court_token_expires_at")
    val courtTokenExpiresAt: String? = null,

    @SerialName("player1_name")
    val player1Name: String? = null,

    @SerialName("player2_name")
    val player2Name: String? = null,

    @SerialName("score")
    val score: DirectorScoreDto? = null,

    @SerialName("match_config")
    val matchConfig: MatchConfigDto? = null
)

@Serializable
data class DirectorScoreDto(
    @SerialName("player1_sets")
    val player1Sets: Int? = null,

    @SerialName("player2_sets")
    val player2Sets: Int? = null,

    @SerialName("player1_games")
    val player1Games: Int? = null,

    @SerialName("player2_games")
    val player2Games: Int? = null,

    @SerialName("player1_points")
    val player1Points: Int? = null,

    @SerialName("player2_points")
    val player2Points: Int? = null,

    @SerialName("sets_history")
    val setsHistory: List<SetScoreDto>? = null,

    @SerialName("is_tiebreak")
    val isTiebreak: Boolean? = null,

    @SerialName("is_super_tiebreak")
    val isSuperTiebreak: Boolean? = null,

    @SerialName("is_player1_serving")
    val isPlayer1Serving: Boolean? = null
)

@Serializable
data class MatchConfigDto(
    @SerialName("games_per_set")
    val gamesPerSet: Int? = null,

    @SerialName("sets_to_win")
    val setsToWin: Int? = null,

    @SerialName("tiebreak_points")
    val tiebreakPoints: Int? = null,

    @SerialName("super_tiebreak_points")
    val superTiebreakPoints: Int? = null,

    @SerialName("tiebreak_at_games")
    val tiebreakAtGames: Int? = null,

    @SerialName("no_advantage")
    val noAdvantage: Boolean? = null,

    @SerialName("tiebreak_only")
    val tiebreakOnly: Boolean? = null,

    @SerialName("stats_mode")
    val statsMode: String? = null
)
