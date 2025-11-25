package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

/**
 * Model dla wysyłania statystyk meczu do API
 */
data class MatchStatisticsRequest(
    @SerializedName("match_id")
    val matchId: Int,
    
    @SerializedName("player1_name")
    val player1Name: String,
    
    @SerializedName("player2_name")
    val player2Name: String,
    
    @SerializedName("player1_stats")
    val player1Stats: PlayerStats,
    
    @SerializedName("player2_stats")
    val player2Stats: PlayerStats,
    
    @SerializedName("match_duration_ms")
    val matchDurationMs: Long,
    
    @SerializedName("winner")
    val winner: String?
)

data class PlayerStats(
    @SerializedName("aces")
    val aces: Int,
    
    @SerializedName("double_faults")
    val doubleFaults: Int,
    
    @SerializedName("winners")
    val winners: Int,
    
    @SerializedName("forced_errors")
    val forcedErrors: Int,
    
    @SerializedName("unforced_errors")
    val unforcedErrors: Int,
    
    @SerializedName("first_serves")
    val firstServes: Int,
    
    @SerializedName("first_serves_in")
    val firstServesIn: Int,
    
    @SerializedName("first_serve_percentage")
    val firstServePercentage: Double
)
