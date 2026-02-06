package pl.vestmedia.tennisreferee.ui.courtselection

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ActivityCourtSelectionBinding
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.repository.TennisRepository
import pl.vestmedia.tennisreferee.ui.playerselection.PlayerSelectionActivity
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.ui.history.MatchHistoryActivity
import pl.vestmedia.tennisreferee.ui.settings.SettingsActivity

/**
 * Activity do wyboru kortu
 */
class CourtSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCourtSelectionBinding
    private val viewModel: CourtSelectionViewModel by viewModels()
    private lateinit var adapter: CourtAdapter
    private val repository = TennisRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Sprawdź czy język został wybrany, jeśli nie - wróć do wyboru języka
        if (!LanguageSelectionActivity.hasLanguageSelected(this)) {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // Zastosuj wybrany język
        LanguageSelectionActivity.setLanguage(
            this,
            LanguageSelectionActivity.getSelectedLanguage(this)
        )
        
        binding = ActivityCourtSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.title = getString(R.string.select_court)
        
        setupRecyclerView()
        setupObservers()
        setupListeners()
        
        // Załaduj korty
        viewModel.loadCourts()
    }
    
    private fun setupRecyclerView() {
        adapter = CourtAdapter { court ->
            onCourtSelected(court)
        }
        
        binding.recyclerViewCourts.apply {
            layoutManager = GridLayoutManager(this@CourtSelectionActivity, 2)
            adapter = this@CourtSelectionActivity.adapter
        }
    }
    
    private fun setupObservers() {
        viewModel.courts.observe(this) { courts ->
            adapter.submitList(courts)
            
            // Pokaż odpowiedni widok
            if (courts.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerViewCourts.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerViewCourts.visibility = View.VISIBLE
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    private fun setupListeners() {
        binding.buttonRefresh.setOnClickListener {
            viewModel.loadCourts()
        }
    }
    
    private fun onCourtSelected(court: Court) {
        showPinDialog(court)
    }
    
    private fun showPinDialog(court: Court) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pin_input, null)
        val textMessage = dialogView.findViewById<TextView>(R.id.textPinMessage)
        val digit1 = dialogView.findViewById<EditText>(R.id.pinDigit1)
        val digit2 = dialogView.findViewById<EditText>(R.id.pinDigit2)
        val digit3 = dialogView.findViewById<EditText>(R.id.pinDigit3)
        val digit4 = dialogView.findViewById<EditText>(R.id.pinDigit4)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        
        textMessage.text = getString(R.string.court_pin_message, court.getDisplayName(this))
        
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.court_pin_title))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        
        // Funkcja do pobrania pełnego PIN
        fun getFullPin(): String = "${digit1.text}${digit2.text}${digit3.text}${digit4.text}"
        
        // Funkcja do automatycznego zatwierdzenia
        fun autoSubmitIfComplete() {
            val pin = getFullPin()
            if (pin.length == 4) {
                progressBar.visibility = View.VISIBLE
                digit1.isEnabled = false
                digit2.isEnabled = false
                digit3.isEnabled = false
                digit4.isEnabled = false
                
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        repository.verifyCourtPin(court.id, pin)
                    }
                    
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        
                        result.onSuccess { _ ->
                            dialog.dismiss()
                            val intent = Intent(this@CourtSelectionActivity, PlayerSelectionActivity::class.java).apply {
                                putExtra(PlayerSelectionActivity.EXTRA_COURT_ID, court.id)
                                putExtra(PlayerSelectionActivity.EXTRA_COURT_NAME, court.name)
                                putExtra(PlayerSelectionActivity.EXTRA_COURT_PIN, pin)
                            }
                            startActivity(intent)
                        }.onFailure { error ->
                            // Wyczyść pola i włącz ponownie
                            digit1.setText("")
                            digit2.setText("")
                            digit3.setText("")
                            digit4.setText("")
                            digit1.isEnabled = true
                            digit2.isEnabled = true
                            digit3.isEnabled = true
                            digit4.isEnabled = true
                            digit1.requestFocus()
                            
                            Toast.makeText(
                                this@CourtSelectionActivity,
                                getString(R.string.pin_invalid, error.message),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
        
        // TextWatcher do automatycznego przechodzenia między polami
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
    
    private fun verifyCourtPin(court: Court, pin: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.VISIBLE
            }
            
            val result = withContext(Dispatchers.IO) {
                repository.verifyCourtPin(court.id, pin)
            }
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                
                result.onSuccess { _ ->
                    // PIN poprawny - przejdź do wyboru zawodników
                    val intent = Intent(this@CourtSelectionActivity, PlayerSelectionActivity::class.java).apply {
                        putExtra(PlayerSelectionActivity.EXTRA_COURT_ID, court.id)
                        putExtra(PlayerSelectionActivity.EXTRA_COURT_NAME, court.name)
                        putExtra(PlayerSelectionActivity.EXTRA_COURT_PIN, pin)
                    }
                    startActivity(intent)
                }.onFailure { error ->
                    // PIN niepoprawny
                    Toast.makeText(
                        this@CourtSelectionActivity,
                        getString(R.string.pin_invalid, error.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_court_selection, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_match_history -> {
                val intent = Intent(this, MatchHistoryActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
