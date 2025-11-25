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
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("flag")
    val flag: String? = null,
    
    @SerializedName("flagUrl")
    val flagUrl: String? = null,
    
    @SerializedName("group")
    val group: String? = null,
    
    @SerializedName("list")
    val list: String? = null
) : Parcelable {
    /**
     * Zwraca pełne imię i nazwisko
     */
    fun getDisplayName(): String {
        return name
    }
}
