package pl.vestmedia.tennisreferee.ui.match

import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

/**
 * UI-level doubles team label with the serving player marked by a tennis ball icon.
 * Kept in `ui.match` so `domain` models stay free of presentation emojis.
 */
fun MatchState.team1ServerAwareDisplayName(): String =
    formatServerAwareTeam(player1, player3, currentServer == 1, currentServer == 3)

fun MatchState.team2ServerAwareDisplayName(): String =
    formatServerAwareTeam(player2, player4, currentServer == 2, currentServer == 4)

private fun formatServerAwareTeam(
    primaryPlayer: Player,
    partnerPlayer: Player?,
    isPrimaryServer: Boolean,
    isPartnerServer: Boolean,
): String {
    if (partnerPlayer == null) {
        return markServingPlayer(primaryPlayer.getDisplayName(), isPrimaryServer)
    }
    return listOf(
        markServingPlayer(primaryPlayer.getDisplayName(), isPrimaryServer),
        markServingPlayer(partnerPlayer.getDisplayName(), isPartnerServer),
    ).joinToString(" / ")
}

private fun markServingPlayer(name: String, isServer: Boolean): String =
    if (isServer) "🎾 $name" else name
