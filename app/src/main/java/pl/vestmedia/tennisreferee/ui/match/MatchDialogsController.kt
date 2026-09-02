package pl.vestmedia.tennisreferee.ui.match

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.utils.AppLogger

class MatchDialogsController(
    private val activity: AppCompatActivity,
    private val onUndoConfirmed: () -> Unit,
    private val getMatchState: () -> MatchState?,
    private val onFinishConfirmed: (FinishMatchRequest) -> Unit,
    private val onExitConfirmed: () -> Unit,
    private val onBracketWarningCleared: () -> Unit
) {
    fun showExitConfirmation() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.confirm_exit_title)
            .setMessage(R.string.confirm_exit_message)
            .setPositiveButton(R.string.yes) { _, _ -> onExitConfirmed() }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    fun showBracketWarning(event: BracketWarningEvent) {
        val (title, message) = when (event.type) {
            "different_groups" -> Pair(
                activity.getString(R.string.bracket_warning_title),
                activity.getString(R.string.bracket_warning_different_groups)
            )
            "no_bracket" -> Pair(
                activity.getString(R.string.bracket_warning_title),
                activity.getString(R.string.bracket_warning_friendly)
            )
            else -> return
        }

        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ -> onBracketWarningCleared() }
            .setCancelable(false)
            .show()
    }

    fun showUndoConfirmation() {
        AppLogger.dialog("UndoConfirm", "show")
        AlertDialog.Builder(activity)
            .setTitle(R.string.undo)
            .setMessage(R.string.confirm_undo)
            .setPositiveButton(R.string.yes) { _, _ ->
                AppLogger.dialog("UndoConfirm", "YES")
                onUndoConfirmed()
            }
            .setNegativeButton(R.string.no) { _, _ ->
                AppLogger.dialog("UndoConfirm", "NO")
            }
            .show()
    }

    fun showFinishMatchConfirmation() {
        AppLogger.dialog("FinishMatchConfirm", "show")
        val state = getMatchState()
        if (state == null) {
            onExitConfirmed()
            return
        }
        val labels = arrayOf(
            activity.getString(R.string.finish_reason_normal),
            activity.getString(R.string.finish_reason_test),
            activity.getString(R.string.finish_reason_retirement),
            activity.getString(R.string.finish_reason_walkover)
        )
        AlertDialog.Builder(activity)
            .setTitle(R.string.finish_reason_prompt)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> confirmFinish(FinishMatchRequest(MatchFinishReason.NORMAL))
                    1 -> confirmFinish(FinishMatchRequest(MatchFinishReason.TEST))
                    2 -> showRetirementPlayerDialog(state)
                    3 -> showWalkoverWinnerDialog(state)
                }
            }
            .setNegativeButton(R.string.no) { _, _ ->
                AppLogger.dialog("FinishMatchConfirm", "NO")
            }
            .show()
    }

    private fun showRetirementPlayerDialog(state: MatchState) {
        val injuredOptions = arrayOf(state.getTeam1FullName(), state.getTeam2FullName())
        AlertDialog.Builder(activity)
            .setTitle(R.string.finish_retirement_prompt)
            .setItems(injuredOptions) { _, which ->
                val injuredPlayerName = injuredOptions[which]
                val winnerName = if (which == 0) state.getTeam2FullName() else state.getTeam1FullName()
                confirmFinish(
                    FinishMatchRequest(
                        finishReason = MatchFinishReason.RETIREMENT,
                        winnerName = winnerName,
                        injuredPlayerName = injuredPlayerName
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showWalkoverWinnerDialog(state: MatchState) {
        val winnerOptions = arrayOf(state.getTeam1FullName(), state.getTeam2FullName())
        AlertDialog.Builder(activity)
            .setTitle(R.string.finish_walkover_prompt)
            .setItems(winnerOptions) { _, which ->
                confirmFinish(
                    FinishMatchRequest(
                        finishReason = MatchFinishReason.WALKOVER,
                        winnerName = winnerOptions[which]
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmFinish(request: FinishMatchRequest) {
        AppLogger.dialog("FinishMatchConfirm", request.finishReason.name)
        onFinishConfirmed(request)
    }
}