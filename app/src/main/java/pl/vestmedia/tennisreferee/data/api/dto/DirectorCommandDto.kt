package pl.vestmedia.tennisreferee.data.api.dto

import com.google.gson.annotations.SerializedName

data class DirectorCommandsResponseDto(
    @SerializedName("commands")
    val commands: List<DirectorCommandDto> = emptyList()
)

data class HeartbeatResponseDto(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("commands")
    val commands: List<DirectorCommandDto> = emptyList()
)

data class DirectorAckResponseDto(
    @SerializedName("ok")
    val ok: Boolean = false,

    @SerializedName("acked")
    val acked: Boolean = false
)

data class DirectorCommandDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("seq")
    val seq: Int? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("match_id")
    val matchId: Int? = null,

    @SerializedName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerializedName("court_id")
    val courtId: String? = null,

    @SerializedName("court_name")
    val courtName: String? = null,

    @SerializedName("court_token")
    val courtToken: String? = null,

    @SerializedName("court_token_expires_at")
    val courtTokenExpiresAt: String? = null,

    @SerializedName("player1_name")
    val player1Name: String? = null,

    @SerializedName("player2_name")
    val player2Name: String? = null,

    @SerializedName("score")
    val score: DirectorScoreDto? = null,

    @SerializedName("match_config")
    val matchConfig: MatchConfigDto? = null
)

data class DirectorScoreDto(
    @SerializedName("player1_sets")
    val player1Sets: Int? = null,

    @SerializedName("player2_sets")
    val player2Sets: Int? = null,

    @SerializedName("player1_games")
    val player1Games: Int? = null,

    @SerializedName("player2_games")
    val player2Games: Int? = null,

    @SerializedName("player1_points")
    val player1Points: Int? = null,

    @SerializedName("player2_points")
    val player2Points: Int? = null,

    @SerializedName("sets_history")
    val setsHistory: List<SetScoreDto>? = null,

    @SerializedName("is_tiebreak")
    val isTiebreak: Boolean? = null,

    @SerializedName("is_super_tiebreak")
    val isSuperTiebreak: Boolean? = null,

    @SerializedName("is_player1_serving")
    val isPlayer1Serving: Boolean? = null
)

data class MatchConfigDto(
    @SerializedName("games_per_set")
    val gamesPerSet: Int? = null,

    @SerializedName("sets_to_win")
    val setsToWin: Int? = null,

    @SerializedName("tiebreak_points")
    val tiebreakPoints: Int? = null,

    @SerializedName("super_tiebreak_points")
    val superTiebreakPoints: Int? = null,

    @SerializedName("no_advantage")
    val noAdvantage: Boolean? = null,

    @SerializedName("tiebreak_only")
    val tiebreakOnly: Boolean? = null,

    @SerializedName("stats_mode")
    val statsMode: String? = null
)
