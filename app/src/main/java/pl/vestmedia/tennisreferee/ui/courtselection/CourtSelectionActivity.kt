package pl.vestmedia.tennisreferee.ui.courtselection

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
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
import pl.vestmedia.tennisreferee.ui.settings.SettingsActivity
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.utils.AppLogger

/**
 * Activity do wyboru kortu
 */
class CourtSelectionActivity : AppCompatActivity() {

    companion object {
        fun createTutorialIntent(context: android.content.Context): Intent {
            return Intent(context, CourtSelectionActivity::class.java).apply {
                putExtra(pl.vestmedia.tennisreferee.ui.tutorial.TutorialNavigator.EXTRA_TUTORIAL, true)
            }
        }
    }
    
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
        
        val tutorial = intent.getBooleanExtra(pl.vestmedia.tennisreferee.ui.tutorial.TutorialNavigator.EXTRA_TUTORIAL, false)
            || pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.isActive

        // Sprawdź czy język został wybrany, jeśli nie - wróć do wyboru języka
        if (!tutorial && !LanguageSelectionActivity.hasLanguageSelected(this)) {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        selectedTournamentId = TournamentSelectionStore.getSelectedTournamentIdForToday(this)
        selectedTournamentName = TournamentSelectionStore.getSelectedTournamentNameForToday(this)
        if (!tutorial && selectedTournamentId == null) {
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this) {
            if (tutorial) {
                pl.vestmedia.tennisreferee.ui.tutorial.TutorialNavigator.goBackScene(this@CourtSelectionActivity)
            } else {
                openTournamentSelection()
            }
        }
        
        setupRecyclerView()
        setupObservers()
        setupListeners()
        
        if (tutorial) {
            supportActionBar?.subtitle = getString(R.string.tutorial_demo_tournament)
            adapter.submitList(pl.vestmedia.tennisreferee.ui.tutorial.TutorialCatalog.courts())
            binding.emptyView.visibility = View.GONE
            binding.recyclerViewCourts.visibility = View.VISIBLE
            attachTutorialOverlay()
            val step = pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.currentStep(this)
            if (step?.scene == "pin") {
                pinDialogController.show(pl.vestmedia.tennisreferee.ui.tutorial.TutorialCatalog.courts().first())
            }
        } else {
            viewModel.loadCourts(selectedTournamentId)
            pl.vestmedia.tennisreferee.ui.tutorial.TutorialNavigator.maybeShowBanner(this)
        }
    }

    private var tutorialOverlay: pl.vestmedia.tennisreferee.ui.tutorial.TutorialOverlayController? = null

    private fun attachTutorialOverlay() {
        tutorialOverlay = pl.vestmedia.tennisreferee.ui.tutorial.TutorialOverlayController(
            activity = this,
            onBack = { pl.vestmedia.tennisreferee.ui.tutorial.TutorialNavigator.goBackScene(this) },
            onNext = {
                pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.goNext(this)
                val step = pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.currentStep(this)
                if (step?.scene == "pin") {
                    pinDialogController.show(pl.vestmedia.tennisreferee.ui.tutorial.TutorialCatalog.courts().first())
                    tutorialOverlay?.refresh()
                } else if (pl.vestmedia.tennisreferee.ui.tutorial.TutorialNavigator.applyStep(this)) {
                    finish()
                } else {
                    tutorialOverlay?.refresh()
                }
            },
            onSkip = { pl.vestmedia.tennisreferee.ui.tutorial.TutorialNavigator.exit(this) },
        )
        tutorialOverlay?.attach()
    }

    private fun openTournamentSelection() {
        AppLogger.button("CourtSelection", "Back:ChangeTournament")
        tournamentSelectionLauncher.launch(
            Intent(this, TournamentSelectionActivity::class.java).apply {
                putExtra(TournamentSelectionActivity.EXTRA_FORCE_SELECTION, true)
            }
        )
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
        if (pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.isActive) {
            if (court.id != pl.vestmedia.tennisreferee.ui.tutorial.TutorialCatalog.COURT_1) return
            pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.noteAction("selectCourt", this)
            if (pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.canAdvance(this)) {
                pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.goNext(this)
            }
            pinDialogController.show(court)
            tutorialOverlay?.refresh()
            return
        }
        pinDialogController.show(court)
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_court_selection, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.isActive) {
                    pl.vestmedia.tennisreferee.ui.tutorial.TutorialNavigator.goBackScene(this)
                } else {
                    openTournamentSelection()
                }
                true
            }
            R.id.action_settings -> {
                AppLogger.button("CourtSelection", "Menu:Settings")
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
