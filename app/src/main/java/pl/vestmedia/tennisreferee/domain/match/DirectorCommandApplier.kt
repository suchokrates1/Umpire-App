package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.data.api.dto.DirectorCommandDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchConfigDto
import pl.vestmedia.tennisreferee.data.api.dto.toModel
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode

object DirectorCommandApplier {
    fun appliesTo(state: MatchState, command: DirectorCommandDto): Boolean {
        val matchId = command.matchId
        if (matchId != null && state.matchId != null && matchId != state.matchId) {
            return false
        }
        val uuid = command.clientMatchUuid?.trim().orEmpty()
        if (uuid.isNotEmpty() && uuid != state.clientMatchUuid) {
            return false
        }
        return true
    }

    fun apply(state: MatchState, command: DirectorCommandDto): MatchState {
        var next = state
        command.courtId?.trim()?.takeIf { it.isNotEmpty() }?.let { courtId ->
            next = next.copy(
                courtId = courtId,
                courtName = command.courtName?.trim()?.takeIf { it.isNotEmpty() } ?: courtId
            )
        }
        command.player1Name?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            next = if (next.isDoubles) {
                next.copy(team1Name = name)
            } else {
                next.copy(player1 = renamePlayer(next.player1, name))
            }
        }
        command.player2Name?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            next = if (next.isDoubles) {
                next.copy(team2Name = name)
            } else {
                next.copy(player2 = renamePlayer(next.player2, name))
            }
        }
        command.matchConfig?.let { config ->
            val applied = applyConfig(next.matchConfig, config)
            next = next.copy(
                matchConfig = applied,
                noAdvantage = applied.noAdvantage
            )
            config.statsMode?.let { mode ->
                runCatching { StatsMode.valueOf(mode.uppercase()) }.getOrNull()?.let { parsed ->
                    next.statsMode = parsed
                }
            }
        }
        command.score?.let { score ->
            score.player1Sets?.let { next.player1Sets = it }
            score.player2Sets?.let { next.player2Sets = it }
            score.player1Games?.let { next.player1Games = it }
            score.player2Games?.let { next.player2Games = it }
            score.player1Points?.let { next.player1Points = it }
            score.player2Points?.let { next.player2Points = it }
            score.isTiebreak?.let { next.isTiebreak = it }
            score.isSuperTiebreak?.let { next.isSuperTiebreak = it }
            score.isPlayer1Serving?.let { next.isPlayer1Serving = it }
            score.setsHistory?.let { history ->
                next.setsHistory.clear()
                next.setsHistory.addAll(history.map { it.toModel() })
            }
        }
        return next
    }

    private fun applyConfig(current: MatchConfig, patch: MatchConfigDto): MatchConfig {
        return current.copy(
            gamesPerSet = patch.gamesPerSet ?: current.gamesPerSet,
            setsToWin = patch.setsToWin ?: current.setsToWin,
            tiebreakPoints = patch.tiebreakPoints ?: current.tiebreakPoints,
            superTiebreakPoints = patch.superTiebreakPoints ?: current.superTiebreakPoints,
            noAdvantage = patch.noAdvantage ?: current.noAdvantage,
            tiebreakOnly = patch.tiebreakOnly ?: current.tiebreakOnly,
            statsMode = patch.statsMode?.let { runCatching { StatsMode.valueOf(it.uppercase()) }.getOrNull() }
                ?: current.statsMode
        )
    }

    internal fun renamePlayer(player: Player, fullName: String): Player {
        val parts = fullName.trim().split(Regex("\\s+"), limit = 2)
        val first = parts.getOrNull(0).orEmpty()
        val last = parts.getOrNull(1).orEmpty()
        return player.copy(
            name = fullName.trim(),
            firstName = first,
            lastName = last.ifEmpty { first }
        )
    }
}
