package pl.vestmedia.tennisreferee.data

import com.google.gson.Gson
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.ActionType
import pl.vestmedia.tennisreferee.domain.match.model.MatchAction
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.MatchStatistics
import pl.vestmedia.tennisreferee.domain.match.model.SetScore
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode

/**
 * The match a referee is in the middle of lives on their tablet. An update that could
 * not read it back would cost them the set, so the new writer has to read what the old
 * one wrote, field for field.
 */
class LocalStorageFormatTest {

    private val gson = Gson()

    private fun midMatchState() = MatchState(
        matchId = 8142,
        clientMatchUuid = "0c6f2f58-2d5f-4a1b-9d51-7a6d2d2a0001",
        player1 = Player(id = 7, name = "Ciborowski", firstName = "Mateusz", lastName = "Ciborowski", flag = "PL"),
        player2 = Player(id = 11, name = "Zgrzebska", firstName = "Dajana", lastName = "Zgrzebska", flag = "PL"),
        courtId = "t32-3",
        courtName = "Kort 3",
        scheduleId = 367,
        matchConfig = MatchConfig(gamesPerSet = 4, setsToWin = 2, statsMode = StatsMode.BASIC),
        player1Sets = 1,
        player2Sets = 0,
        player1Games = 2,
        player2Games = 3,
        player1Points = 2,
        player2Points = 1,
        setsHistory = mutableListOf(
            SetScore(setNumber = 1, player1Games = 4, player2Games = 2),
            SetScore(setNumber = 2, player1Games = 3, player2Games = 4, tiebreakLoserPoints = 5),
        ),
        isTiebreak = true,
        tiebreakOpeningServer = 2,
        matchStartTime = 1_759_000_000_000,
        player1Stats = MatchStatistics(aces = 2, doubleFaults = 1),
        statsMode = StatsMode.BASIC,
        actionsHistory = mutableListOf(
            MatchAction(
                timestamp = 1_759_000_123_456,
                actionType = ActionType.ACE,
                previousPlayer1Points = 1,
                previousPlayer2Points = 1,
                previousPlayer1Games = 2,
                previousPlayer2Games = 3,
                previousPlayer1Sets = 1,
                previousPlayer2Sets = 0,
                previousIsPlayer1Serving = true,
                previousIsFirstServe = true,
                previousIsTiebreak = true,
                previousIsSuperTiebreak = false,
                previousSetsHistorySize = 2,
                previousPlayer1Stats = MatchStatistics(aces = 1),
                previousPlayer2Stats = MatchStatistics(),
                description = "As",
            ),
        ),
        finishReason = MatchFinishReason.NORMAL,
    )

    @Test
    fun `a match saved by the Gson build is read back whole`() {
        val saved = gson.toJson(midMatchState())

        val restored = localJson.decodeFromString<MatchState>(saved)

        assertEquals(midMatchState(), restored)
    }

    @Test
    fun `what the new writer produces is read by both`() {
        val written = localJson.encodeToString(midMatchState())

        assertEquals(midMatchState(), localJson.decodeFromString<MatchState>(written))
        assertEquals(midMatchState(), gson.fromJson(written, MatchState::class.java))
    }

    @Test
    fun `the field names on disk are the ones the old build wrote`() {
        val written = localJson.encodeToString(midMatchState())

        for (name in listOf(
            "matchId", "clientMatchUuid", "player1", "player2", "courtId", "courtName",
            "player1Sets", "player2Games", "setsHistory", "setNumber", "tiebreakLoserPoints",
            "isTiebreak", "tiebreakOpeningServer", "actionsHistory", "actionType",
            "previousPlayer1Stats", "statsMode", "finishReason",
        )) {
            assertTrue("$name is missing from $written", written.contains("\"$name\""))
        }
        assertTrue("enums stay names, not numbers", written.contains("\"BASIC\"") && written.contains("\"ACE\""))
    }

    @Test
    fun `a row Room stored with Gson still decodes`() {
        val player = Player(id = 9, name = "Praškevičienė", firstName = "Indrė", lastName = "Praškevičienė", flag = "LT")
        val sets = listOf(SetScore(setNumber = 1, player1Games = 4, player2Games = 1, isSuperTiebreak = false))

        assertEquals(player, localJson.decodeFromString<Player>(gson.toJson(player)))
        assertEquals(sets, localJson.decodeFromString<List<SetScore>>(gson.toJson(sets)))
    }

    @Test
    fun `an unknown field from a newer build does not throw the match away`() {
        val saved = localJson.encodeToString(midMatchState())
            .replaceFirst("{", "{\"somethingAddedLater\":true,")

        assertNotNull(localJson.decodeFromString<MatchState>(saved))
    }

    @Test
    fun `a payload that is not a match at all comes back as nothing, not a crash`() {
        val strict = Json { ignoreUnknownKeys = true }

        val failed = runCatching { strict.decodeFromString<MatchState>("{\"nonsense\":1}") }

        assertTrue(failed.isFailure)
    }
}
