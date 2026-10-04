package pl.vestmedia.tennisreferee.domain.match.model

import kotlinx.serialization.Serializable

@Serializable
enum class MatchFinishReason {
    NORMAL,

    TEST,

    RETIREMENT,

    WALKOVER
}

@Serializable
data class FinishMatchRequest(
    val finishReason: MatchFinishReason = MatchFinishReason.NORMAL,

    val winnerName: String? = null,

    val injuredPlayerName: String? = null,

    val resultNote: String? = null
)
