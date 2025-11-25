package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response z serwera zawierający listę kortów
 */
data class CourtsResponse(
    @SerializedName("courts")
    val courts: List<Court>,
    
    @SerializedName("total_count")
    val totalCount: Int
)
