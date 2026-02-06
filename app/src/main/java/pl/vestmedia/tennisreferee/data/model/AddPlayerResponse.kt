package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response z serwera przy dodawaniu gracza
 */
data class AddPlayerResponse(
    @SerializedName("ok")
    val ok: Boolean,
    
    @SerializedName("player")
    val player: Player?,
    
    @SerializedName("error")
    val error: String? = null
)
