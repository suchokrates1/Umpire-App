package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response z serwera zawierający listę zawodników
 * Serwer v1 zwraca: {"ok": true, "count": N, "players": [...]}
 */
data class PlayersResponse(
    @SerializedName("players")
    val players: List<Player>,
    
    @SerializedName(value = "count", alternate = ["total_count"])
    val totalCount: Int? = null,
    
    @SerializedName("ok")
    val ok: Boolean? = null
)
