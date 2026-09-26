package pl.vestmedia.tennisreferee.data.api.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DirectorCommandsResponseDto(
    @SerializedName("commands")
    @SerialName("commands")
    val commands: List<DirectorCommandDto> = emptyList()
)

@Serializable
data class HeartbeatResponseDto(
    @SerializedName("status")
    @SerialName("status")
    val status: String? = null,

    @SerializedName("commands")
    @SerialName("commands")
    val commands: List<DirectorCommandDto> = emptyList()
)

@Serializable
data class HeartbeatRequestDto(
    @SerializedName("court_id")
    @SerialName("court_id")
    val courtId: String = "",

    @SerializedName("screen")
    @SerialName("screen")
    val screen: String = "",

    @SerializedName("app_version")
    @SerialName("app_version")
    val appVersion: String = "",

    @SerializedName("timestamp")
    @SerialName("timestamp")
    val timestamp: String = "",

    @SerializedName("match_id")
    @SerialName("match_id")
    val matchId: String? = null,

    @SerializedName("client_match_uuid")
    @SerialName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerializedName("battery_level")
    @SerialName("battery_level")
    val batteryLevel: String? = null,

    @SerializedName("is_charging")
    @SerialName("is_charging")
    val isCharging: String? = null,

    @SerializedName("snapshot")
    @SerialName("snapshot")
    val snapshot: DirectorDeviceSnapshotDto? = null
)

@Serializable
data class DirectorDeviceSnapshotDto(
    @SerializedName("court_id")
    @SerialName("court_id")
    val courtId: String? = null,

    @SerializedName("court_name")
    @SerialName("court_name")
    val courtName: String? = null,

    @SerializedName("player1_name")
    @SerialName("player1_name")
    val player1Name: String? = null,

    @SerializedName("player2_name")
    @SerialName("player2_name")
    val player2Name: String? = null,

    @SerializedName("is_doubles")
    @SerialName("is_doubles")
    val isDoubles: Boolean? = null,

    @SerializedName("player1_sets")
    @SerialName("player1_sets")
    val player1Sets: Int? = null,

    @SerializedName("player2_sets")
    @SerialName("player2_sets")
    val player2Sets: Int? = null,

    @SerializedName("player1_games")
    @SerialName("player1_games")
    val player1Games: Int? = null,

    @SerializedName("player2_games")
    @SerialName("player2_games")
    val player2Games: Int? = null,

    @SerializedName("player1_points")
    @SerialName("player1_points")
    val player1Points: Int? = null,

    @SerializedName("player2_points")
    @SerialName("player2_points")
    val player2Points: Int? = null,

    @SerializedName("sets_history")
    @SerialName("sets_history")
    val setsHistory: List<SetScoreDto>? = null,

    @SerializedName("is_player1_serving")
    @SerialName("is_player1_serving")
    val isPlayer1Serving: Boolean? = null,

    @SerializedName("is_tiebreak")
    @SerialName("is_tiebreak")
    val isTiebreak: Boolean? = null,

    @SerializedName("is_super_tiebreak")
    @SerialName("is_super_tiebreak")
    val isSuperTiebreak: Boolean? = null,

    @SerializedName("match_start_time_ms")
    @SerialName("match_start_time_ms")
    val matchStartTimeMs: Long? = null,

    @SerializedName("match_duration_ms")
    @SerialName("match_duration_ms")
    val matchDurationMs: Long? = null,

    @SerializedName("games_per_set")
    @SerialName("games_per_set")
    val gamesPerSet: Int? = null,

    @SerializedName("sets_to_win")
    @SerialName("sets_to_win")
    val setsToWin: Int? = null,

    @SerializedName("tiebreak_at_games")
    @SerialName("tiebreak_at_games")
    val tiebreakAtGames: Int? = null,

    @SerializedName("no_advantage")
    @SerialName("no_advantage")
    val noAdvantage: Boolean? = null,

    @SerializedName("tiebreak_only")
    @SerialName("tiebreak_only")
    val tiebreakOnly: Boolean? = null,

    @SerializedName("stats_mode")
    @SerialName("stats_mode")
    val statsMode: String? = null
)

@Serializable
data class DirectorAckResponseDto(
    @SerializedName("ok")
    @SerialName("ok")
    val ok: Boolean = false,

    @SerializedName("acked")
    @SerialName("acked")
    val acked: Boolean = false
)

@Serializable
data class DirectorCommandDto(
    @SerializedName("id")
    @SerialName("id")
    val id: String? = null,

    @SerializedName("seq")
    @SerialName("seq")
    val seq: Int? = null,

    @SerializedName("type")
    @SerialName("type")
    val type: String? = null,

    @SerializedName("match_id")
    @SerialName("match_id")
    val matchId: Int? = null,

    @SerializedName("client_match_uuid")
    @SerialName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerializedName("court_id")
    @SerialName("court_id")
    val courtId: String? = null,

    @SerializedName("court_name")
    @SerialName("court_name")
    val courtName: String? = null,

    @SerializedName("court_token")
    @SerialName("court_token")
    val courtToken: String? = null,

    @SerializedName("court_token_expires_at")
    @SerialName("court_token_expires_at")
    val courtTokenExpiresAt: String? = null,

    @SerializedName("player1_name")
    @SerialName("player1_name")
    val player1Name: String? = null,

    @SerializedName("player2_name")
    @SerialName("player2_name")
    val player2Name: String? = null,

    @SerializedName("score")
    @SerialName("score")
    val score: DirectorScoreDto? = null,

    @SerializedName("match_config")
    @SerialName("match_config")
    val matchConfig: MatchConfigDto? = null
)

@Serializable
data class DirectorScoreDto(
    @SerializedName("player1_sets")
    @SerialName("player1_sets")
    val player1Sets: Int? = null,

    @SerializedName("player2_sets")
    @SerialName("player2_sets")
    val player2Sets: Int? = null,

    @SerializedName("player1_games")
    @SerialName("player1_games")
    val player1Games: Int? = null,

    @SerializedName("player2_games")
    @SerialName("player2_games")
    val player2Games: Int? = null,

    @SerializedName("player1_points")
    @SerialName("player1_points")
    val player1Points: Int? = null,

    @SerializedName("player2_points")
    @SerialName("player2_points")
    val player2Points: Int? = null,

    @SerializedName("sets_history")
    @SerialName("sets_history")
    val setsHistory: List<SetScoreDto>? = null,

    @SerializedName("is_tiebreak")
    @SerialName("is_tiebreak")
    val isTiebreak: Boolean? = null,

    @SerializedName("is_super_tiebreak")
    @SerialName("is_super_tiebreak")
    val isSuperTiebreak: Boolean? = null,

    @SerializedName("is_player1_serving")
    @SerialName("is_player1_serving")
    val isPlayer1Serving: Boolean? = null
)

@Serializable
data class MatchConfigDto(
    @SerializedName("games_per_set")
    @SerialName("games_per_set")
    val gamesPerSet: Int? = null,

    @SerializedName("sets_to_win")
    @SerialName("sets_to_win")
    val setsToWin: Int? = null,

    @SerializedName("tiebreak_points")
    @SerialName("tiebreak_points")
    val tiebreakPoints: Int? = null,

    @SerializedName("super_tiebreak_points")
    @SerialName("super_tiebreak_points")
    val superTiebreakPoints: Int? = null,

    @SerializedName("tiebreak_at_games")
    @SerialName("tiebreak_at_games")
    val tiebreakAtGames: Int? = null,

    @SerializedName("no_advantage")
    @SerialName("no_advantage")
    val noAdvantage: Boolean? = null,

    @SerializedName("tiebreak_only")
    @SerialName("tiebreak_only")
    val tiebreakOnly: Boolean? = null,

    @SerializedName("stats_mode")
    @SerialName("stats_mode")
    val statsMode: String? = null
)
