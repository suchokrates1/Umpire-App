package pl.vestmedia.tennisreferee.data.model

/**
 * Response z serwera zawierający listę zawodników
 * Serwer v1 zwraca: {"ok": true, "count": N, "players": [...]}
 */
data class PlayersResponse(
    val players: List<Player>,
    
    val totalCount: Int? = null,
    
    val ok: Boolean? = null
)
