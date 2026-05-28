package pl.vestmedia.tennisreferee.ui.match

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.databinding.LayoutServerSelectionBinding

class ServerSelectionViewBinder(
    private val context: Context,
    private val binding: LayoutServerSelectionBinding,
    private val getState: () -> MatchState?,
    private val onServerSelected: (Int) -> Unit,
    private val onSwapSides: () -> Unit,
    private val onButtonLogged: (String) -> Unit
) {
    fun bind() {
        bindServerButton(binding.buttonPlayer1Serves, 1, "Player1Serves")
        bindServerButton(binding.buttonPlayer2Serves, 2, "Player2Serves")
        bindServerButton(binding.buttonPlayer3Serves, 3, "Player3Serves")
        bindServerButton(binding.buttonPlayer4Serves, 4, "Player4Serves")

        binding.buttonSwapSides.setOnClickListener {
            onButtonLogged("SwapSides")
            onSwapSides()
        }
    }

    fun render(state: MatchState) {
        val buttons = listOf(
            binding.buttonPlayer1Serves,
            binding.buttonPlayer2Serves,
            binding.buttonPlayer3Serves,
            binding.buttonPlayer4Serves
        )

        ServerSelectionController.buildButtonStates(state).zip(buttons).forEach { (buttonState, button) ->
            renderButton(button, buttonState)
        }
    }

    private fun bindServerButton(button: MaterialButton, buttonIndex: Int, logName: String) {
        button.setOnClickListener {
            onButtonLogged(logName)
            getState()?.let { state ->
                onServerSelected(ServerSelectionController.resolveServerNumber(buttonIndex, state))
            }
        }
    }

    private fun renderButton(button: MaterialButton, buttonState: ServerSelectionController.ButtonState) {
        button.visibility = if (buttonState.visible) View.VISIBLE else View.GONE
        button.text = buttonState.label

        val colorRes = when (buttonState.colorRole) {
            ServerSelectionController.ButtonColorRole.Singles -> R.color.player_selected
            ServerSelectionController.ButtonColorRole.Team1 -> R.color.team1_color
            ServerSelectionController.ButtonColorRole.Team2 -> R.color.team2_color
        }
        val textColorRes = when (buttonState.colorRole) {
            ServerSelectionController.ButtonColorRole.Singles -> null
            ServerSelectionController.ButtonColorRole.Team1,
            ServerSelectionController.ButtonColorRole.Team2 -> R.color.team_button_text_color
        }

        button.setBackgroundColor(ContextCompat.getColor(context, colorRes))
        textColorRes?.let { button.setTextColor(ContextCompat.getColor(context, it)) }
        button.strokeWidth = if (buttonState.selected) 3 else 1
        button.strokeColor = ColorStateList.valueOf(
            ColorUtils.setAlphaComponent(Color.WHITE, if (buttonState.selected) 224 else 104)
        )
        button.alpha = if (buttonState.selected) 1.0f else 0.9f
    }
}