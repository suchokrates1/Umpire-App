package pl.vestmedia.tennisreferee.data.model

/**
 * Response z serwera przy dodawaniu gracza
 */
data class AddPlayerResponse(
    val ok: Boolean,
    
    val player: Player?,
    
    val error: String? = null
)
