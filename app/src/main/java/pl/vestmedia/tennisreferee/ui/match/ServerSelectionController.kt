package pl.vestmedia.tennisreferee.ui.match

import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player

object ServerSelectionController {
    enum class ButtonColorRole {
        Singles,
        Team1,
        Team2
    }

    data class ButtonState(
        val buttonIndex: Int,
        val serverNumber: Int,
        val label: String,
        val visible: Boolean,
        val selected: Boolean,
        val colorRole: ButtonColorRole
    )

    fun resolveServerNumber(buttonIndex: Int, state: MatchState): Int {
        return if (state.isDoubles) {
            resolveDoublesServerNumber(buttonIndex, state.sidesSwapped)
        } else {
            resolveSinglesServerNumber(buttonIndex, state.sidesSwapped)
        }
    }

    fun buildButtonStates(state: MatchState): List<ButtonState> {
        return if (state.isDoubles) {
            buildDoublesButtonStates(state)
        } else {
            buildSinglesButtonStates(state)
        }
    }

    private fun resolveSinglesServerNumber(buttonIndex: Int, sidesSwapped: Boolean): Int {
        return when (buttonIndex) {
            1 -> if (sidesSwapped) 2 else 1
            2 -> if (sidesSwapped) 1 else 2
            else -> 1
        }
    }

    private fun resolveDoublesServerNumber(buttonIndex: Int, sidesSwapped: Boolean): Int {
        return when (buttonIndex) {
            1 -> if (sidesSwapped) 2 else 1
            2 -> if (sidesSwapped) 1 else 2
            3 -> if (sidesSwapped) 4 else 3
            4 -> if (sidesSwapped) 3 else 4
            else -> 1
        }
    }

    private fun buildSinglesButtonStates(state: MatchState): List<ButtonState> {
        val leftServerNumber = if (state.sidesSwapped) 2 else 1
        val rightServerNumber = if (state.sidesSwapped) 1 else 2
        val leftPlayer = if (state.sidesSwapped) state.player2 else state.player1
        val rightPlayer = if (state.sidesSwapped) state.player1 else state.player2

        return listOf(
            buttonState(1, leftServerNumber, leftPlayer, state, isDoubles = false, ButtonColorRole.Singles),
            buttonState(2, rightServerNumber, rightPlayer, state, isDoubles = false, ButtonColorRole.Singles),
            hiddenButtonState(3),
            hiddenButtonState(4)
        )
    }

    private fun buildDoublesButtonStates(state: MatchState): List<ButtonState> {
        val leftTop = if (state.sidesSwapped) Pair(2, state.player2) else Pair(1, state.player1)
        val rightTop = if (state.sidesSwapped) Pair(1, state.player1) else Pair(2, state.player2)
        val leftBottom = if (state.sidesSwapped) {
            Pair(4, state.player4 ?: state.player2)
        } else {
            Pair(3, state.player3 ?: state.player1)
        }
        val rightBottom = if (state.sidesSwapped) {
            Pair(3, state.player3 ?: state.player1)
        } else {
            Pair(4, state.player4 ?: state.player2)
        }

        return listOf(
            doublesButtonState(1, leftTop.first, leftTop.second, state),
            doublesButtonState(2, rightTop.first, rightTop.second, state),
            doublesButtonState(3, leftBottom.first, leftBottom.second, state),
            doublesButtonState(4, rightBottom.first, rightBottom.second, state)
        )
    }

    private fun doublesButtonState(
        buttonIndex: Int,
        serverNumber: Int,
        player: Player,
        state: MatchState
    ): ButtonState {
        val colorRole = if (serverNumber == 1 || serverNumber == 3) {
            ButtonColorRole.Team1
        } else {
            ButtonColorRole.Team2
        }
        return buttonState(buttonIndex, serverNumber, player, state, isDoubles = true, colorRole)
    }

    private fun buttonState(
        buttonIndex: Int,
        serverNumber: Int,
        player: Player,
        state: MatchState,
        isDoubles: Boolean,
        colorRole: ButtonColorRole
    ): ButtonState {
        val selected = state.currentServer == serverNumber
        return ButtonState(
            buttonIndex = buttonIndex,
            serverNumber = serverNumber,
            label = playerLabel(player, selected, isDoubles),
            visible = true,
            selected = selected,
            colorRole = colorRole
        )
    }

    private fun hiddenButtonState(buttonIndex: Int): ButtonState {
        return ButtonState(
            buttonIndex = buttonIndex,
            serverNumber = buttonIndex,
            label = "",
            visible = false,
            selected = false,
            colorRole = ButtonColorRole.Singles
        )
    }

    private fun playerLabel(player: Player, selected: Boolean, isDoubles: Boolean): String {
        val prefix = if (selected) {
            if (isDoubles) "🎾 " else "• "
        } else {
            ""
        }
        val genderLabel = player.getGenderShortLabel()?.let { "$it " }.orEmpty()
        return "$prefix$genderLabel${player.getDisplayName()}"
    }
}