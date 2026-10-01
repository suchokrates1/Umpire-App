package pl.vestmedia.tennisreferee.data.api.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PanicRequestDto(
    @SerializedName("court_id")
    @SerialName("court_id")
    val courtId: String? = null,
    @SerializedName("note")
    @SerialName("note")
    val note: String? = null,
)
