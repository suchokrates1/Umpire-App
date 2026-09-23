package pl.vestmedia.tennisreferee.data.api.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

@Serializable
data class MatchDto(
    @SerializedName("id")
    @SerialName("id")
    val id: Int,

    @SerializedName("court_id")
    @SerialName("court_id")
    val courtId: String,

    @SerializedName("player1_name")
    @SerialName("player1_name")
    val player1Name: String,

    @SerializedName("player2_name")
    @SerialName("player2_name")
    val player2Name: String,

    @SerializedName("score")
    @SerialName("score")
    val score: ScoreDto,

    @SerializedName("status")
    @SerialName("status")
    val status: MatchStatusDto,

    @SerializedName("created_at")
    @SerialName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("bracket_warning")
    @SerialName("bracket_warning")
    val bracketWarning: String? = null,

    @SerializedName("phase")
    @SerialName("phase")
    val phase: String? = null,

    @SerializedName("schedule_id")
    @SerialName("schedule_id")
    val scheduleId: Int? = null,

    @SerializedName("client_match_uuid")
    @SerialName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerializedName("finish_reason")
    @SerialName("finish_reason")
    val finishReason: MatchFinishReasonDto? = null,

    @SerializedName("winner_name")
    @SerialName("winner_name")
    val winnerName: String? = null,

    @SerializedName("injured_player_name")
    @SerialName("injured_player_name")
    val injuredPlayerName: String? = null,

    @SerializedName("result_note")
    @SerialName("result_note")
    val resultNote: String? = null,

    @SerializedName("match_config")
    @SerialName("match_config")
    val matchConfig: MatchConfigDto? = null,

    @SerializedName("match_start_time_ms")
    @SerialName("match_start_time_ms")
    val matchStartTimeMs: Long? = null,

    @SerializedName("serve")
    @SerialName("serve")
    val serve: String? = null
)

@Serializable
enum class MatchStatusDto {
    @SerializedName("not_started")
    @SerialName("not_started")
    NOT_STARTED,

    @SerializedName("in_progress")
    @SerialName("in_progress")
    IN_PROGRESS,

    @SerializedName("finished")
    @SerialName("finished")
    FINISHED
}

@Serializable
data class ScoreDto(
    @SerializedName("player1_sets")
    @SerialName("player1_sets")
    val player1Sets: Int = 0,

    @SerializedName("player2_sets")
    @SerialName("player2_sets")
    val player2Sets: Int = 0,

    @SerializedName("player1_games")
    @SerialName("player1_games")
    val player1Games: Int = 0,

    @SerializedName("player2_games")
    @SerialName("player2_games")
    val player2Games: Int = 0,

    @SerializedName("player1_points")
    @SerialName("player1_points")
    val player1Points: Int = 0,

    @SerializedName("player2_points")
    @SerialName("player2_points")
    val player2Points: Int = 0,

    @SerializedName("sets_history")
    @SerialName("sets_history")
    val setsHistory: List<SetScoreDto> = emptyList()
)

@Serializable
data class SetScoreDto(
    @SerializedName("set_number")
    @SerialName("set_number")
    val setNumber: Int,

    @SerializedName("player1_games")
    @SerialName("player1_games")
    val player1Games: Int,

    @SerializedName("player2_games")
    @SerialName("player2_games")
    val player2Games: Int,

    @SerializedName("tiebreak_loser_points")
    @SerialName("tiebreak_loser_points")
    val tiebreakLoserPoints: Int? = null,

    @SerializedName("is_super_tiebreak")
    @SerialName("is_super_tiebreak")
    val isSuperTiebreak: Boolean = false
)

fun SetScoreDto.toModel(): SetScore {
    return SetScore(
        setNumber = setNumber,
        player1Games = player1Games,
        player2Games = player2Games,
        tiebreakLoserPoints = tiebreakLoserPoints,
        isSuperTiebreak = isSuperTiebreak
    )
}

fun SetScore.toDto(): SetScoreDto {
    return SetScoreDto(
        setNumber = setNumber,
        player1Games = player1Games,
        player2Games = player2Games,
        tiebreakLoserPoints = tiebreakLoserPoints,
        isSuperTiebreak = isSuperTiebreak
    )
}
