package pl.vestmedia.tennisreferee.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PanicRequestDto(
    @SerialName("court_id")
    val courtId: String? = null,
    @SerialName("note")
    val note: String? = null,
)
