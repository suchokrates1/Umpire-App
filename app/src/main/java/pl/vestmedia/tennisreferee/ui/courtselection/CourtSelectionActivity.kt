package pl.vestmedia.tennisreferee.ui.courtselection

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ActivityCourtSelectionBinding
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.repository.TennisRepository
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.ui.tournamentselection.TournamentSelectionActivity
import pl.vestmedia.tennisreferee.ui.tournamentselection.TournamentSelectionStore
import pl.vestmedia.tennisreferee.ui.history.MatchHistoryActivity
import pl.vestmedia.tennisreferee.ui.settings.SettingsActivity
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.utils.AppLogger

/**
 * Activity do wyboru kortu
 */
class CourtSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCourtSelectionBinding
    private val viewModel: CourtSelectionViewModel by viewModels()
    private lateinit var adapter: CourtAdapter
    private val repository = TennisRepository()
    private val pinDialogController by lazy { CourtPinDialogController(this, repository) }
    private var selectedTournamentId: Int? = null
    private var selectedTournamentName: String? = null

    private val tournamentSelectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedTournamentId = TournamentSelectionActivity.selectedTournamentId(result.data)
                ?: TournamentSelectionStore.getSelectedTournamentIdForToday(this)
            selectedTournamentName = TournamentSelectionActivity.selectedTournamentName(result.data)
                ?: TournamentSelectionStore.getSelectedTournamentNameForToday(this)
            supportActionBar?.subtitle = selectedTournamentName
            viewModel.loadCourts(selectedTournamentId)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Sprawdź czy język został wybrany, jeśli nie - wróć do wyboru języka
        if (!LanguageSelectionActivity.hasLanguageSelected(this)) {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        selectedTournamentId = TournamentSelectionStore.getSelectedTournamentIdForToday(this)
        selectedTournamentName = TournamentSelectionStore.getSelectedTournamentNameForToday(this)
        if (selectedTournamentId == null) {
            startActivity(Intent(this, TournamentSelectionActivity::class.java))
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

        // Podnieś zawartość nad pasek nawigacyjny
        val rootPadding = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val navBar = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                rootPadding + navBar
            )
            windowInsets
        }

        AppLogger.screen("CourtSelection")
        (application as TennisRefereeApp).healthCheckManager.currentScreen = "CourtSelection"
        
        supportActionBar?.title = getString(R.string.select_court)
        supportActionBar?.subtitle = selectedTournamentName
        
        setupRecyclerView()
        setupObservers()
        setupListeners()
        
        // Załaduj korty
        viewModel.loadCourts(selectedTournamentId)
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
            AppLogger.button("CourtSelection", "Refresh")
            viewModel.loadCourts(selectedTournamentId)
        }
    }
    
    private fun onCourtSelected(court: Court) {
        AppLogger.button("CourtSelection", "CourtTap", "court=${court.id} name=${court.name}")
        pinDialogController.show(court)
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_court_selection, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                AppLogger.button("CourtSelection", "Menu:Settings")
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_match_history -> {
                AppLogger.button("CourtSelection", "Menu:History")
                val intent = Intent(this, MatchHistoryActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_change_tournament -> {
                AppLogger.button("CourtSelection", "Menu:ChangeTournament")
                tournamentSelectionLauncher.launch(
                    Intent(this, TournamentSelectionActivity::class.java).apply {
                        putExtra(TournamentSelectionActivity.EXTRA_FORCE_SELECTION, true)
                    }
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
