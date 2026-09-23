package pl.vestmedia.tennisreferee.data.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.dto.CourtAuthResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.PlayerDto
import pl.vestmedia.tennisreferee.data.api.dto.PlayersResponseDto

/**
 * Gson still accepts the old field names. The written JSON keeps the primary name,
 * so a later serializer has a fixed shape to match.
 */
class SerializedNameAlternateTest {

    private val gson = Gson()

    @Test
    fun playerNameAcceptsSurnameAndFullName() {
        assertEquals("Kowalski", player("""{"id":1,"surname":"Kowalski"}""").name)
        assertEquals("Jan Kowalski", player("""{"id":1,"full_name":"Jan Kowalski"}""").name)
        assertEquals("Kowalski", player("""{"id":1,"name":"Kowalski"}""").name)
    }

    @Test
    fun playerFlagGroupAndFlagUrlAcceptLegacyKeys() {
        val parsed = player(
            """{"id":2,"name":"Nowak","country_code":"DE","flag_url":"https://flags/de.png","category":"B1"}"""
        )
        assertEquals("DE", parsed.flag)
        assertEquals("https://flags/de.png", parsed.flagUrl)
        assertEquals("B1", parsed.group)
        assertEquals(
            """{"id":2,"name":"Nowak","flag":"DE","flagUrl":"https://flags/de.png","group":"B1"}""",
            gson.toJson(parsed),
        )
    }

    @Test
    fun playersResponseAcceptsTotalCount() {
        val parsed = gson.fromJson(
            """{"players":[{"id":1,"name":"Kowalski"}],"total_count":1}""",
            PlayersResponseDto::class.java,
        )
        assertEquals(1, parsed.totalCount)
        assertEquals(
            """{"players":[{"id":1,"name":"Kowalski"}],"count":1}""",
            gson.toJson(parsed),
        )
    }

    @Test
    fun courtAuthAcceptsKortIdAndExpiryAliases() {
        listOf("expires_at", "expiresAt", "expiry").forEach { expiryKey ->
            val parsed = gson.fromJson(
                """{"ok":true,"authorized":true,"kort_id":"t2-1","token":"abc","$expiryKey":"2030-01-01T00:00:00Z"}""",
                CourtAuthResponseDto::class.java,
            )
            assertEquals("t2-1", parsed.courtId)
            assertEquals("2030-01-01T00:00:00Z", parsed.expiresAt)
        }
        val canonical = gson.fromJson(
            """{"ok":true,"authorized":true,"court_id":"t2-1","token":"abc","expires_at":"2030-01-01T00:00:00Z"}""",
            CourtAuthResponseDto::class.java,
        )
        assertEquals(
            """{"ok":true,"authorized":true,"court_id":"t2-1","token":"abc","expires_at":"2030-01-01T00:00:00Z"}""",
            gson.toJson(canonical),
        )
    }

    private fun player(json: String): PlayerDto = gson.fromJson(json, PlayerDto::class.java)
}
