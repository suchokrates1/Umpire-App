package pl.vestmedia.tennisreferee.data.model

/**
 * Model dla wysyłania statystyk meczu do API
 */
data class MatchStatisticsRequest(
    val matchId: Int,
    
    val player1Name: String,
    
    val player2Name: String,
    
    val player1Stats: PlayerStats,
    
    val player2Stats: PlayerStats,
    
    val matchDurationMs: Long,
    
    val winner: String?,
    
    val statsMode: String? = null
)

data class PlayerStats(
    val aces: Int,
    
    val doubleFaults: Int,
    
    val winners: Int,
    
    val forcedErrors: Int,
    
    val unforcedErrors: Int,
    
    val firstServes: Int,
    
    val firstServesIn: Int,
    
    val firstServePercentage: Double
)
