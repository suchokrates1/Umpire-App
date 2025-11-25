package pl.vestmedia.tennisreferee.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Model reprezentujący szczegółowe statystyki gracza w meczu
 */
@Parcelize
data class MatchStatistics(
    var aces: Int = 0,
    var doubleFaults: Int = 0,
    var winners: Int = 0,
    var forcedErrors: Int = 0,
    var unforcedErrors: Int = 0,
    var firstServesIn: Int = 0,
    var firstServesTotal: Int = 0,
    var secondServesIn: Int = 0,
    var secondServesTotal: Int = 0
) : Parcelable {
    
    fun getFirstServePercentage(): Int {
        return if (firstServesTotal > 0) {
            ((firstServesIn.toFloat() / firstServesTotal) * 100).toInt()
        } else 0
    }
    
    fun getSecondServePercentage(): Int {
        return if (secondServesTotal > 0) {
            ((secondServesIn.toFloat() / secondServesTotal) * 100).toInt()
        } else 0
    }
    
    /**
     * Tworzy kopię statystyk
     */
    fun copy(): MatchStatistics {
        return MatchStatistics(
            aces = this.aces,
            doubleFaults = this.doubleFaults,
            winners = this.winners,
            forcedErrors = this.forcedErrors,
            unforcedErrors = this.unforcedErrors,
            firstServesIn = this.firstServesIn,
            firstServesTotal = this.firstServesTotal,
            secondServesIn = this.secondServesIn,
            secondServesTotal = this.secondServesTotal
        )
    }
}
