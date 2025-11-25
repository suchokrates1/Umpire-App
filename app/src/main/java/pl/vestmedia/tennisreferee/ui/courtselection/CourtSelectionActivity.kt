package pl.vestmedia.tennisreferee.ui.courtselection

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
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
        val pinInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = getString(R.string.pin_hint)
            setPadding(50, 20, 50, 20)
        }
        
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.court_pin_title))
            .setMessage(getString(R.string.court_pin_message, court.getDisplayName(this)))
            .setView(pinInput)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val pin = pinInput.text.toString()
                if (pin.isNotEmpty()) {
                    verifyCourtPin(court, pin)
                } else {
                    Toast.makeText(this, getString(R.string.pin_empty), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        
        dialog.show()
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
                
                result.onSuccess { authResponse ->
                    // PIN poprawny - przejdź do wyboru zawodników
                    val intent = Intent(this@CourtSelectionActivity, PlayerSelectionActivity::class.java).apply {
                        putExtra(PlayerSelectionActivity.EXTRA_COURT_ID, court.id)
                        putExtra(PlayerSelectionActivity.EXTRA_COURT_NAME, court.name)
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
}
