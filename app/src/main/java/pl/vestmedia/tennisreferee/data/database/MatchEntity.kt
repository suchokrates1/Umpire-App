package pl.vestmedia.tennisreferee.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

/**
 * Encja reprezentująca mecz w bazie danych
 */
@Entity(tableName = "matches")
@TypeConverters(Converters::class)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Informacje podstawowe
    val courtId: Int,
    val courtName: String,
    val matchStartTime: Long,
    val matchEndTime: Long?,
    val matchDuration: Long,
    
    // Gracze
    val player1: Player,
    val player2: Player,
    val player3: Player? = null,
    val player4: Player? = null,
    val isDoubles: Boolean = false,
    val isMixedDoubles: Boolean = false,
    val umpireName: String? = null,
    
    // Wynik końcowy
    val player1Sets: Int,
    val player2Sets: Int,
    val setsHistory: List<SetScore>,
    
    // Statystyki Player 1
    val player1Aces: Int,
    val player1DoubleFaults: Int,
    val player1Winners: Int,
    val player1ForcedErrors: Int,
    val player1UnforcedErrors: Int,
    val player1FirstServesIn: Int,
    val player1FirstServesTotal: Int,
    val player1SecondServesIn: Int,
    val player1SecondServesTotal: Int,
    
    // Statystyki Player 2
    val player2Aces: Int,
    val player2DoubleFaults: Int,
    val player2Winners: Int,
    val player2ForcedErrors: Int,
    val player2UnforcedErrors: Int,
    val player2FirstServesIn: Int,
    val player2FirstServesTotal: Int,
    val player2SecondServesIn: Int,
    val player2SecondServesTotal: Int,
    
    // Status
    val isCompleted: Boolean = true,
    val winnerId: Int?
) {
    fun getPlayer1SideDisplayName(): String {
        return if (isDoubles && player3 != null) {
            "${player1.getDisplayName()} / ${player3.getDisplayName()}"
        } else {
            player1.getDisplayName()
        }
    }

    fun getPlayer2SideDisplayName(): String {
        return if (isDoubles && player4 != null) {
            "${player2.getDisplayName()} / ${player4.getDisplayName()}"
        } else {
            player2.getDisplayName()
        }
    }

    fun getPlayer1SideFullName(): String {
        return if (isDoubles && player3 != null) {
            "${player1.getFullName()} / ${player3.getFullName()}"
        } else {
            player1.getFullName()
        }
    }

    fun getPlayer2SideFullName(): String {
        return if (isDoubles && player4 != null) {
            "${player2.getFullName()} / ${player4.getFullName()}"
        } else {
            player2.getFullName()
        }
    }

    fun getWinnerName(): String? {
        return when (winnerId) {
            player1.id -> getPlayer1SideDisplayName()
            player2.id -> getPlayer2SideDisplayName()
            else -> null
        }
    }
    
    fun getFormattedDuration(): String {
        val seconds = matchDuration / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return if (hours > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        } else {
            String.format(java.util.Locale.US, "%d:%02d", minutes, seconds % 60)
        }
    }
    
    fun getFirstServePercentage(isPlayer1: Boolean): Int {
        return if (isPlayer1) {
            if (player1FirstServesTotal > 0) {
                (player1FirstServesIn * 100) / player1FirstServesTotal
            } else 0
        } else {
            if (player2FirstServesTotal > 0) {
                (player2FirstServesIn * 100) / player2FirstServesTotal
            } else 0
        }
    }
}
