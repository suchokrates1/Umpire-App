package pl.vestmedia.tennisreferee.domain.match

import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

/**
 * The finished match read back as a score, not as the live point columns.
 *
 * The scoreboard only has room for two sets and its point column belongs to the game in
 * play, so a match decided in a third set (or a match tiebreak) needs its own line.
 */
object MatchScoreSummary {

    /** "2 : 1" — sets won. */
    fun sets(state: MatchState): String = "${state.player1Sets} : ${state.player2Sets}"

    /** "4:1, 1:4, 10:8" — every set in order, a set decided by a tiebreak as "4:3(5)". */
    fun setBySet(state: MatchState): String =
        state.setsHistory.joinToString(", ") { formatSet(it) }

    private fun formatSet(set: SetScore): String {
        val score = "${set.player1Games}:${set.player2Games}"
        val tiebreakLoserPoints = set.tiebreakLoserPoints
        return if (!set.isSuperTiebreak && tiebreakLoserPoints != null) {
            "$score($tiebreakLoserPoints)"
        } else {
            score
        }
    }
}
