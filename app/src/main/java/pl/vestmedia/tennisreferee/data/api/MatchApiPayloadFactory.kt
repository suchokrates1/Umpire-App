package pl.vestmedia.tennisreferee.data.api

import pl.vestmedia.tennisreferee.data.model.Match
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.data.model.MatchStatisticsRequest
import pl.vestmedia.tennisreferee.data.model.MatchStatus
import pl.vestmedia.tennisreferee.data.model.PlayerStats
import pl.vestmedia.tennisreferee.data.model.Score

object MatchApiPayloadFactory {
    fun toMatch(state: MatchState): Match {
        return Match(
            id = state.matchId ?: 0,
            courtId = state.courtId,
            player1Name = if (state.isDoubles) state.getTeam1FullName() else state.player1.getFullName(),
            player2Name = if (state.isDoubles) state.getTeam2FullName() else state.player2.getFullName(),
            score = Score(
                player1Sets = state.player1Sets,
                player2Sets = state.player2Sets,
                player1Games = state.player1Games,
                player2Games = state.player2Games,
                player1Points = state.player1Points,
                player2Points = state.player2Points,
                setsHistory = state.setsHistory.toList()
            ),
            status = when {
                state.isMatchFinished -> MatchStatus.FINISHED
                state.matchStartTime > 0 -> MatchStatus.IN_PROGRESS
                else -> MatchStatus.NOT_STARTED
            },
            createdAt = null,
            updatedAt = null
        )
    }

    fun toStatisticsRequest(state: MatchState): MatchStatisticsRequest? {
        if (state.matchId == null || !state.isMatchFinished) return null

        val winner = when {
            state.player1Sets > state.player2Sets -> state.player1.getFullName()
            state.player2Sets > state.player1Sets -> state.player2.getFullName()
            else -> null
        }

        return MatchStatisticsRequest(
            matchId = state.matchId!!,
            player1Name = state.player1.getFullName(),
            player2Name = state.player2.getFullName(),
            player1Stats = PlayerStats(
                aces = state.player1Stats.aces,
                doubleFaults = state.player1Stats.doubleFaults,
                winners = state.player1Stats.winners,
                forcedErrors = state.player1Stats.forcedErrors,
                unforcedErrors = state.player1Stats.unforcedErrors,
                firstServes = state.player1Stats.firstServesTotal,
                firstServesIn = state.player1Stats.firstServesIn,
                firstServePercentage = state.player1Stats.getFirstServePercentage().toDouble()
            ),
            player2Stats = PlayerStats(
                aces = state.player2Stats.aces,
                doubleFaults = state.player2Stats.doubleFaults,
                winners = state.player2Stats.winners,
                forcedErrors = state.player2Stats.forcedErrors,
                unforcedErrors = state.player2Stats.unforcedErrors,
                firstServes = state.player2Stats.firstServesTotal,
                firstServesIn = state.player2Stats.firstServesIn,
                firstServePercentage = state.player2Stats.getFirstServePercentage().toDouble()
            ),
            matchDurationMs = state.matchDuration,
            winner = winner,
            statsMode = state.statsMode.name
        )
    }
}