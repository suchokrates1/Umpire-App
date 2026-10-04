package pl.vestmedia.tennisreferee.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.dto.CourtAuthResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.PlayerDto
import pl.vestmedia.tennisreferee.data.api.dto.PlayersResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.apiJson

/**
 * Older servers name some fields differently. The app still reads those names, and
 * writes only the current one, so what it sends has one fixed shape.
 */
class ApiAlternateNamesTest {

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
        val written = apiJson.encodeToString(PlayerDto.serializer(), parsed)
        listOf("\"flag\":\"DE\"", "\"flagUrl\":", "\"group\":\"B1\"").forEach { assertTrue(written, it in written) }
        listOf("country_code", "flag_url", "category").forEach { assertFalse(written, "\"$it\"" in written) }
    }

    @Test
    fun playersResponseAcceptsTotalCount() {
        val parsed = apiJson.decodeFromString(
            PlayersResponseDto.serializer(),
            """{"players":[{"id":1,"name":"Kowalski"}],"total_count":1}""",
        )
        assertEquals(1, parsed.totalCount)
        val written = apiJson.encodeToString(PlayersResponseDto.serializer(), parsed)
        assertTrue(written, "\"count\":1" in written && "total_count" !in written)
    }

    @Test
    fun courtAuthAcceptsKortIdAndExpiryAliases() {
        listOf("expires_at", "expiresAt", "expiry").forEach { expiryKey ->
            val parsed = apiJson.decodeFromString(
                CourtAuthResponseDto.serializer(),
                """{"ok":true,"authorized":true,"kort_id":"t2-1","token":"abc","$expiryKey":"2030-01-01T00:00:00Z"}""",
            )
            assertEquals("t2-1", parsed.courtId)
            assertEquals("2030-01-01T00:00:00Z", parsed.expiresAt)
        }
        val canonical = """{"ok":true,"authorized":true,"court_id":"t2-1","token":"abc","expires_at":"2030-01-01T00:00:00Z"}"""
        assertEquals(
            canonical,
            apiJson.encodeToString(
                CourtAuthResponseDto.serializer(),
                apiJson.decodeFromString(CourtAuthResponseDto.serializer(), canonical),
            ),
        )
    }

    @Test
    fun unknownKeysAreIgnored() {
        assertEquals("Kowalski", player("""{"id":1,"name":"Kowalski","not_a_field":true}""").name)
    }

    private fun player(json: String): PlayerDto = apiJson.decodeFromString(PlayerDto.serializer(), json)
}
