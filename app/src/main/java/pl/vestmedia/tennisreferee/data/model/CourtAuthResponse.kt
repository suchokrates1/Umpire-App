package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response z autoryzacji kortu przez PIN
 */
data class CourtAuthResponse(
    @SerializedName("ok")
    val ok: Boolean,
    
    @SerializedName("authorized")
    val authorized: Boolean,
    
    @SerializedName("kort_id")
    val courtId: String? = null,
    
    @SerializedName("error")
    val error: String? = null
)
