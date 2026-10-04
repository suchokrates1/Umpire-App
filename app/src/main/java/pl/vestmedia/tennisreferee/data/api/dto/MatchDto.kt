package pl.vestmedia.tennisreferee.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

@Serializable
data class MatchDto(
    @SerialName("id")
    val id: Int,

    @SerialName("court_id")
    val courtId: String,

    @SerialName("player1_name")
    val player1Name: String,

    @SerialName("player2_name")
    val player2Name: String,

    @SerialName("score")
    val score: ScoreDto,

    @SerialName("status")
    val status: MatchStatusDto,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("bracket_warning")
    val bracketWarning: String? = null,

    @SerialName("phase")
    val phase: String? = null,

    @SerialName("schedule_id")
    val scheduleId: Int? = null,

    @SerialName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerialName("finish_reason")
    val finishReason: MatchFinishReasonDto? = null,

    @SerialName("winner_name")
    val winnerName: String? = null,

    @SerialName("injured_player_name")
    val injuredPlayerName: String? = null,

    @SerialName("result_note")
    val resultNote: String? = null,

    @SerialName("match_config")
    val matchConfig: MatchConfigDto? = null,

    @SerialName("match_start_time_ms")
    val matchStartTimeMs: Long? = null,

    @SerialName("serve")
    val serve: String? = null
)

@Serializable
enum class MatchStatusDto {
    @SerialName("not_started")
    NOT_STARTED,

    @SerialName("in_progress")
    IN_PROGRESS,

    @SerialName("finished")
    FINISHED
}

@Serializable
data class ScoreDto(
    @SerialName("player1_sets")
    val player1Sets: Int = 0,

    @SerialName("player2_sets")
    val player2Sets: Int = 0,

    @SerialName("player1_games")
    val player1Games: Int = 0,

    @SerialName("player2_games")
    val player2Games: Int = 0,

    @SerialName("player1_points")
    val player1Points: Int = 0,

    @SerialName("player2_points")
    val player2Points: Int = 0,

    @SerialName("sets_history")
    val setsHistory: List<SetScoreDto> = emptyList()
)

@Serializable
data class SetScoreDto(
    @SerialName("set_number")
    val setNumber: Int,

    @SerialName("player1_games")
    val player1Games: Int,

    @SerialName("player2_games")
    val player2Games: Int,

    @SerialName("tiebreak_loser_points")
    val tiebreakLoserPoints: Int? = null,

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
