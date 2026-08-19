package pl.vestmedia.tennisreferee.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Model reprezentujący zawodnika tenisowego
 */
@Parcelize
data class Player(
    val id: Int,  // API v1 zwraca Int
    
    // Obsługuje różne formaty z API
    val name: String,

    val firstName: String = "",

    val lastName: String = "",

    val flag: String? = null,

    val flagUrl: String? = null,

    val group: String? = null,

    val gender: String? = null,

    val list: String? = null,

    val partner: Player? = null
) : Parcelable {
    /**
     * Zwraca nazwisko (do scoreboard w aplikacji)
     */
    fun getDisplayName(): String {
        return lastName.ifEmpty { name }
    }

    /**
     * Zwraca pełne imię i nazwisko (lista wyboru graczy)
     */
    fun getFullName(): String {
        val full = "$firstName $lastName".trim()
        return full.ifEmpty { name }
    }

    fun getGenderShortLabel(): String? {
        return when (gender?.trim()?.uppercase()) {
            "F" -> "K"
            "M" -> "M"
            else -> null
        }
    }
}
