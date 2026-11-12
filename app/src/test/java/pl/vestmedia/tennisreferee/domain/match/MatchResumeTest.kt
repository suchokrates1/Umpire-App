package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode

class MatchResumeTest {
    private val pietruszyńska = Player(id = 1, name = "Katarzyna Pietruszyńska", firstName = "Katarzyna", lastName = "Pietruszyńska")
    private val stopierzyńska = Player(id = 2, name = "Justyna Stopierzyńska", firstName = "Justyna", lastName = "Stopierzyńska")
    private val other = Player(id = 3, name = "Anna Bujak", firstName = "Anna", lastName = "Bujak")

    @Test
    fun samePairOnTheSameSlotResumesEvenWhenSidesAreSwapped() {
        val saved = MatchState(
            clientMatchUuid = "leave-and-back",
            player1 = pietruszyńska,
            player2 = stopierzyńska,
            courtId = "t32-1",
            courtName = "1",
            scheduleId = 1306,
        )

        assertTrue(
            MatchResume.canResume(
                saved,
                courtId = "t32-1",
                scheduleId = 1306,
                selectedPlayers = listOf(stopierzyńska, pietruszyńska),
                isDoubles = false,
            )
        )
    }

    @Test
    fun aFormatChosenBeforeTheFirstPointReplacesTheOneTheMatchWasOpenedWith() {
        val saved = MatchState(
            clientMatchUuid = "wrong-format",
            player1 = pietruszyńska,
            player2 = stopierzyńska,
            courtId = "t32-1",
            courtName = "1",
            matchConfig = MatchConfig(gamesPerSet = 4),
            statsMode = StatsMode.ADVANCED,
        )
        val chosen = MatchConfig(gamesPerSet = 6, noAdvantage = true, statsMode = StatsMode.BASIC)

        val reopened = MatchResume.withChosenConfig(saved, chosen)

        assertNotSame(saved, reopened)
        assertEquals("wrong-format", reopened.clientMatchUuid)
        assertEquals(chosen, reopened.matchConfig)
        assertEquals(StatsMode.BASIC, reopened.statsMode)
        assertTrue(reopened.noAdvantage)
        assertFalse(reopened.isSuperTiebreak)
    }

    @Test
    fun aTiebreakOnlyFormatChosenBeforeTheFirstPointStartsInTheSuperTiebreak() {
        val saved = MatchState(player1 = pietruszyńska, player2 = stopierzyńska, courtId = "t32-1", courtName = "1")

        val reopened = MatchResume.withChosenConfig(saved, MatchConfig(tiebreakOnly = true))

        assertTrue(reopened.isSuperTiebreak)
    }

    @Test
    fun onceAPointHasBeenPlayedTheFormatStays() {
        val saved = MatchState(
            player1 = pietruszyńska,
            player2 = stopierzyńska,
            courtId = "t32-1",
            courtName = "1",
            matchConfig = MatchConfig(gamesPerSet = 4),
        ).apply { player1Points = 1 }

        assertSame(saved, MatchResume.withChosenConfig(saved, MatchConfig(gamesPerSet = 6)))
        assertTrue(MatchResume.keepsPlayedFormat(saved, MatchConfig(gamesPerSet = 6)))
        assertFalse(MatchResume.keepsPlayedFormat(saved, saved.matchConfig))
    }

    @Test
    fun theSameFormatReopensTheSameState() {
        val saved = MatchState(player1 = pietruszyńska, player2 = stopierzyńska, courtId = "t32-1", courtName = "1")

        assertSame(saved, MatchResume.withChosenConfig(saved, saved.matchConfig))
    }

    @Test
    fun aDifferentPairOrAStartedSlotStartsFresh() {
        val saved = MatchState(
            player1 = pietruszyńska,
            player2 = stopierzyńska,
            courtId = "t32-1",
            courtName = "1",
            scheduleId = 1306,
        )

        assertFalse(
            MatchResume.canResume(saved, "t32-1", 1306, listOf(pietruszyńska, other), false)
        )
        assertFalse(
            MatchResume.canResume(saved, "t32-1", 1311, listOf(pietruszyńska, stopierzyńska), false)
        )
        saved.isMatchFinished = true
        assertFalse(
            MatchResume.canResume(saved, "t32-1", 1306, listOf(pietruszyńska, stopierzyńska), false)
        )
    }
}
