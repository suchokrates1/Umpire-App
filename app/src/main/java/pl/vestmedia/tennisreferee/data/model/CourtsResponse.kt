package pl.vestmedia.tennisreferee.data.model

/**
 * Response z serwera zawierający listę kortów
 */
data class CourtsResponse(
    val courts: List<Court> = emptyList(),
    
    val totalCount: Int = 0
)
