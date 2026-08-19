package pl.vestmedia.tennisreferee.utils

import android.content.Context
import pl.vestmedia.tennisreferee.R

/**
 * Maps canonical Polish backend phase labels to localized UI strings.
 * Group names like "B1 Group A" are left untouched by the caller.
 */
object PhaseLabels {

    fun localize(context: Context, rawPhase: String?): String? {
        return localize(rawPhase) { resId -> context.getString(resId) }
    }

    fun localize(rawPhase: String?, resolve: (Int) -> String): String? {
        val phase = rawPhase?.trim().orEmpty()
        if (phase.isEmpty()) return null

        val lower = phase.lowercase()

        // Canonical group / rematch labels first (avoid "Grupowa — Rewanż" → split mishap)
        if (isGroupRematch(lower)) return resolve(R.string.phase_group_rematch)
        if (isGroupStage(lower) && !lower.contains(" — ")) return resolve(R.string.phase_group)
        if (isKnockout(lower) && !lower.contains(" — ")) return resolve(R.string.phase_knockout)

        // "B1 — Finał" / "B1 — Półfinał" → keep category prefix, localize suffix
        val dashParts = phase.split(" — ", limit = 2)
        if (dashParts.size == 2) {
            val prefix = dashParts[0].trim()
            val suffix = localizeAtomic(dashParts[1].trim(), resolve)
            return if (prefix.isNotEmpty()) "$prefix — $suffix" else suffix
        }

        return localizeAtomic(phase, resolve, lower)
    }

    private fun localizeAtomic(
        phase: String,
        resolve: (Int) -> String,
        lower: String = phase.lowercase(),
    ): String {
        return when {
            isGroupRematch(lower) -> resolve(R.string.phase_group_rematch)
            isGroupStage(lower) -> resolve(R.string.phase_group)
            isThirdPlace(lower) -> resolve(R.string.phase_third_place)
            isSemifinal(lower) -> resolve(R.string.phase_semifinal)
            isFinal(lower) -> resolve(R.string.phase_final)
            isKnockout(lower) -> resolve(R.string.phase_knockout)
            else -> phase
        }
    }

    private fun isGroupRematch(lower: String): Boolean {
        return lower.contains("rewan")
            || lower.contains("rematch")
            || lower.contains("revanch")
            || lower.contains("rückspiel")
            || lower.contains("ritorno")
            || lower.contains("replay")
            || lower.contains("dogryw")
            || lower.contains("revanš")
    }

    private fun isGroupStage(lower: String): Boolean {
        if (isGroupRematch(lower)) return false
        return lower == "grupowa"
            || lower.contains("grupowa")
            || lower == "group"
            || lower.contains("group stage")
            || lower.contains("gruppenphase")
            || lower.contains("girone")
            || lower.contains("fase de grupos")
            || lower.contains("phase de groupes")
            || lower.contains("poule")
            || lower.contains("grupių")
    }

    private fun isKnockout(lower: String): Boolean {
        return lower == "pucharowa"
            || lower.contains("puchar")
            || lower.contains("knockout")
            || lower.contains("k.o")
            || lower.contains("eliminat")
            || lower.contains("atkrintam")
    }

    private fun isSemifinal(lower: String): Boolean {
        return lower.contains("półfina")
            || lower.contains("polfina")
            || lower.contains("semif")
            || lower.contains("halbfinale")
            || lower.contains("demi-finale")
            || lower.contains("pusfinal")
    }

    private fun isFinal(lower: String): Boolean {
        if (isSemifinal(lower) || isThirdPlace(lower)) return false
        return lower == "finał"
            || lower == "final"
            || lower.endsWith(" finał")
            || lower.endsWith(" final")
            || lower == "finalas"
            || lower.endsWith(" finalas")
            || (lower.contains("finale") && !lower.contains("semi"))
    }

    private fun isThirdPlace(lower: String): Boolean {
        return lower.contains("3. miejsce")
            || lower.contains("3rd")
            || lower.contains("third place")
            || lower.contains("bronze")
            || lower.contains("o 3")
            || lower.contains("3 viet")
    }
}
