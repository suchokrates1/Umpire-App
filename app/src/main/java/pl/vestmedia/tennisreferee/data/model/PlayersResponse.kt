package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response z serwera zawierający listę zawodników
 */
data class PlayersResponse(
    @SerializedName("players")
    val players: List<Player>,
    
    @SerializedName("total_count")
    val totalCount: Int
)
