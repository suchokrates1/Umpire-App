package pl.vestmedia.tennisreferee.e2e

import pl.vestmedia.tennisreferee.data.model.Player

data class TournamentFixture(
    val marker: String,
    val tournamentId: Int,
    val players: List<E2EPlayer>
) {
    var courtOrdinal: Int = 0
        private set

    fun nextCourtId(): String {
        courtOrdinal += 1
        return "t$tournamentId-$courtOrdinal"
    }

    /** Fixed court slot for parallel multi-device runs (0-based index). */
    fun courtIdFor(index: Int): String {
        require(index >= 0) { "court index must be >= 0" }
        val ordinal = index + 1
        if (ordinal > courtOrdinal) {
            courtOrdinal = ordinal
        }
        return "t$tournamentId-$ordinal"
    }

    fun courtNameFor(index: Int): String = "E2E Court ${index + 1}"
}

data class E2EPlayer(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val country: String
) {
    val fullName: String = "$firstName $lastName"

    fun toPlayer(): Player = Player(
        id = id,
        name = fullName,
        firstName = firstName,
        lastName = lastName,
        flag = country,
        group = "E2E",
        gender = gender
    )
}

data class FinishedArtifacts(
    val match: org.json.JSONObject,
    val history: org.json.JSONObject,
    val statistics: org.json.JSONObject
)
