package pl.vestmedia.tennisreferee.data.model

import pl.vestmedia.tennisreferee.domain.match.model.SetScore

/**
 * Model zdarzenia meczowego wysyłanego do serwera
 */
data class MatchEvent(
    val courtId: String,

    val matchId: Int? = null,

    val clientMatchUuid: String? = null,
    
    val eventType: String, // "point", "game", "set", "match_start", "match_end", "serve_change", "side_change"
    
    val player1: PlayerInfo,
    
    val player2: PlayerInfo,
    
    val score: ScoreInfo,
    
    val stats: LiveStatsInfo? = null,
    
    val batteryLevel: Int? = null,
    
    val isCharging: Boolean? = null,
    
    val timestamp: Long = System.currentTimeMillis()
)

data class PlayerInfo(
    val name: String,
    
    val fullName: String? = null,
    
    val flag: String?,
    
    val isServing: Boolean
)

data class ScoreInfo(
    val player1Sets: Int,
    
    val player2Sets: Int,
    
    val player1Games: Int,
    
    val player2Games: Int,
    
    val player1Points: Int,
    
    val player2Points: Int,
    
    val isTiebreak: Boolean,
    
    val isSuperTiebreak: Boolean,
    
    val matchFinished: Boolean,
    
    val setsHistory: List<SetScore> = emptyList(),
    
    val statsMode: String? = null
)

data class MatchEventResponse(
    val success: Boolean,
    
    val message: String?
)

data class LiveStatsInfo(
    val player1Aces: Int,
    val player1DoubleFaults: Int,
    val player1Winners: Int,
    val player1UnforcedErrors: Int,
    val player1FirstServePct: Int,
    
    val player2Aces: Int,
    val player2DoubleFaults: Int,
    val player2Winners: Int,
    val player2UnforcedErrors: Int,
    val player2FirstServePct: Int
)
