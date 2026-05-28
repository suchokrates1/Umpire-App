package pl.vestmedia.tennisreferee.ui.match

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.utils.AppLogger

class MatchDialogsController(
    private val activity: AppCompatActivity,
    private val onUndoConfirmed: () -> Unit,
    private val onFinishConfirmed: () -> Unit,
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
        AlertDialog.Builder(activity)
            .setTitle(R.string.finish_match)
            .setMessage(R.string.confirm_finish_match)
            .setPositiveButton(R.string.yes) { _, _ ->
                AppLogger.dialog("FinishMatchConfirm", "YES")
                onFinishConfirmed()
            }
            .setNegativeButton(R.string.no) { _, _ ->
                AppLogger.dialog("FinishMatchConfirm", "NO")
            }
            .show()
    }
}