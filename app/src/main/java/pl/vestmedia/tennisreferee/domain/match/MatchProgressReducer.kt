package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

object MatchProgressReducer {
    const val ANNOUNCEMENT_SIDE_CHANGE = "side_change"
    const val ANNOUNCEMENT_TIEBREAK = "tiebreak"
    const val ANNOUNCEMENT_SUPER_TIEBREAK = "super_tiebreak"
    const val ANNOUNCEMENT_DECIDING_POINT = "deciding_point"

    fun reduceAfterPoint(
        state: MatchState,
        currentAnnouncementType: String?,
        nowMs: Long
    ): MatchProgressResult {
        var pendingAnnouncementType = currentAnnouncementType
        val events = mutableListOf<MatchProgressEvent>()

        if (!state.isGameWon()) {
            if (state.noAdvantage && state.player1Points == 3 && state.player2Points == 3
                && !state.isTiebreak && !state.isSuperTiebreak) {
                return MatchProgressResult(
                    announcementType = ANNOUNCEMENT_DECIDING_POINT,
                    nextScreen = MatchProgressScreen.Announcement,
                    publishState = true
                )
            }

            return MatchProgressResult(
                announcementType = pendingAnnouncementType,
                nextScreen = MatchProgressScreen.Scoring,
                publishState = false
            )
        }

        val player1Won = state.player1Points > state.player2Points

        if (state.isTiebreak || state.isSuperTiebreak) {
            val wasSuperTiebreak = state.isSuperTiebreak
            val tiebreakLoserPoints = if (player1Won) state.player2Points else state.player1Points

            if (wasSuperTiebreak) {
                if (player1Won) state.player1Sets++ else state.player2Sets++
                state.setsHistory.add(
                    SetScore(
                        setNumber = state.setsHistory.size + 1,
                        player1Games = state.player1Points,
                        player2Games = state.player2Points,
                        tiebreakLoserPoints = tiebreakLoserPoints,
                        isSuperTiebreak = true
                    )
                )
            } else {
                if (player1Won) state.player1Games++ else state.player2Games++
                if (player1Won) state.player1Sets++ else state.player2Sets++
                state.setsHistory.add(
                    SetScore(
                        setNumber = state.setsHistory.size + 1,
                        player1Games = state.player1Games,
                        player2Games = state.player2Games,
                        tiebreakLoserPoints = tiebreakLoserPoints
                    )
                )
            }

            state.player1Games = 0
            state.player2Games = 0
            state.isTiebreak = false
            state.isSuperTiebreak = false
            state.sidesSwapped = !state.sidesSwapped
            state.totalGamesPlayed = 0
            pendingAnnouncementType = ANNOUNCEMENT_SIDE_CHANGE

            if (state.shouldEndMatch()) {
                state.isMatchFinished = true
                state.matchDuration = nowMs - state.matchStartTime
                return MatchProgressResult(
                    events = events,
                    announcementType = pendingAnnouncementType,
                    nextScreen = MatchProgressScreen.MatchFinished,
                    publishState = true,
                    finalizeMatch = true
                )
            }

            val setsToWinTiebreak = state.matchConfig.setsToWin
            if (state.player1Sets == (setsToWinTiebreak - 1) && state.player2Sets == (setsToWinTiebreak - 1)) {
                state.isSuperTiebreak = true
                pendingAnnouncementType = ANNOUNCEMENT_SUPER_TIEBREAK
            }
        } else {
            if (player1Won) {
                state.player1Games++
            } else {
                state.player2Games++
            }

            state.totalGamesPlayed++
            if (state.totalGamesPlayed % 2 == 1) {
                state.sidesSwapped = !state.sidesSwapped
                pendingAnnouncementType = ANNOUNCEMENT_SIDE_CHANGE
            }
        }

        state.player1Points = 0
        state.player2Points = 0

        if (state.isDoubles) {
            DoublesServeRotation.rotate(state)
        } else {
            state.isPlayer1Serving = !state.isPlayer1Serving
        }
        events.add(MatchProgressEvent.Game)

        if (state.isSetWon()) {
            val setWinner = if (state.player1Games > state.player2Games) 1 else 2
            if (setWinner == 1) {
                state.player1Sets++
            } else {
                state.player2Sets++
            }

            state.setsHistory.add(
                SetScore(
                    setNumber = state.setsHistory.size + 1,
                    player1Games = state.player1Games,
                    player2Games = state.player2Games
                )
            )
            events.add(MatchProgressEvent.Set)

            if (state.shouldEndMatch()) {
                state.isMatchFinished = true
                state.matchDuration = nowMs - state.matchStartTime
                return MatchProgressResult(
                    events = events,
                    announcementType = pendingAnnouncementType,
                    nextScreen = MatchProgressScreen.MatchFinished,
                    publishState = true,
                    finalizeMatch = true
                )
            }

            val setsToWin = state.matchConfig.setsToWin
            if (state.player1Sets == (setsToWin - 1) && state.player2Sets == (setsToWin - 1)) {
                state.isSuperTiebreak = true
                pendingAnnouncementType = ANNOUNCEMENT_SUPER_TIEBREAK
            }

            state.player1Games = 0
            state.player2Games = 0
            state.totalGamesPlayed = 0
        }

        if (state.shouldStartTiebreak() && !state.isSuperTiebreak) {
            state.isTiebreak = true
            pendingAnnouncementType = ANNOUNCEMENT_TIEBREAK
        }

        return MatchProgressResult(
            events = events,
            announcementType = pendingAnnouncementType,
            nextScreen = if (pendingAnnouncementType != null) MatchProgressScreen.Announcement else MatchProgressScreen.Scoring,
            publishState = true,
            syncMatch = true
        )
    }
}

data class MatchProgressResult(
    val events: List<MatchProgressEvent> = emptyList(),
    val announcementType: String? = null,
    val nextScreen: MatchProgressScreen,
    val publishState: Boolean,
    val syncMatch: Boolean = false,
    val finalizeMatch: Boolean = false
)

enum class MatchProgressEvent {
    Game,
    Set
}

enum class MatchProgressScreen {
    Scoring,
    Announcement,
    MatchFinished
}