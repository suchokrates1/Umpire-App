package pl.vestmedia.tennisreferee.data.api

import pl.vestmedia.tennisreferee.data.api.dto.MatchDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchConfigDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatusDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatisticsRequestDto
import pl.vestmedia.tennisreferee.data.api.dto.PlayerStatsDto
import pl.vestmedia.tennisreferee.data.api.dto.ScoreDto
import pl.vestmedia.tennisreferee.data.api.dto.toDto
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason

object MatchApiPayloadFactory {
    fun toMatch(state: MatchState): MatchDto {
        return MatchDto(
            id = state.matchId ?: 0,
            courtId = state.courtId,
            player1Name = if (state.isDoubles) state.getTeam1FullName() else state.player1.getFullName(),
            player2Name = if (state.isDoubles) state.getTeam2FullName() else state.player2.getFullName(),
            score = ScoreDto(
                player1Sets = state.player1Sets,
                player2Sets = state.player2Sets,
                player1Games = state.player1Games,
                player2Games = state.player2Games,
                player1Points = state.player1Points,
                player2Points = state.player2Points,
                setsHistory = state.setsHistory.map { it.toDto() }
            ),
            status = when {
                state.isMatchFinished -> MatchStatusDto.FINISHED
                state.matchStartTime > 0 -> MatchStatusDto.IN_PROGRESS
                else -> MatchStatusDto.NOT_STARTED
            },
            createdAt = null,
            updatedAt = null,
            scheduleId = state.scheduleId,
            clientMatchUuid = state.clientMatchUuid,
            finishReason = state.finishReason.toDto(),
            winnerName = state.finishWinnerName,
            injuredPlayerName = state.injuredPlayerName,
            resultNote = state.resultNote,
            matchConfig = MatchConfigDto(
                gamesPerSet = state.matchConfig.gamesPerSet,
                setsToWin = state.matchConfig.setsToWin,
                tiebreakPoints = state.matchConfig.tiebreakPoints,
                superTiebreakPoints = state.matchConfig.superTiebreakPoints,
                noAdvantage = state.matchConfig.noAdvantage || state.noAdvantage,
                tiebreakOnly = state.matchConfig.tiebreakOnly,
                statsMode = state.statsMode.name
            ),
            matchStartTimeMs = state.matchStartTime.takeIf { it > 0L }
        )
    }

    fun toFinishRequest(state: MatchState): FinishMatchRequest {
        return FinishMatchRequest(
            finishReason = state.finishReason,
            winnerName = state.finishWinnerName,
            injuredPlayerName = state.injuredPlayerName,
            resultNote = state.resultNote
        )
    }

    fun toStatisticsRequest(state: MatchState): MatchStatisticsRequestDto? {
        if (state.matchId == null || !state.isMatchFinished) return null
        if (state.finishReason == MatchFinishReason.TEST) return null

        val winner = when {
            state.player1Sets > state.player2Sets -> state.player1.getFullName()
            state.player2Sets > state.player1Sets -> state.player2.getFullName()
            else -> null
        }

        return MatchStatisticsRequestDto(
            matchId = state.matchId!!,
            player1Name = state.player1.getFullName(),
            player2Name = state.player2.getFullName(),
            player1Stats = PlayerStatsDto(
                aces = state.player1Stats.aces,
                doubleFaults = state.player1Stats.doubleFaults,
                winners = state.player1Stats.winners,
                forcedErrors = state.player1Stats.forcedErrors,
                unforcedErrors = state.player1Stats.unforcedErrors,
                firstServes = state.player1Stats.firstServesTotal,
                firstServesIn = state.player1Stats.firstServesIn,
                firstServePercentage = state.player1Stats.getFirstServePercentage().toDouble()
            ),
            player2Stats = PlayerStatsDto(
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