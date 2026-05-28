package pl.vestmedia.tennisreferee.ui.match

import android.app.Activity
import android.content.Intent
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.databinding.LayoutMatchFinishedBinding
import pl.vestmedia.tennisreferee.ui.playerselection.PlayerSelectionActivity
import pl.vestmedia.tennisreferee.utils.AppLogger

class MatchFinishController(
    private val activity: Activity,
    private val binding: LayoutMatchFinishedBinding
) {
    fun render(state: MatchState) {
        val winner = if (state.player1Sets > state.player2Sets) {
            state.player1.getDisplayName()
        } else {
            state.player2.getDisplayName()
        }

        binding.textWinner.text = activity.getString(R.string.winner_label, winner)
        renderStatistics(state)
        bindNextMatchButtons(state)
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
            val intent = Intent(activity, PlayerSelectionActivity::class.java).apply {
                putExtra(PlayerSelectionActivity.EXTRA_COURT_ID, state.courtId)
                putExtra(PlayerSelectionActivity.EXTRA_COURT_NAME, state.courtName)
                putExtra(PlayerSelectionActivity.EXTRA_MATCH_CONFIG, state.matchConfig)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            activity.startActivity(intent)
            activity.finish()
        }

        binding.buttonNextMatchNewSetup.setOnClickListener {
            AppLogger.button("Match", "NextMatchNewSetup", "court=${state.courtId}")
            AppLogger.navigate("Match", "PlayerSelection", "sameSetup=false")
            val intent = Intent(activity, PlayerSelectionActivity::class.java).apply {
                putExtra(PlayerSelectionActivity.EXTRA_COURT_ID, state.courtId)
                putExtra(PlayerSelectionActivity.EXTRA_COURT_NAME, state.courtName)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            activity.startActivity(intent)
            activity.finish()
        }
    }

    private fun formatPercentage(value: Int): String {
        return activity.getString(R.string.percentage_value, value)
    }
}