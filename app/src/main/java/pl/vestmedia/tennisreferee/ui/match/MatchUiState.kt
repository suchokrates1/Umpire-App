package pl.vestmedia.tennisreferee.ui.match

data class BracketWarningEvent(
    val type: String,
    val matchId: Int
)

enum class MatchView {
    SERVER_SELECTION,
    SERVE,
    RALLY,
    BASIC_SCORING,
    ANNOUNCEMENT,
    MATCH_FINISHED
}

enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    FAILED,
    OFFLINE
}