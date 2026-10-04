package pl.vestmedia.tennisreferee.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PanicResponseDto(
    val ok: Boolean = false,
    @SerialName("thread_token")
    val threadToken: String? = null,
    val error: String? = null,
)

@Serializable
data class PanicLineDto(
    val direction: String = "",
    val text: String = "",
)

@Serializable
data class PanicThreadDto(
    val messages: List<PanicLineDto> = emptyList(),
    val error: String? = null,
)
