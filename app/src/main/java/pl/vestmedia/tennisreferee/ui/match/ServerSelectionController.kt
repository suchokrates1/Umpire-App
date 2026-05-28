package pl.vestmedia.tennisreferee.ui.match

import pl.vestmedia.tennisreferee.data.model.MatchState

object ServerSelectionController {
    fun resolveServerNumber(buttonIndex: Int, state: MatchState): Int {
        return if (state.isDoubles) {
            resolveDoublesServerNumber(buttonIndex, state.sidesSwapped)
        } else {
            resolveSinglesServerNumber(buttonIndex, state.sidesSwapped)
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
}