package pl.vestmedia.tennisreferee.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.vestmedia.tennisreferee.R

class PhaseLabelsTest {

    private val resolve: (Int) -> String = { resId ->
        when (resId) {
            R.string.phase_group -> "Group stage"
            R.string.phase_group_rematch -> "Group stage — rematch"
            R.string.phase_knockout -> "Knockout stage"
            R.string.phase_semifinal -> "Semifinal"
            R.string.phase_final -> "Final"
            R.string.phase_third_place -> "3rd place match"
            else -> "unknown"
        }
    }

    @Test
    fun localizesGroupStage() {
        assertEquals("Group stage", PhaseLabels.localize("Grupowa", resolve))
    }

    @Test
    fun localizesGroupRematch() {
        assertEquals("Group stage — rematch", PhaseLabels.localize("Grupowa — Rewanż", resolve))
    }

    @Test
    fun localizesKnockout() {
        assertEquals("Knockout stage", PhaseLabels.localize("Pucharowa", resolve))
    }

    @Test
    fun localizesCompoundFinal() {
        assertEquals("B1 — Final", PhaseLabels.localize("B1 — Finał", resolve))
    }

    @Test
    fun blankReturnsNull() {
        assertNull(PhaseLabels.localize("  ", resolve))
        assertNull(PhaseLabels.localize(null, resolve))
    }
}
