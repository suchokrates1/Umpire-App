package pl.vestmedia.tennisreferee.ui.playerselection

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.model.ScheduleSuggestion
import pl.vestmedia.tennisreferee.databinding.ActivityPlayerSelectionBinding
import pl.vestmedia.tennisreferee.utils.AppLogger

/**
 * Controls schedule suggestion card: render, apply, and dismiss.
 */
class SuggestedMatchController(
    private val activity: AppCompatActivity,
    private val binding: ActivityPlayerSelectionBinding,
    private val applySuggestion: (ScheduleSuggestion) -> Boolean,
    private val onScheduleIdChanged: (Int?) -> Unit
) {
    private var currentSuggestion: ScheduleSuggestion? = null

    fun bind() {
        binding.buttonUseSuggestedMatch.setOnClickListener {
            val suggestion = currentSuggestion ?: return@setOnClickListener
            onScheduleIdChanged(suggestion.id)
            if (applySuggestion(suggestion)) {
                AppLogger.button("PlayerSelection", "UseSuggestedMatch", "schedule=${suggestion.id}")
                binding.cardSuggestedMatch.visibility = View.GONE
                Toast.makeText(
                    activity,
                    activity.getString(R.string.suggested_match_applied),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                onScheduleIdChanged(null)
            }
        }

        binding.buttonManualPlayers.setOnClickListener {
            AppLogger.button("PlayerSelection", "ManualPlayersDespiteSuggestion")
            onScheduleIdChanged(null)
            binding.cardSuggestedMatch.visibility = View.GONE
        }
    }

    fun render(suggestion: ScheduleSuggestion?) {
        currentSuggestion = suggestion
        if (suggestion == null) {
            binding.cardSuggestedMatch.visibility = View.GONE
            return
        }

        binding.textSuggestedMatchPlayers.text = "${suggestion.player1Name} vs ${suggestion.player2Name}"
        binding.textSuggestedMatchMeta.text = listOf(
            suggestion.scheduledTime,
            suggestion.categoryName,
            suggestion.phase
        ).mapNotNull { value -> value?.takeIf { it.isNotBlank() } }
            .joinToString(" • ")
        binding.cardSuggestedMatch.visibility = View.VISIBLE
    }

    fun clear() {
        currentSuggestion = null
        binding.cardSuggestedMatch.visibility = View.GONE
    }
}
