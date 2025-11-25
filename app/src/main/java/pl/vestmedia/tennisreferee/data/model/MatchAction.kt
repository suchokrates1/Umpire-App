package pl.vestmedia.tennisreferee.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Model reprezentujący pojedynczą akcję w meczu (do cofania)
 */
@Parcelize
data class MatchAction(
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: ActionType,
    
    // Stan przed akcją (do przywrócenia)
    val previousPlayer1Points: Int,
    val previousPlayer2Points: Int,
    val previousPlayer1Games: Int,
    val previousPlayer2Games: Int,
    val previousPlayer1Sets: Int,
    val previousPlayer2Sets: Int,
    val previousIsPlayer1Serving: Boolean,
    val previousIsFirstServe: Boolean,
    val previousIsTiebreak: Boolean,
    val previousIsSuperTiebreak: Boolean,
    val previousSetsHistorySize: Int,
    
    // Statystyki przed akcją
    val previousPlayer1Stats: MatchStatistics,
    val previousPlayer2Stats: MatchStatistics,
    
    // Opis akcji (do wyświetlenia)
    val description: String
) : Parcelable

enum class ActionType {
    ACE,
    FAULT,
    DOUBLE_FAULT,
    WINNER,
    FORCED_ERROR,
    UNFORCED_ERROR,
    BALL_IN_PLAY,
    GAME_WON,
    SET_WON
}
