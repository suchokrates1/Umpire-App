package pl.vestmedia.tennisreferee.ui.match

import android.app.Activity
import android.view.View
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.databinding.LayoutMatchFinishedBinding
import pl.vestmedia.tennisreferee.utils.AppLogger

class MatchFinishController(
    private val activity: Activity,
    private val binding: LayoutMatchFinishedBinding,
    private val onNextMatch: (String) -> Unit
) {
    fun render(state: MatchState) {
        val winner = state.finishWinnerName?.takeIf { it.isNotBlank() }
            ?: when {
                state.player1Sets > state.player2Sets -> state.player1.getDisplayName()
                state.player2Sets > state.player1Sets -> state.player2.getDisplayName()
                else -> null
            }

        if (winner.isNullOrBlank()) {
            binding.textWinner.visibility = View.GONE
        } else {
            binding.textWinner.visibility = View.VISIBLE
            binding.textWinner.text = activity.getString(winnerLabel(state, winner), winner)
        }
        renderStatistics(state)
        bindNextMatchButtons(state)
    }

    private fun winnerLabel(state: MatchState, winner: String): Int {
        val players = listOfNotNull(state.player1, state.player2, state.player3, state.player4)
        val matched = players.filter {
            it.getDisplayName() == winner || it.getFullName() == winner || it.name == winner
        }
        val female = matched.isNotEmpty() && matched.all {
            it.gender?.trim()?.equals("F", ignoreCase = true) == true
        }
        return if (female) R.string.winner_label_female else R.string.winner_label
    }

    private fun renderStatistics(state: MatchState) {
        binding.headerPlayer1Name.text = state.player1.getDisplayName()
        binding.headerPlayer2Name.text = state.player2.getDisplayName()

        binding.textAcesPlayer1.text = state.player1Stats.aces.toString()
        binding.textAcesPlayer2.text = state.player2Stats.aces.toString()

        binding.textDoubleFaultsPlayer1.text = state.player1Stats.doubleFaults.toString()
        binding.textDoubleFaultsPlayer2.text = state.player2Stats.doubleFaults.toString()

        binding.textWinnersPlayer1.text = state.player1Stats.winners.toString()
        binding.textWinnersPlayer2.text = state.player2Stats.winners.toString()

        binding.textUnforcedErrorsPlayer1.text = state.player1Stats.unforcedErrors.toString()
        binding.textUnforcedErrorsPlayer2.text = state.player2Stats.unforcedErrors.toString()

        binding.textFirstServePctPlayer1.text = formatPercentage(state.player1Stats.getFirstServePercentage())
        binding.textFirstServePctPlayer2.text = formatPercentage(state.player2Stats.getFirstServePercentage())
    }

    private fun bindNextMatchButtons(state: MatchState) {
        binding.buttonNextMatchSameSetup.setOnClickListener {
            AppLogger.button("Match", "NextMatchSameSetup", "court=${state.courtId}")
            AppLogger.navigate("Match", "PlayerSelection", "sameSetup=true")
            onNextMatch(MatchActivity.RESULT_NEXT_MATCH_SAME_SETUP)
        }

        binding.buttonNextMatchNewSetup.setOnClickListener {
            AppLogger.button("Match", "NextMatchNewSetup", "court=${state.courtId}")
            AppLogger.navigate("Match", "PlayerSelection", "sameSetup=false")
            onNextMatch(MatchActivity.RESULT_NEXT_MATCH_NEW_SETUP)
        }
    }

    private fun formatPercentage(value: Int): String {
        return activity.getString(R.string.percentage_value, value)
    }
}