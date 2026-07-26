package pl.vestmedia.tennisreferee.ui.courtselection

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.repository.TennisRepository
import pl.vestmedia.tennisreferee.ui.playerselection.PlayerSelectionActivity
import pl.vestmedia.tennisreferee.utils.AppLogger

/**
 * Court PIN dialog → authorize → PlayerSelection (extracted from CourtSelectionActivity).
 */
class CourtPinDialogController(
    private val activity: AppCompatActivity,
    private val repository: TennisRepository = TennisRepository()
) {
    fun show(court: Court) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_pin_input, null)
        val textMessage = dialogView.findViewById<TextView>(R.id.textPinMessage)
        val digit1 = dialogView.findViewById<EditText>(R.id.pinDigit1)
        val digit2 = dialogView.findViewById<EditText>(R.id.pinDigit2)
        val digit3 = dialogView.findViewById<EditText>(R.id.pinDigit3)
        val digit4 = dialogView.findViewById<EditText>(R.id.pinDigit4)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)

        textMessage.text = activity.getString(R.string.court_pin_message, court.getDisplayName(activity))

        val dialog = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.court_pin_title))
            .setView(dialogView)
            .setNegativeButton(activity.getString(R.string.cancel), null)
            .create()

        fun getFullPin(): String = "${digit1.text}${digit2.text}${digit3.text}${digit4.text}"

        fun autoSubmitIfComplete() {
            val pin = getFullPin()
            if (pin.length != 4) return
            progressBar.visibility = View.VISIBLE
            listOf(digit1, digit2, digit3, digit4).forEach { it.isEnabled = false }

            activity.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    repository.verifyCourtPin(court.id, pin)
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    result.onSuccess {
                        AppLogger.action("CourtSelection", "PIN_OK", "court=${court.id}")
                        (activity.application as TennisRefereeApp).healthCheckManager.courtId = court.id
                        dialog.dismiss()
                        val courtDisplayName = court.getDisplayName(activity)
                        val intent = Intent(activity, PlayerSelectionActivity::class.java).apply {
                            putExtra(PlayerSelectionActivity.EXTRA_COURT_ID, court.id)
                            putExtra(PlayerSelectionActivity.EXTRA_COURT_NAME, courtDisplayName)
                        }
                        AppLogger.navigate("CourtSelection", "PlayerSelection", "court=${court.id}")
                        activity.startActivity(intent)
                    }.onFailure { error ->
                        AppLogger.action("CourtSelection", "PIN_FAIL", "court=${court.id} error=${error.message}")
                        listOf(digit1, digit2, digit3, digit4).forEach {
                            it.setText("")
                            it.isEnabled = true
                        }
                        digit1.requestFocus()
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.pin_invalid, error.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        fun createDigitWatcher(nextDigit: EditText?, previousDigit: EditText?): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        nextDigit?.requestFocus() ?: autoSubmitIfComplete()
                    } else if (s?.isEmpty() == true && previousDigit != null) {
                        previousDigit.requestFocus()
                    }
                }
            }
        }

        digit1.addTextChangedListener(createDigitWatcher(digit2, null))
        digit2.addTextChangedListener(createDigitWatcher(digit3, digit1))
        digit3.addTextChangedListener(createDigitWatcher(digit4, digit2))
        digit4.addTextChangedListener(createDigitWatcher(null, digit3))

        dialog.show()
        digit1.requestFocus()
    }
}
