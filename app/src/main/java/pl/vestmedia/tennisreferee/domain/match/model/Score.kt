package pl.vestmedia.tennisreferee.domain.match.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SetScore(
    val setNumber: Int,
    
    val player1Games: Int,
    
    val player2Games: Int,
    
    val tiebreakLoserPoints: Int? = null,  // For display like 5-4(7) — stores loser's tiebreak points
    
    val isSuperTiebreak: Boolean = false  // True when this "set" is actually a super tiebreak (stores points, not games)
) : Parcelable
