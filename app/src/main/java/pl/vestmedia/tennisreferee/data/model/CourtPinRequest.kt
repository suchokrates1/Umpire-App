package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request body dla weryfikacji PIN kortu
 */
data class CourtPinRequest(
    @SerializedName("pin")
    val pin: String
)
