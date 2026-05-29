package pl.vestmedia.tennisreferee.data.model

import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason

/**
 * Model reprezentujący mecz tenisowy
 */
data class Match(
    val id: Int,
    
    val courtId: String,
    
    val player1Name: String,
    
    val player2Name: String,
    
    val score: Score,
    
    val status: MatchStatus,
    
    val createdAt: String?,
    
    val updatedAt: String?,
    
    val bracketWarning: String? = null,
    
    val phase: String? = null,

    val scheduleId: Int? = null,

    val clientMatchUuid: String? = null,

    val finishReason: MatchFinishReason? = null,

    val winnerName: String? = null,

    val injuredPlayerName: String? = null,

    val resultNote: String? = null
)

enum class MatchStatus {
    NOT_STARTED,
    
    IN_PROGRESS,
    
    FINISHED
}
