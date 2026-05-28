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
    val isMixedDoubles: Boolean = false,
    val team1Name: String? = null,
    val team2Name: String? = null,
    val umpireName: String? = null,
    val manualStartTime: Long? = null,
    var currentServer: Int = 1, // 1-4, aktualny serwujący w deblu
    
    // No-Advantage (deciding point) mode — at deuce, next point wins the game
    val noAdvantage: Boolean = false,
    
    // Konfiguracja formatu meczu
    val matchConfig: MatchConfig = MatchConfig(),
    
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
    
    // Tryb statystyk (BASIC = uproszczony, ADVANCED = pełny)
    var statsMode: StatsMode = StatsMode.ADVANCED,
    
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
     * Zwraca pełną nazwę zespołu 1 (dla debla) lub pełne imię i nazwisko gracza 1
     */
    fun getTeam1FullName(): String {
        return if (isDoubles && !team1Name.isNullOrEmpty()) {
            team1Name
        } else if (isDoubles && player3 != null) {
            "${player1.getFullName()} / ${player3.getFullName()}"
        } else {
            player1.getFullName()
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
     * Zwraca pełną nazwę zespołu 2 (dla debla) lub pełne imię i nazwisko gracza 2
     */
    fun getTeam2FullName(): String {
        return if (isDoubles && !team2Name.isNullOrEmpty()) {
            team2Name
        } else if (isDoubles && player4 != null) {
            "${player2.getFullName()} / ${player4.getFullName()}"
        } else {
            player2.getFullName()
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

    fun getTeam1ServerAwareDisplayName(): String {
        return formatTeamDisplay(player1, player3, currentServer == 1, currentServer == 3)
    }

    fun getTeam2ServerAwareDisplayName(): String {
        return formatTeamDisplay(player2, player4, currentServer == 2, currentServer == 4)
    }

    private fun formatTeamDisplay(
        primaryPlayer: Player,
        partnerPlayer: Player?,
        isPrimaryServer: Boolean,
        isPartnerServer: Boolean
    ): String {
        if (partnerPlayer == null) {
            return markServer(primaryPlayer.getDisplayName(), isPrimaryServer)
        }

        return listOf(
            markServer(primaryPlayer.getDisplayName(), isPrimaryServer),
            markServer(partnerPlayer.getDisplayName(), isPartnerServer)
        ).joinToString(" / ")
    }

    private fun markServer(name: String, isServer: Boolean): String {
        return if (isServer) "🎾 $name" else name
    }

    /**
     * Zwraca etykietę typu meczu do wyświetlenia w UI.
     */
    fun getMatchTypeLabel(): String {
        return when {
            isMixedDoubles -> "Mixed"
            isDoubles -> "Doubles"
            else -> "Singles"
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
        
        // No-Advantage mode: at deuce, show "40-40" (immediate deciding point)
        if (noAdvantage) {
            return when(points) {
                0 -> "0"
                1 -> "15"
                2 -> "30"
                else -> "40"  // 3+ always shows "40", game decided at first to 4
            }
        }
        
        return when {
            // Deuce/Advantage territory: both players reached 40 (3 points)
            points >= 3 && opponentPoints >= 3 -> {
                when {
                    points == opponentPoints -> "40"  // Deuce
                    points > opponentPoints -> "ADV"  // Advantage this player
                    else -> "40"  // Opponent has advantage
                }
            }
            else -> when(points) {
                0 -> "0"
                1 -> "15"
                2 -> "30"
                3 -> "40"
                else -> "40"  // Should not happen (game won before this)
            }
        }
    }
    
    /**
     * Sprawdza czy ktoś wygrał gema
     */
    fun isGameWon(): Boolean {
        if (isTiebreak) {
            // Tie-break do matchConfig.tiebreakPoints (z przewagą 2)
            val tbPts = matchConfig.tiebreakPoints
            return (player1Points >= tbPts || player2Points >= tbPts) && 
                   kotlin.math.abs(player1Points - player2Points) >= 2
        } else if (isSuperTiebreak) {
            // Super tie-break do matchConfig.superTiebreakPoints (z przewagą 2)
            val stbPts = matchConfig.superTiebreakPoints
            return (player1Points >= stbPts || player2Points >= stbPts) && 
                   kotlin.math.abs(player1Points - player2Points) >= 2
        } else if (noAdvantage) {
            // No-Advantage: at deuce (3-3), next point wins — first to 4 wins
            return player1Points >= 4 || player2Points >= 4
        } else {
            // Normalny gem z advantage
            return (player1Points >= 4 || player2Points >= 4) && 
                   kotlin.math.abs(player1Points - player2Points) >= 2
        }
    }
    
    /**
     * Sprawdza czy ktoś wygrał seta
     * Np. przy gamesPerSet=4: wygrana 4:0, 4:1, 4:2, 5:3
     * Np. przy gamesPerSet=3 (krótki set): wygrana 3:0, 3:1, 3:2 (po TB z 2:2)
     * Np. przy gamesPerSet=6: wygrana 6:0..6:4, 7:5
     */
    fun isSetWon(): Boolean {
        if (isTiebreak || isSuperTiebreak) {
            return false // W tiebreaku sprawdzamy isGameWon
        }
        
        val gps = matchConfig.gamesPerSet
        
        // Krótkie sety (gps=3): set wygrywa ten kto pierwszy osiągnie gps gemów
        // (bo TB startuje przy gps-1:gps-1, np. 2:2, i zwycięzca TB dostaje gem → 3:2)
        if (gps <= 3) {
            return player1Games >= gps || player2Games >= gps
        }
        
        // Standardowe sety: wygrana z przewagą ≥2 gemów
        if ((player1Games >= gps && player1Games - player2Games >= 2) ||
            (player2Games >= gps && player2Games - player1Games >= 2)) {
            return true
        }
        
        // Wygrana przy gamesPerSet+1 : gamesPerSet-1 (np. 5:3 przy gps=4, 7:5 przy gps=6)
        val gpsPlus1 = gps + 1
        val gpsMinus1 = gps - 1
        if ((player1Games == gpsPlus1 && player2Games == gpsMinus1) ||
            (player2Games == gpsPlus1 && player1Games == gpsMinus1)) {
            return true
        }
        
        return false
    }
    
    /**
     * Sprawdza czy powinien zacząć się tiebreak.
     * Krótkie sety (gamesPerSet=3): TB przy (gps-1):(gps-1) → 2:2
     * Standardowe sety (gamesPerSet=4,6): TB przy gps:gps → 4:4, 6:6
     */
    fun shouldStartTiebreak(): Boolean {
        val gps = matchConfig.gamesPerSet
        val tbTrigger = if (gps <= 3) gps - 1 else gps
        return player1Games == tbTrigger && player2Games == tbTrigger && !isTiebreak && !isSuperTiebreak
    }
    
    /**
     * Sprawdza czy mecz powinien się zakończyć
     */
    fun shouldEndMatch(): Boolean {
        return player1Sets == matchConfig.setsToWin || player2Sets == matchConfig.setsToWin
    }
    
}

