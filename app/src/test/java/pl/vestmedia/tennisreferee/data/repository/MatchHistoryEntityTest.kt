package pl.vestmedia.tennisreferee.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

class MatchHistoryEntityTest {
    private val kowalski = Player(id = 11, name = "Jan Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val nowak = Player(id = 22, name = "Adam Nowak", firstName = "Adam", lastName = "Nowak")

    private fun state(courtId: String = "t32-3") =
        MatchState(player1 = kowalski, player2 = nowak, courtId = courtId, courtName = "3")

    @Test
    fun theCourtNumberIsReadFromATournamentCourtId() {
        assertEquals(3, state("t32-3").toEntity().courtId)
        assertEquals(12, state("t7-12").toEntity().courtId)
        assertEquals(4, state("4").toEntity().courtId)
        assertEquals(0, state("centre").toEntity().courtId)
    }

    @Test
    fun aPlayerWhoRetiresWhileLeadingDoesNotWinInTheHistory() {
        val retired = state().apply {
            player1Sets = 1
            player2Sets = 0
            finishReason = MatchFinishReason.RETIREMENT
            finishWinnerName = player2.getFullName()
        }

        assertEquals(nowak.id, retired.toEntity().winnerId)
    }

    @Test
    fun withoutANamedWinnerTheSetsDecide() {
        assertEquals(kowalski.id, state().apply { player1Sets = 2 }.toEntity().winnerId)
        assertEquals(nowak.id, state().apply { player2Sets = 2; player1Sets = 1 }.toEntity().winnerId)
        assertNull(state().apply { player1Sets = 1; player2Sets = 1 }.toEntity().winnerId)
    }
}
