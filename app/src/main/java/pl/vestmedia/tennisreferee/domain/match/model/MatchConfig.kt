package pl.vestmedia.tennisreferee.domain.match.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Konfiguracja formatu meczu — ilość gemów do seta, setów do meczu, opcje tiebreaka
 */
@Parcelize
data class MatchConfig(
    // Ile gemów potrzeba by wygrać seta (np. 3, 4, 6)
    val gamesPerSet: Int = 4,
    
    // Ile setów potrzeba by wygrać mecz (np. 2 z 3, 3 z 5)
    val setsToWin: Int = 2,
    
    // Tiebreak do ilu punktów (7 = normalny, 10 = super TB)
    val tiebreakPoints: Int = 7,
    
    // Super tiebreak do ilu punktów (ostatni set)
    val superTiebreakPoints: Int = 10,
    
    // Tryb statystyk
    val statsMode: StatsMode = StatsMode.ADVANCED,
    
    // No-Advantage (deciding point at deuce)
    val noAdvantage: Boolean = false,
    
    // Tryb samego tiebreaka (bez setów/gemów, od razu super TB)
    val tiebreakOnly: Boolean = false,

    // Przy ilu gemach startuje tiebreak (np. 3 = TB przy 3:3); null = domyślnie dla formatu
    val tiebreakAtGames: Int? = null
) : Parcelable {

    /** Gemy, przy których obie strony wchodzą w tiebreak seta. */
    val tiebreakAt: Int
        get() = tiebreakAtGames?.coerceIn(1, gamesPerSet) ?: defaultTiebreakAtGames(gamesPerSet)

    companion object {
        /** Krótkie sety otwierają TB o gem wcześniej (2:2 przy trzech), dłuższe na długości seta. */
        fun defaultTiebreakAtGames(gamesPerSet: Int): Int =
            if (gamesPerSet <= 3) gamesPerSet - 1 else gamesPerSet

        /** Dwa progi TB, jakie format może zaoferować: gem wcześniej albo na długości seta. */
        fun tiebreakAtOptions(gamesPerSet: Int): List<Int> =
            if (gamesPerSet > 1) listOf(gamesPerSet - 1, gamesPerSet) else listOf(gamesPerSet)

        /** Tylko tiebreak (do 10 punktów) */
        fun tiebreakOnly(points: Int = 10) = MatchConfig(
            setsToWin = 1,
            superTiebreakPoints = points,
            tiebreakOnly = true
        )
    }
}
