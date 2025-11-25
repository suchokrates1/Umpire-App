package pl.vestmedia.tennisreferee.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Model stanu meczu podczas rozgrywki
 */
@Parcelize
data class MatchState(
    // Wybrani gracze
    val player1: Player,
    val player2: Player,
    val courtId: String,
    val courtName: String,
    
    // Wyniki
    var player1Sets: Int = 0,
    var player2Sets: Int = 0,
    var player1Games: Int = 0,
    var player2Games: Int = 0,
    var player1Points: Int = 0,
    var player2Points: Int = 0,
    
    // Historia setów
    val setsHistory: @RawValue MutableList<SetScore> = mutableListOf(),
    
    // Stan gry
    var isPlayer1Serving: Boolean = true,
    var isFirstServe: Boolean = true,
    var isTiebreak: Boolean = false,
    var isSuperTiebreak: Boolean = false,
    var isMatchFinished: Boolean = false,
    var sidesSwapped: Boolean = false, // Czy gracze zamienili strony
    var totalGamesPlayed: Int = 0, // Liczba rozegranych gemów w secie
    
    // Czas
    var matchStartTime: Long = 0,
    var matchDuration: Long = 0,
    
    // Statystyki
    val player1Stats: MatchStatistics = MatchStatistics(),
    val player2Stats: MatchStatistics = MatchStatistics(),
    
    // Historia akcji (do cofania)
    val actionsHistory: @RawValue MutableList<MatchAction> = mutableListOf()
) : Parcelable {
    
    /**
     * Zwraca punkty w formacie tenisowym (0, 15, 30, 40, ADV)
     */
    fun getPlayer1PointsDisplay(): String {
        return getPointsDisplay(player1Points, player2Points, isTiebreak || isSuperTiebreak)
    }
    
    fun getPlayer2PointsDisplay(): String {
        return getPointsDisplay(player2Points, player1Points, isTiebreak || isSuperTiebreak)
    }
    
    private fun getPointsDisplay(points: Int, opponentPoints: Int, isTiebreakMode: Boolean): String {
        if (isTiebreakMode) {
            return points.toString()
        }
        
        return when {
            points >= 4 && opponentPoints >= 4 -> {
                when {
                    points == opponentPoints -> "40"
                    points > opponentPoints -> "ADV"
                    else -> "40"
                }
            }
            points >= 4 -> "40"
            else -> when(points) {
                0 -> "0"
                1 -> "15"
                2 -> "30"
                3 -> "40"
                else -> "0"
            }
        }
    }
    
    /**
     * Sprawdza czy ktoś wygrał gema
     */
    fun isGameWon(): Boolean {
        if (isTiebreak) {
            // Tie-break do 7 (z przewagą 2)
            return (player1Points >= 7 || player2Points >= 7) && 
                   kotlin.math.abs(player1Points - player2Points) >= 2
        } else if (isSuperTiebreak) {
            // Super tie-break do 10 (z przewagą 2)
            return (player1Points >= 10 || player2Points >= 10) && 
                   kotlin.math.abs(player1Points - player2Points) >= 2
        } else {
            // Normalny gem
            return (player1Points >= 4 || player2Points >= 4) && 
                   kotlin.math.abs(player1Points - player2Points) >= 2
        }
    }
    
    /**
     * Sprawdza czy ktoś wygrał seta
     * Wygrana: 4:0, 4:1, 4:2, 5:3
     */
    fun isSetWon(): Boolean {
        if (isTiebreak || isSuperTiebreak) {
            return false // W tiebreaku sprawdzamy isGameWon
        }
        
        // Wygrana przy 4 gemach z przewagą 2 (4:0, 4:1, 4:2)
        if ((player1Games >= 4 && player2Games <= 2 && player1Games - player2Games >= 2) ||
            (player2Games >= 4 && player1Games <= 2 && player2Games - player1Games >= 2)) {
            return true
        }
        
        // Wygrana przy 5:3
        if ((player1Games == 5 && player2Games == 3) || (player2Games == 5 && player1Games == 3)) {
            return true
        }
        
        return false
    }
    
    /**
     * Sprawdza czy powinien zacząć się tiebreak
     * Tiebreak rozpoczyna się przy 4:4
     */
    fun shouldStartTiebreak(): Boolean {
        return player1Games == 4 && player2Games == 4 && !isTiebreak && !isSuperTiebreak
    }
    
    /**
     * Sprawdza czy mecz powinien się zakończyć
     */
    fun shouldEndMatch(): Boolean {
        return player1Sets == 2 || player2Sets == 2
    }
}
