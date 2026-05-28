package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.data.model.MatchState

object MatchPointReducer {
    const val ANNOUNCEMENT_SIDE_CHANGE = "side_change"

    fun addPoint(state: MatchState, isPlayer1: Boolean): MatchPointResult {
        val events = mutableListOf(MatchPointEvent.Point)

        if (isPlayer1) {
            state.player1Points++
        } else {
            state.player2Points++
        }

        var announcementType: String? = null
        var showAnnouncementImmediately = false

        if (state.isTiebreak || state.isSuperTiebreak) {
            val totalPoints = state.player1Points + state.player2Points

            if (totalPoints % 2 == 1) {
                if (state.isDoubles) {
                    DoublesServeRotation.rotate(state)
                } else {
                    state.isPlayer1Serving = !state.isPlayer1Serving
                }
                events.add(MatchPointEvent.ServeChange)
            }

            if (totalPoints > 0 && totalPoints % 6 == 0 && !state.isGameWon()) {
                state.sidesSwapped = !state.sidesSwapped
                events.add(MatchPointEvent.SideChange)
                announcementType = ANNOUNCEMENT_SIDE_CHANGE
                showAnnouncementImmediately = true
            }
        }

        return MatchPointResult(
            events = events,
            announcementType = announcementType,
            showAnnouncementImmediately = showAnnouncementImmediately
        )
    }
}

data class MatchPointResult(
    val events: List<MatchPointEvent>,
    val announcementType: String? = null,
    val showAnnouncementImmediately: Boolean = false
)

enum class MatchPointEvent {
    Point,
    ServeChange,
    SideChange
}