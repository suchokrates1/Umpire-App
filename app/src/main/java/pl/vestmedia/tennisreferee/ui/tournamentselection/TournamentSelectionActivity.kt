package pl.vestmedia.tennisreferee.ui.tournamentselection

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.model.TournamentOption
import pl.vestmedia.tennisreferee.data.repository.TennisRepository
import pl.vestmedia.tennisreferee.databinding.ActivityTournamentSelectionBinding
import pl.vestmedia.tennisreferee.ui.courtselection.CourtSelectionActivity
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.utils.AppLogger

class TournamentSelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FORCE_SELECTION = "force_selection"
    }

    private lateinit var binding: ActivityTournamentSelectionBinding
    private lateinit var adapter: TournamentAdapter
    private val repository = TennisRepository()
    private var tournaments: List<TournamentOption> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!LanguageSelectionActivity.hasLanguageSelected(this)) {
            startActivity(Intent(this, LanguageSelectionActivity::class.java))
            finish()
            return
        }

        LanguageSelectionActivity.setLanguage(this, LanguageSelectionActivity.getSelectedLanguage(this))

        binding = ActivityTournamentSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rootPaddingBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val navBar = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, rootPaddingBottom + navBar)
            windowInsets
        }

        AppLogger.screen("TournamentSelection")
        supportActionBar?.title = getString(R.string.select_tournament)

        setupRecyclerView()
        binding.buttonRefresh.setOnClickListener {
            AppLogger.button("TournamentSelection", "Refresh")
            loadTournaments()
        }

        loadTournaments()
    }

    private fun setupRecyclerView() {
        adapter = TournamentAdapter(tournaments) { tournament ->
            onTournamentSelected(tournament)
        }
        binding.recyclerViewTournaments.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTournaments.adapter = adapter
    }

    private fun loadTournaments() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            binding.recyclerViewTournaments.visibility = View.GONE

            val result = repository.getActiveTournaments()
            binding.progressBar.visibility = View.GONE

            result.onSuccess { activeTournaments ->
                tournaments = activeTournaments.sortedBy { it.name.lowercase() }
                val forceSelection = intent.getBooleanExtra(EXTRA_FORCE_SELECTION, false)
                val selectedTournamentId = TournamentSelectionStore.getSelectedTournamentIdForToday(this@TournamentSelectionActivity)

                if (!forceSelection && selectedTournamentId != null && tournaments.any { it.id == selectedTournamentId }) {
                    navigateToCourts()
                    return@onSuccess
                }

                adapter = TournamentAdapter(tournaments) { tournament -> onTournamentSelected(tournament) }
                binding.recyclerViewTournaments.adapter = adapter
                binding.emptyView.visibility = if (tournaments.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerViewTournaments.visibility = if (tournaments.isEmpty()) View.GONE else View.VISIBLE
            }.onFailure { error ->
                AppLogger.error("TournamentSelection", error)
                Toast.makeText(this@TournamentSelectionActivity, getString(R.string.error_loading_tournaments), Toast.LENGTH_LONG).show()
                binding.emptyView.visibility = View.VISIBLE
                binding.emptyView.text = getString(R.string.no_tournaments_available)
            }
        }
    }

    private fun onTournamentSelected(tournament: TournamentOption) {
        AppLogger.button("TournamentSelection", "TournamentTap", "id=${tournament.id} name=${tournament.name}")
        TournamentSelectionStore.saveSelection(this, tournament)
        navigateToCourts()
    }

    private fun navigateToCourts() {
        AppLogger.navigate("TournamentSelection", "CourtSelection")
        startActivity(
            Intent(this, CourtSelectionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}