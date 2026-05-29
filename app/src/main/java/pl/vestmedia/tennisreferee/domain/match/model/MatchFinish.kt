package pl.vestmedia.tennisreferee.domain.match.model

enum class MatchFinishReason {
    NORMAL,

    TEST,

    RETIREMENT,

    WALKOVER
}

data class FinishMatchRequest(
    val finishReason: MatchFinishReason = MatchFinishReason.NORMAL,

    val winnerName: String? = null,

    val injuredPlayerName: String? = null,

    val resultNote: String? = null
)
