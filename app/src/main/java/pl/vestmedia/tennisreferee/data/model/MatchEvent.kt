package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

/**
 * Model zdarzenia meczowego wysyłanego do serwera
 */
data class MatchEvent(
    @SerializedName("court_id")
    val courtId: String,
    
    @SerializedName("event_type")
    val eventType: String, // "point", "game", "set", "match_start", "match_end", "serve_change", "side_change"
    
    @SerializedName("player1")
    val player1: PlayerInfo,
    
    @SerializedName("player2")
    val player2: PlayerInfo,
    
    @SerializedName("score")
    val score: ScoreInfo,
    
    @SerializedName("stats")
    val stats: LiveStatsInfo? = null,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

data class PlayerInfo(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("flag")
    val flag: String?,
    
    @SerializedName("is_serving")
    val isServing: Boolean
)

data class ScoreInfo(
    @SerializedName("player1_sets")
    val player1Sets: Int,
    
    @SerializedName("player2_sets")
    val player2Sets: Int,
    
    @SerializedName("player1_games")
    val player1Games: Int,
    
    @SerializedName("player2_games")
    val player2Games: Int,
    
    @SerializedName("player1_points")
    val player1Points: Int,
    
    @SerializedName("player2_points")
    val player2Points: Int,
    
    @SerializedName("is_tiebreak")
    val isTiebreak: Boolean,
    
    @SerializedName("is_super_tiebreak")
    val isSuperTiebreak: Boolean,
    
    @SerializedName("match_finished")
    val matchFinished: Boolean
)

data class MatchEventResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String?
)

data class LiveStatsInfo(
    @SerializedName("player1_aces")
    val player1Aces: Int,
    @SerializedName("player1_double_faults")
    val player1DoubleFaults: Int,
    @SerializedName("player1_winners")
    val player1Winners: Int,
    @SerializedName("player1_unforced_errors")
    val player1UnforcedErrors: Int,
    @SerializedName("player1_first_serve_pct")
    val player1FirstServePct: Int,
    
    @SerializedName("player2_aces")
    val player2Aces: Int,
    @SerializedName("player2_double_faults")
    val player2DoubleFaults: Int,
    @SerializedName("player2_winners")
    val player2Winners: Int,
    @SerializedName("player2_unforced_errors")
    val player2UnforcedErrors: Int,
    @SerializedName("player2_first_serve_pct")
    val player2FirstServePct: Int
)
