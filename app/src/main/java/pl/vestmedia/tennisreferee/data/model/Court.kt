package pl.vestmedia.tennisreferee.data.model

import android.content.Context
import com.google.gson.annotations.SerializedName
import pl.vestmedia.tennisreferee.R

/**
 * Model reprezentujący kort tenisowy
 */
data class Court(
    @SerializedName("kort_id")
    val id: String,
    
    @SerializedName("overlay_id")
    val overlayId: String? = null,
    
    @SerializedName("name")
    val name: String? = null,
    
    @SerializedName("is_available")
    val isAvailable: Boolean = true,
    
    @SerializedName("current_match_id")
    val currentMatchId: Int? = null
) {
    fun getDisplayName(context: Context): String {
        return name ?: context.getString(R.string.court_name, id)
    }
}
