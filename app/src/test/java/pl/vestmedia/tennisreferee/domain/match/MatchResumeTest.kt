package pl.vestmedia.tennisreferee.domain.match

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

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
