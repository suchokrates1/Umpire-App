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
