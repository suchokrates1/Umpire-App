package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

data class TournamentOption(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("city")
    val city: String? = null,

    @SerializedName("country")
    val country: String? = null,

    @SerializedName("location")
    val location: String? = null,

    @SerializedName("start_date")
    val startDate: String? = null,

    @SerializedName("end_date")
    val endDate: String? = null,

    @SerializedName("active")
    val active: Boolean = true,
)