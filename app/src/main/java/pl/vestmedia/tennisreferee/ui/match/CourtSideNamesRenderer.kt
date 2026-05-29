package pl.vestmedia.tennisreferee.ui.match

import android.content.Context
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.databinding.LayoutBasicScoringBinding
import pl.vestmedia.tennisreferee.databinding.LayoutRallyBinding
import pl.vestmedia.tennisreferee.databinding.LayoutServeBinding
import pl.vestmedia.tennisreferee.databinding.LayoutServerSelectionBinding

class CourtSideNamesRenderer(
    private val context: Context,
    private val serverSelectionBinding: LayoutServerSelectionBinding,
    private val serveBinding: LayoutServeBinding,
    private val rallyBinding: LayoutRallyBinding,
    private val basicScoringBinding: LayoutBasicScoringBinding
) {
    fun render(state: MatchState) {
        if (state.isDoubles) {
            renderDoublesNames(state)
        } else {
            renderSinglesNames(state)
        }
    }

    private fun renderDoublesNames(state: MatchState) {
        val leftTeamLabel = if (state.sidesSwapped) {
            state.getTeam2ServerAwareDisplayName()
        } else {
            state.getTeam1ServerAwareDisplayName()
        }
        val rightTeamLabel = if (state.sidesSwapped) {
            state.getTeam1ServerAwareDisplayName()
        } else {
            state.getTeam2ServerAwareDisplayName()
        }

        renderLeftRightNames(leftTeamLabel, rightTeamLabel)
    }

    private fun renderSinglesNames(state: MatchState) {
        val leftPlayer = if (state.sidesSwapped) state.player2 else state.player1
        val rightPlayer = if (state.sidesSwapped) state.player1 else state.player2

        serverSelectionBinding.buttonPlayer1Serves.text =
            context.getString(R.string.player_serves, state.player1.getDisplayName())
        serverSelectionBinding.buttonPlayer2Serves.text =
            context.getString(R.string.player_serves, state.player2.getDisplayName())

        renderLeftRightNames(leftPlayer.getDisplayName(), rightPlayer.getDisplayName())
    }

    private fun renderLeftRightNames(leftName: String, rightName: String) {
        serveBinding.textPlayerLeftName.text = leftName
        serveBinding.textPlayerRightName.text = rightName
        rallyBinding.textPlayerLeftName.text = leftName
        rallyBinding.textPlayerRightName.text = rightName
        basicScoringBinding.textPlayerLeftName.text = leftName
        basicScoringBinding.textPlayerRightName.text = rightName
    }
}