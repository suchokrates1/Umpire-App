package pl.vestmedia.tennisreferee.data.model

data class ScheduleSuggestionResponse(
    val suggestion: ScheduleSuggestion? = null
)

data class ScheduleSuggestion(
    val id: Int,

    val tournamentId: Int,

    val dayDate: String? = null,

    val scheduledTime: String? = null,

    val courtId: String? = null,

    val courtLabel: String? = null,

    val categoryName: String? = null,

    val phase: String? = null,

    val player1Name: String,

    val player2Name: String,

    val player1: Player? = null,

    val player2: Player? = null
)