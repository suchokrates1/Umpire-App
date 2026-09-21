package pl.vestmedia.tennisreferee.data.model

data class TournamentOption(
    val id: Int,

    val name: String,

    val city: String? = null,

    val country: String? = null,

    val location: String? = null,

    val startDate: String? = null,

    val endDate: String? = null,

    /** Play-review sandbox (server `is_simulation`): the only event an automated test may open. */
    val isSimulation: Boolean = false,
)