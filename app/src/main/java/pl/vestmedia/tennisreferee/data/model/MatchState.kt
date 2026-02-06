package pl.vestmedia.tennisreferee.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Model stanu meczu podczas rozgrywki
 */
@Parcelize
data class MatchState(
    // Identyfikator meczu na serwerze
    var matchId: Int? = null,
    
    // Wybrani gracze
    val player1: Player,
    val player2: Player,
    val player3: Player? = null,  // Dla debla - partner gracza 1
    val player4: Player? = null,  // Dla debla - partner gracza 2
    val courtId: String,
    val courtName: String,
    
    // Debel
    val isDoubles: Boolean = false,
    val team1Name: String? = null,
    val team2Name: String? = null,
    var currentServer: Int = 1, // 1-4, aktualny serwujący w deblu
    
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
     * Zwraca nazwę zespołu 1 (dla debla) lub imię gracza 1
     */
    fun getTeam1DisplayName(): String {
        return if (isDoubles && !team1Name.isNullOrEmpty()) {
            team1Name
        } else if (isDoubles && player3 != null) {
            "${player1.getDisplayName()} / ${player3.getDisplayName()}"
        } else {
            player1.getDisplayName()
        }
    }
    
    /**
     * Zwraca nazwę zespołu 2 (dla debla) lub imię gracza 2
     */
    fun getTeam2DisplayName(): String {
        return if (isDoubles && !team2Name.isNullOrEmpty()) {
            team2Name
        } else if (isDoubles && player4 != null) {
            "${player2.getDisplayName()} / ${player4.getDisplayName()}"
        } else {
            player2.getDisplayName()
        }
    }
    
    /**
     * Zwraca nazwę aktualnie serwującego gracza (dla debla)
     */
    fun getCurrentServerName(): String {
        return when (currentServer) {
            1 -> player1.getDisplayName()
            2 -> player2.getDisplayName()
            3 -> player3?.getDisplayName() ?: player1.getDisplayName()
            4 -> player4?.getDisplayName() ?: player2.getDisplayName()
            else -> player1.getDisplayName()
        }
    }
    
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
    
    /**
     * Konwertuje stan meczu na obiekt Match dla API
     */
    fun toMatch(): Match {
        return Match(
            id = matchId ?: 0,
            courtId = courtId,
            player1Name = if (isDoubles) getTeam1DisplayName() else player1.getDisplayName(),
            player2Name = if (isDoubles) getTeam2DisplayName() else player2.getDisplayName(),
            score = Score(
                player1Sets = player1Sets,
                player2Sets = player2Sets,
                player1Games = player1Games,
                player2Games = player2Games,
                player1Points = player1Points,
                player2Points = player2Points,
                setsHistory = setsHistory.toList()
            ),
            status = when {
                isMatchFinished -> MatchStatus.FINISHED
                matchStartTime > 0 -> MatchStatus.IN_PROGRESS
                else -> MatchStatus.NOT_STARTED
            },
            createdAt = null,
            updatedAt = null
        )
    }
    
    /**
     * Konwertuje statystyki meczu na obiekt dla API
     */
    fun toMatchStatisticsRequest(): MatchStatisticsRequest? {
        if (matchId == null || !isMatchFinished) return null
        
        val winner = when {
            player1Sets > player2Sets -> player1.getDisplayName()
            player2Sets > player1Sets -> player2.getDisplayName()
            else -> null
        }
        
        return MatchStatisticsRequest(
            matchId = matchId!!,
            player1Name = player1.getDisplayName(),
            player2Name = player2.getDisplayName(),
            player1Stats = PlayerStats(
                aces = player1Stats.aces,
                doubleFaults = player1Stats.doubleFaults,
                winners = player1Stats.winners,
                forcedErrors = player1Stats.forcedErrors,
                unforcedErrors = player1Stats.unforcedErrors,
                firstServes = player1Stats.firstServesTotal,
                firstServesIn = player1Stats.firstServesIn,
                firstServePercentage = player1Stats.getFirstServePercentage().toDouble()
            ),
            player2Stats = PlayerStats(
                aces = player2Stats.aces,
                doubleFaults = player2Stats.doubleFaults,
                winners = player2Stats.winners,
                forcedErrors = player2Stats.forcedErrors,
                unforcedErrors = player2Stats.unforcedErrors,
                firstServes = player2Stats.firstServesTotal,
                firstServesIn = player2Stats.firstServesIn,
                firstServePercentage = player2Stats.getFirstServePercentage().toDouble()
            ),
            matchDurationMs = matchDuration,
            winner = winner
        )
    }
}

