package pl.vestmedia.tennisreferee.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Model reprezentujący zawodnika tenisowego
 */
@Parcelize
data class Player(
    @SerializedName("id")
    val id: Int,  // API v1 zwraca Int
    
    // Obsługuje różne formaty z API
    @SerializedName(value = "name", alternate = ["surname", "full_name"])
    val name: String,

    @SerializedName("first_name")
    val firstName: String = "",

    @SerializedName("last_name")
    val lastName: String = "",

    @SerializedName(value = "flag", alternate = ["country_code"])
    val flag: String? = null,

    @SerializedName(value = "flagUrl", alternate = ["flag_url"])
    val flagUrl: String? = null,

    @SerializedName(value = "group", alternate = ["category"])
    val group: String? = null,

    @SerializedName("gender")
    val gender: String? = null,

    @SerializedName("list")
    val list: String? = null
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
