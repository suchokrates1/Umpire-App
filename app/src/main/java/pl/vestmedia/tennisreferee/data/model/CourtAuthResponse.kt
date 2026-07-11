package pl.vestmedia.tennisreferee.data.model

/**
 * Response z autoryzacji kortu przez PIN
 */
data class CourtAuthResponse(
    val ok: Boolean,
    
    val authorized: Boolean,
    
    val courtId: String? = null,

    val token: String? = null,

    val expiresAt: String? = null,

    val error: String? = null
)
