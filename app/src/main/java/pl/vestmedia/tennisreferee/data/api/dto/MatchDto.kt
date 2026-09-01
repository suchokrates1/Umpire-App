package pl.vestmedia.tennisreferee.data.api.dto

import com.google.gson.annotations.SerializedName
import pl.vestmedia.tennisreferee.domain.match.model.Match
import pl.vestmedia.tennisreferee.domain.match.model.MatchStatus
import pl.vestmedia.tennisreferee.domain.match.model.Score
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

data class MatchDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("court_id")
    val courtId: String,

    @SerializedName("player1_name")
    val player1Name: String,

    @SerializedName("player2_name")
    val player2Name: String,

    @SerializedName("score")
    val score: ScoreDto,

    @SerializedName("status")
    val status: MatchStatusDto,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?,

    @SerializedName("bracket_warning")
    val bracketWarning: String? = null,

    @SerializedName("phase")
    val phase: String? = null,

    @SerializedName("schedule_id")
    val scheduleId: Int? = null,

    @SerializedName("client_match_uuid")
    val clientMatchUuid: String? = null,

    @SerializedName("finish_reason")
    val finishReason: MatchFinishReasonDto? = null,

    @SerializedName("winner_name")
    val winnerName: String? = null,

    @SerializedName("injured_player_name")
    val injuredPlayerName: String? = null,

    @SerializedName("result_note")
    val resultNote: String? = null,

    @SerializedName("match_config")
    val matchConfig: MatchConfigDto? = null,

    @SerializedName("match_start_time_ms")
    val matchStartTimeMs: Long? = null
)

enum class MatchStatusDto {
    @SerializedName("not_started")
    NOT_STARTED,

    @SerializedName("in_progress")
    IN_PROGRESS,

    @SerializedName("finished")
    FINISHED
}

data class ScoreDto(
    @SerializedName("player1_sets")
    val player1Sets: Int = 0,

    @SerializedName("player2_sets")
    val player2Sets: Int = 0,

    @SerializedName("player1_games")
    val player1Games: Int = 0,

    @SerializedName("player2_games")
    val player2Games: Int = 0,

    @SerializedName("player1_points")
    val player1Points: Int = 0,

    @SerializedName("player2_points")
    val player2Points: Int = 0,

    @SerializedName("sets_history")
    val setsHistory: List<SetScoreDto> = emptyList()
)

data class SetScoreDto(
    @SerializedName("set_number")
    val setNumber: Int,

    @SerializedName("player1_games")
    val player1Games: Int,

    @SerializedName("player2_games")
    val player2Games: Int,

    @SerializedName("tiebreak_loser_points")
    val tiebreakLoserPoints: Int? = null,

    @SerializedName("is_super_tiebreak")
    val isSuperTiebreak: Boolean = false
)

fun MatchDto.toModel(): Match {
    return Match(
        id = id,
        courtId = courtId,
        player1Name = player1Name,
        player2Name = player2Name,
        score = score.toModel(),
        status = status.toModel(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        bracketWarning = bracketWarning,
        phase = phase,
        scheduleId = scheduleId,
        clientMatchUuid = clientMatchUuid,
        finishReason = finishReason?.toModel(),
        winnerName = winnerName,
        injuredPlayerName = injuredPlayerName,
        resultNote = resultNote
    )
}

fun Match.toDto(): MatchDto {
    return MatchDto(
        id = id,
        courtId = courtId,
        player1Name = player1Name,
        player2Name = player2Name,
        score = score.toDto(),
        status = status.toDto(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        bracketWarning = bracketWarning,
        phase = phase,
        scheduleId = scheduleId,
        clientMatchUuid = clientMatchUuid,
        finishReason = finishReason?.toDto(),
        winnerName = winnerName,
        injuredPlayerName = injuredPlayerName,
        resultNote = resultNote
    )
}

fun ScoreDto.toModel(): Score {
    return Score(
        player1Sets = player1Sets,
        player2Sets = player2Sets,
        player1Games = player1Games,
        player2Games = player2Games,
        player1Points = player1Points,
        player2Points = player2Points,
        setsHistory = setsHistory.map { it.toModel() }
    )
}

fun Score.toDto(): ScoreDto {
    return ScoreDto(
        player1Sets = player1Sets,
        player2Sets = player2Sets,
        player1Games = player1Games,
        player2Games = player2Games,
        player1Points = player1Points,
        player2Points = player2Points,
        setsHistory = setsHistory.map { it.toDto() }
    )
}

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

fun MatchStatusDto.toModel(): MatchStatus {
    return when (this) {
        MatchStatusDto.NOT_STARTED -> MatchStatus.NOT_STARTED
        MatchStatusDto.IN_PROGRESS -> MatchStatus.IN_PROGRESS
        MatchStatusDto.FINISHED -> MatchStatus.FINISHED
    }
}

fun MatchStatus.toDto(): MatchStatusDto {
    return when (this) {
        MatchStatus.NOT_STARTED -> MatchStatusDto.NOT_STARTED
        MatchStatus.IN_PROGRESS -> MatchStatusDto.IN_PROGRESS
        MatchStatus.FINISHED -> MatchStatusDto.FINISHED
    }
}