package pl.vestmedia.tennisreferee.ui.playerselection

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import pl.vestmedia.tennisreferee.R

/**
 * Shows the add-player dialog and forwards a valid result to the ViewModel.
 */
class AddPlayerDialogController(
    private val activity: AppCompatActivity,
    private val getSearchQuery: () -> String,
    private val getCourtId: () -> String,
    private val onAddPlayer: (
        firstName: String,
        lastName: String,
        flagCode: String,
        category: String,
        courtId: String
    ) -> Unit
) {
    fun show() {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_add_player, null)
        val editFirstName = dialogView.findViewById<TextInputEditText>(R.id.editPlayerFirstName)
        val editLastName = dialogView.findViewById<TextInputEditText>(R.id.editPlayerLastName)
        val spinnerCountry = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCountry)
        val spinnerCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)

        val currentSearchQuery = getSearchQuery()
        if (currentSearchQuery.contains(" ")) {
            val parts = currentSearchQuery.split(" ", limit = 2)
            editFirstName.setText(parts[0])
            editLastName.setText(parts[1])
        } else {
            editLastName.setText(currentSearchQuery)
        }

        val countries = activity.resources.getStringArray(R.array.countries)
        val countryCodes = activity.resources.getStringArray(R.array.country_codes)
        val categories = activity.resources.getStringArray(R.array.player_categories)

        val countryAdapter = ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, countries)
        spinnerCountry.setAdapter(countryAdapter)
        spinnerCountry.setText(countries[0], false)

        val categoryAdapter = ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerCategory.setAdapter(categoryAdapter)
        spinnerCategory.setText(categories[0], false)

        AlertDialog.Builder(activity)
            .setTitle(R.string.add_player)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val firstName = editFirstName.text.toString().trim()
                val lastName = editLastName.text.toString().trim()
                val selectedCountry = spinnerCountry.text.toString()
                val selectedCategory = spinnerCategory.text.toString()

                if (firstName.isEmpty()) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.error_enter_first_name),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (lastName.isEmpty()) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.error_enter_last_name),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val countryIndex = countries.indexOf(selectedCountry)
                val flagCode = if (countryIndex >= 0) countryCodes[countryIndex] else "PL"

                onAddPlayer(firstName, lastName, flagCode, selectedCategory, getCourtId())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
