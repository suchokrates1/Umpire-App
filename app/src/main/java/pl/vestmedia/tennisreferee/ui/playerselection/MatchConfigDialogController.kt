package pl.vestmedia.tennisreferee.ui.playerselection

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode
import pl.vestmedia.tennisreferee.utils.AppLogger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Match configuration dialog (stats mode, format, umpire, manual start time).
 */
class MatchConfigDialogController(
    private val activity: AppCompatActivity,
    private val getIsDoubles: () -> Boolean,
    private val isMixedDoublesSelection: (List<Player>) -> Boolean,
    private val buildDoublesTeamsPlainText: (List<Player>) -> String,
    private val onConfigChosen: (
        selectedPlayers: List<Player>,
        config: MatchConfig,
        umpireName: String,
        manualStartTime: Long?
    ) -> Unit
) {
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun show(selectedPlayers: List<Player>) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_match_config, null)
        val dialogContent = dialogView.findViewById<View>(R.id.dialogContent)
        val editUmpireName = dialogView.findViewById<TextInputEditText>(R.id.editUmpireName)
        val layoutMixedDoubles = dialogView.findViewById<View>(R.id.layoutMixedDoubles)
        val textMixedStatus = dialogView.findViewById<android.widget.TextView>(R.id.textMixedStatus)
        val textMixedDoublesSummary = dialogView.findViewById<android.widget.TextView>(R.id.textMixedDoublesSummary)
        val textManualStartTime = dialogView.findViewById<android.widget.TextView>(R.id.textManualStartTime)
        val buttonSelectManualDateTime =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonSelectManualDateTime)
        val buttonClearManualDateTime =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonClearManualDateTime)
        val isDoublesMatch = getIsDoubles()
        val isMixedDoublesMatch = isDoublesMatch && isMixedDoublesSelection(selectedPlayers)
        var manualStartTime: Long? = null

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        applyBottomNavigationInset(dialogContent)

        val toggleGamesPerSet =
            dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleGamesPerSet)
        val toggleSetsToWin =
            dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleSetsToWin)
        val toggleTiebreakPoints =
            dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleTiebreakPoints)
        val toggleSuperTiebreakPoints =
            dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleSuperTiebreakPoints)
        val switchNoAdvantage =
            dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchNoAdvantage)
        val switchTiebreakOnly =
            dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchTiebreakOnly)
        val layoutMatchFormat = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutMatchFormat)
        val layoutTbOnlyPoints = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutTbOnlyPoints)
        val toggleTbOnlyPoints =
            dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleTbOnlyPoints)
        layoutMixedDoubles.visibility = if (isDoublesMatch) View.VISIBLE else View.GONE
        if (isDoublesMatch) {
            textMixedStatus.text = if (isMixedDoublesMatch) {
                activity.getString(R.string.match_type_mixed)
            } else {
                activity.getString(R.string.match_type_doubles)
            }
            textMixedDoublesSummary.text = buildDoublesTeamsPlainText(selectedPlayers)
        }

        fun updateManualStartTimeLabel() {
            if (manualStartTime == null) {
                textManualStartTime.setText(R.string.match_config_manual_datetime_empty)
                buttonClearManualDateTime.visibility = View.GONE
            } else {
                textManualStartTime.text = dateTimeFormat.format(manualStartTime)
                buttonClearManualDateTime.visibility = View.VISIBLE
            }
        }

        buttonSelectManualDateTime.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                manualStartTime?.let { timeInMillis = it }
            }
            DatePickerDialog(
                activity,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    TimePickerDialog(
                        activity,
                        { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            manualStartTime = calendar.timeInMillis
                            updateManualStartTimeLabel()
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonClearManualDateTime.setOnClickListener {
            manualStartTime = null
            updateManualStartTimeLabel()
        }

        updateManualStartTimeLabel()

        toggleGamesPerSet.check(R.id.btnGames4)
        toggleSetsToWin.check(R.id.btnSets2)
        toggleTiebreakPoints.check(R.id.btnTB7)
        toggleSuperTiebreakPoints.check(R.id.btnSTB10)
        toggleTbOnlyPoints.check(R.id.btnTbOnly10)

        switchTiebreakOnly.setOnCheckedChangeListener { _, isChecked ->
            layoutMatchFormat.visibility = if (isChecked) View.GONE else View.VISIBLE
            layoutTbOnlyPoints.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        fun buildMatchConfig(statsMode: StatsMode): MatchConfig {
            if (switchTiebreakOnly.isChecked) {
                val tbPoints = when (toggleTbOnlyPoints.checkedButtonId) {
                    R.id.btnTbOnly7 -> 7
                    else -> 10
                }
                return MatchConfig(
                    setsToWin = 1,
                    superTiebreakPoints = tbPoints,
                    statsMode = statsMode,
                    noAdvantage = switchNoAdvantage.isChecked,
                    tiebreakOnly = true
                )
            }
            val gamesPerSet = when (toggleGamesPerSet.checkedButtonId) {
                R.id.btnGames3 -> 3
                R.id.btnGames5 -> 5
                R.id.btnGames6 -> 6
                else -> 4
            }
            val setsToWin = when (toggleSetsToWin.checkedButtonId) {
                R.id.btnSets1 -> 1
                R.id.btnSets3 -> 3
                else -> 2
            }
            val tiebreakPoints = when (toggleTiebreakPoints.checkedButtonId) {
                R.id.btnTB10 -> 10
                else -> 7
            }
            val superTiebreakPoints = when (toggleSuperTiebreakPoints.checkedButtonId) {
                R.id.btnSTB7 -> 7
                else -> 10
            }
            return MatchConfig(
                gamesPerSet = gamesPerSet,
                setsToWin = setsToWin,
                tiebreakPoints = tiebreakPoints,
                superTiebreakPoints = superTiebreakPoints,
                statsMode = statsMode,
                noAdvantage = switchNoAdvantage.isChecked
            )
        }

        dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardBasicMode)
            .setOnClickListener {
                dialog.dismiss()
                val config = buildMatchConfig(StatsMode.BASIC)
                AppLogger.dialog("MatchConfig", "BASIC | $config")
                onConfigChosen(
                    selectedPlayers,
                    config,
                    editUmpireName.text?.toString()?.trim().orEmpty(),
                    manualStartTime
                )
            }

        dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAdvancedMode)
            .setOnClickListener {
                dialog.dismiss()
                val config = buildMatchConfig(StatsMode.ADVANCED)
                AppLogger.dialog("MatchConfig", "ADVANCED | $config")
                onConfigChosen(
                    selectedPlayers,
                    config,
                    editUmpireName.text?.toString()?.trim().orEmpty(),
                    manualStartTime
                )
            }

        dialog.setOnShowListener {
            ViewCompat.requestApplyInsets(dialogContent)
        }

        dialog.show()
    }

    private fun applyBottomNavigationInset(contentView: View) {
        val baseLeftPadding = contentView.paddingLeft
        val baseTopPadding = contentView.paddingTop
        val baseRightPadding = contentView.paddingRight
        val baseBottomPadding = contentView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(
                baseLeftPadding,
                baseTopPadding,
                baseRightPadding,
                baseBottomPadding + navigationInsets
            )
            windowInsets
        }
    }
}
