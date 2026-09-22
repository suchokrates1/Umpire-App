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
    val tiebreakOnly: Boolean = false
) : Parcelable {
    
    companion object {
        /** Tylko tiebreak (do 10 punktów) */
        fun tiebreakOnly(points: Int = 10) = MatchConfig(
            setsToWin = 1,
            superTiebreakPoints = points,
            tiebreakOnly = true
        )
    }
}
