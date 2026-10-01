package pl.vestmedia.tennisreferee.ui.panic

import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.api.dto.PanicRequestDto
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider

object PanicPrompt {
    fun addTo(activity: AppCompatActivity, menu: Menu) {
        activity.menuInflater.inflate(R.menu.menu_panic, menu)
    }

    fun handle(activity: AppCompatActivity, item: MenuItem): Boolean {
        if (item.itemId != R.id.action_panic) return false
        show(activity)
        return true
    }

    fun show(activity: AppCompatActivity) {
        val note = EditText(activity).apply {
            hint = activity.getString(R.string.panic_note)
            minLines = 2
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.panic_title)
            .setMessage(R.string.panic_message)
            .setView(note)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.panic_send) { _, _ ->
                activity.lifecycleScope.launch {
                    val message = send(activity, note.text?.toString().orEmpty())
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private suspend fun send(activity: AppCompatActivity, note: String): String {
        val app = activity.application as TennisRefereeApp
        val courtId = CourtSessionProvider.get().current()?.courtId?.trim()?.takeIf { it.isNotEmpty() }
            ?: app.healthCheckManager.courtId?.trim()?.takeIf { it.isNotEmpty() }
        return try {
            val response = app.container.apiService.sendPanic(
                PanicRequestDto(courtId = courtId, note = note.trim().ifEmpty { null })
            )
            when (response.code()) {
                200 -> activity.getString(R.string.panic_sent)
                429 -> activity.getString(R.string.panic_cooldown)
                else -> activity.getString(R.string.panic_failed)
            }
        } catch (_: Exception) {
            activity.getString(R.string.panic_failed)
        }
    }
}
