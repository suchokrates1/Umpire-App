package pl.vestmedia.tennisreferee.ui.playerselection

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ActivityPlayerSelectionBinding
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.utils.AppLogger
import pl.vestmedia.tennisreferee.ui.match.ActiveMatchStore
import pl.vestmedia.tennisreferee.ui.tournamentselection.TournamentSelectionStore

/**
 * Activity do wyboru zawodników (singiel lub debel)
 */
class PlayerSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPlayerSelectionBinding
    private val viewModel: PlayerSelectionViewModel by viewModels()
    private lateinit var adapter: PlayerAdapter
    private var allPlayers: List<Player> = emptyList()
    private var currentSearchQuery: String = ""
    
    companion object {
        const val EXTRA_COURT_ID = "court_id"
        const val EXTRA_COURT_NAME = "court_name"
        const val EXTRA_MATCH_CONFIG = "match_config"
    }
    
    private var courtId: String = ""
    private var courtName: String = ""
    private var selectedTournamentId: Int? = null
    private var selectedScheduleId: Int? = null
    private var savedMatchConfig: MatchConfig? = null
    private lateinit var activeMatchStore: ActiveMatchStore

    private lateinit var suggestedMatchController: SuggestedMatchController
    private lateinit var addPlayerDialogController: AddPlayerDialogController
    private lateinit var nextMatchController: NextMatchController
    private lateinit var matchConfigDialogController: MatchConfigDialogController

    private val matchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        nextMatchController.handleMatchResult(result.resultCode, result.data)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activeMatchStore = ActiveMatchStore(this)

        // Podnieś przyciski nad pasek nawigacyjny
        val rootPaddingBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val navBar = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, rootPaddingBottom + navBar)
            windowInsets
        }

        AppLogger.screen("PlayerSelection")
        (application as TennisRefereeApp).healthCheckManager.currentScreen = "PlayerSelection"
        
        // Pobierz dane kortu z Intent
        courtId = intent.getStringExtra(EXTRA_COURT_ID) ?: ""
        courtName = intent.getStringExtra(EXTRA_COURT_NAME) ?: ""
        selectedTournamentId = TournamentSelectionStore.getSelectedTournamentIdForToday(this)
        
        // Sprawdź czy przekazano konfigurację z poprzedniego meczu
        @Suppress("DEPRECATION")
        savedMatchConfig = intent.getParcelableExtra(EXTRA_MATCH_CONFIG)
        
        if (courtId.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_court_data), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupControllers()
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        
        // Załaduj zawodników
        viewModel.loadPlayers(courtId)
        viewModel.loadSuggestedMatch(courtId, selectedTournamentId)
    }

    private fun setupControllers() {
        suggestedMatchController = SuggestedMatchController(
            activity = this,
            binding = binding,
            applySuggestion = { suggestion -> viewModel.applySuggestedMatch(suggestion) },
            onScheduleIdChanged = { selectedScheduleId = it }
        )

        addPlayerDialogController = AddPlayerDialogController(
            activity = this,
            getSearchQuery = { currentSearchQuery },
            getCourtId = { courtId },
            onAddPlayer = { firstName, lastName, flagCode, category, courtId ->
                viewModel.addPlayer(firstName, lastName, flagCode, category, courtId)
            }
        )

        nextMatchController = NextMatchController(
            activity = this,
            activeMatchStore = activeMatchStore,
            matchLauncher = matchLauncher,
            getCourtId = { courtId },
            getCourtName = { courtName },
            getSelectedScheduleId = { selectedScheduleId },
            getIsDoubles = { viewModel.isDoubles.value ?: false },
            getTeam1Name = { viewModel.appliedTeam1Name() },
            getTeam2Name = { viewModel.appliedTeam2Name() },
            isMixedDoublesSelection = { isMixedDoublesSelection(it) },
            onPrepareForNextMatch = { reuseSetup -> prepareForNextMatch(reuseSetup) }
        )

        matchConfigDialogController = MatchConfigDialogController(
            activity = this,
            getIsDoubles = { viewModel.isDoubles.value == true },
            isMixedDoublesSelection = { isMixedDoublesSelection(it) },
            buildDoublesTeamsPlainText = { buildDoublesTeamsPlainText(it) },
            onConfigChosen = { selectedPlayers, config, umpireName, manualStartTime ->
                nextMatchController.startMatchWithConfig(
                    selectedPlayers = selectedPlayers,
                    config = config,
                    umpireName = umpireName,
                    manualStartTime = manualStartTime
                )
            }
        )

        suggestedMatchController.bind()
    }
    
    private fun setupUI() {
        binding.textCourtInfo.text = getString(R.string.court_label, courtName)
    }
    
    private fun setupRecyclerView() {
        adapter = PlayerAdapter(
            onPlayerClick = { player ->
                selectedScheduleId = null
                viewModel.togglePlayerSelection(player)
            },
            isPlayerSelected = { player ->
                viewModel.isPlayerSelected(player)
            },
            getSelectionIndex = { player ->
                viewModel.getSelectionIndex(player)
            },
            isDoublesMode = {
                viewModel.isDoubles.value ?: false
            }
        )
        
        binding.recyclerViewPlayers.apply {
            layoutManager = LinearLayoutManager(this@PlayerSelectionActivity)
            adapter = this@PlayerSelectionActivity.adapter
        }
    }
    
    private fun setupObservers() {
        viewModel.players.observe(this) { players ->
            allPlayers = players
            filterPlayers(currentSearchQuery)
            
            // Pokaż odpowiedni widok
            if (players.isEmpty()) {
                binding.emptyViewContainer.visibility = View.VISIBLE
                binding.textAddPlayerHint.visibility = View.VISIBLE
                binding.recyclerViewPlayers.visibility = View.GONE
            } else {
                binding.emptyViewContainer.visibility = View.GONE
                binding.recyclerViewPlayers.visibility = View.VISIBLE
            }
        }
        
        viewModel.selectedPlayers.observe(this) { selectedPlayers ->
            updateSelectedPlayersInfo(selectedPlayers)
            updateRequiredPlayersText(viewModel.isDoubles.value ?: false)
            // Odswież adapter aby pokazać checkmarki
            adapter.notifyDataSetChanged()
            
            // Auto-przejście gdy wybrano wymaganą ilość graczy
            val requiredCount = if (viewModel.isDoubles.value == true) 4 else 2
            if (selectedPlayers.size == requiredCount) {
                // Opóźnienie 300ms dla lepszego UX
                binding.buttonNext.postDelayed({
                    proceedToNextScreen()
                }, 300)
            }
        }
        
        viewModel.isDoubles.observe(this) { isDoubles ->
            binding.checkboxDoubles.isChecked = isDoubles
            updateRequiredPlayersText(isDoubles)
        }

        viewModel.suggestedMatch.observe(this) { suggestion ->
            suggestedMatchController.render(suggestion)
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
        
        viewModel.canProceed.observe(this) { canProceed ->
            binding.buttonNext.isEnabled = canProceed
        }
        
        // Obserwuj nowo dodanego gracza i przewiń listę do niego
        viewModel.newlyAddedPlayer.observe(this) { newPlayer ->
            newPlayer?.let { player ->
                // Wyczyść wyszukiwanie żeby pokazać całą listę
                binding.editTextSearch.setText("")
                currentSearchQuery = ""
                filterPlayers("")
                
                // Przewiń do nowego gracza z opóźnieniem (żeby lista się odświeżyła)
                binding.recyclerViewPlayers.postDelayed({
                    val position = allPlayers.indexOf(player)
                    if (position >= 0) {
                        // Przewiń do gracza wyśrodkowując go
                        val layoutManager = binding.recyclerViewPlayers.layoutManager as LinearLayoutManager
                        layoutManager.scrollToPositionWithOffset(position, binding.recyclerViewPlayers.height / 3)
                    }
                    viewModel.clearNewlyAddedPlayer()
                }, 200)
            }
        }
    }
    
    private fun setupListeners() {
        // Wyszukiwanie zawodników
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString() ?: ""
                filterPlayers(currentSearchQuery)
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        binding.checkboxDoubles.setOnCheckedChangeListener { _, isChecked ->
            if ((viewModel.isDoubles.value == true) == isChecked) {
                return@setOnCheckedChangeListener
            }
            AppLogger.button("PlayerSelection", "Doubles", isChecked.toString())
            if (!viewModel.shouldKeepScheduleIdOnDoublesToggle(isChecked)) {
                selectedScheduleId = null
            }
            viewModel.setDoubles(isChecked)
        }
        
        // Przycisk "+" obok pola wyszukiwania
        binding.buttonAddPlayerTop.setOnClickListener {
            AppLogger.button("PlayerSelection", "AddPlayer")
            selectedScheduleId = null
            addPlayerDialogController.show()
        }
        
        // Przycisk dodawania przy braku wyników (zachowany dla kompatybilności)
        binding.buttonAddPlayer.setOnClickListener {
            selectedScheduleId = null
            addPlayerDialogController.show()
        }
        
        binding.buttonNext.setOnClickListener {
            AppLogger.button("PlayerSelection", "Next")
            proceedToNextScreen()
        }
        
        binding.buttonBack.setOnClickListener {
            AppLogger.button("PlayerSelection", "Back")
            finish()
        }
    }

    private fun prepareForNextMatch(reuseSetup: Boolean) {
        selectedScheduleId = null
        savedMatchConfig = nextMatchController.resolveSavedConfigForNextMatch(
            reuseSetup = reuseSetup,
            lastStarted = nextMatchController.lastStartedMatchConfig
        )
        viewModel.clearSelection()
        suggestedMatchController.clear()
        viewModel.loadSuggestedMatch(courtId, selectedTournamentId)
    }
    
    private fun updateSelectedPlayersInfo(selectedPlayers: List<Player>) {
        val isDoubles = viewModel.isDoubles.value ?: false
        val requiredCount = if (isDoubles) 4 else 2
        val selectedCount = selectedPlayers.size
        
        binding.textSelectedInfo.text = getString(R.string.selected_info, selectedCount, requiredCount)
        
        // Pokaż listę wybranych graczy
        if (selectedPlayers.isNotEmpty()) {
            binding.textSelectedPlayers.text = if (isDoubles) {
                buildDoublesSelectionText(selectedPlayers)
            } else {
                selectedPlayers.joinToString(", ") { formatPlayerSelectionLabel(it) }
            }
            binding.textSelectedPlayers.visibility = View.VISIBLE
        } else {
            binding.textSelectedPlayers.visibility = View.GONE
        }
    }
    
    private fun updateRequiredPlayersText(isDoubles: Boolean) {
        binding.textGameType.text = if (isDoubles) {
            buildString {
                append(getString(R.string.game_type_doubles))
                if (isMixedDoublesSelection(viewModel.getSelectedPlayersList())) {
                    append(" • ")
                    append(getString(R.string.match_type_mixed))
                }
            }
        } else {
            getString(R.string.game_type_singles)
        }
    }
    
    private fun proceedToNextScreen() {
        val selectedPlayers = viewModel.getSelectedPlayersList()
        
        if (selectedPlayers.size < 2) {
            Toast.makeText(this, getString(R.string.error_select_correct_players), Toast.LENGTH_SHORT).show()
            return
        }
        // Jeśli mamy konfigurację z poprzedniego meczu, pomiń dialog
        savedMatchConfig?.let { config ->
            AppLogger.action("PlayerSelection", "ReuseSavedConfig", config.toString())
            savedMatchConfig = null // Zużyj — następne użycie pokaże dialog
            nextMatchController.startMatchWithConfig(selectedPlayers, config)
            return
        }
        // Pokaż dialog wyboru trybu statystyk
        AppLogger.dialog("MatchConfig", "show")
        matchConfigDialogController.show(selectedPlayers)
    }

    private fun isMixedDoublesSelection(selectedPlayers: List<Player>): Boolean {
        if (selectedPlayers.size != 4) {
            return false
        }

        val team1Genders = selectedPlayers.take(2)
            .mapNotNull { normalizeGender(it.gender) }
            .toSet()
        val team2Genders = selectedPlayers.drop(2).take(2)
            .mapNotNull { normalizeGender(it.gender) }
            .toSet()

        return team1Genders.size == 2 && team2Genders.size == 2
    }

    private fun normalizeGender(gender: String?): String? {
        return gender?.trim()?.uppercase()?.takeIf { it == "M" || it == "F" }
    }

    private fun formatPlayerSelectionLabel(player: Player): String {
        val genderLabel = player.getGenderShortLabel()
        return if (genderLabel != null) "$genderLabel ${player.getFullName()}" else player.getFullName()
    }

    private fun buildDoublesTeamsPlainText(selectedPlayers: List<Player>): String {
        val lines = mutableListOf<String>()
        val team1 = selectedPlayers.take(2)
        val team2 = selectedPlayers.drop(2).take(2)

        if (team1.isNotEmpty()) {
            lines += "• ${team1.joinToString(" / ") { formatPlayerSelectionLabel(it) }}"
        }
        if (team2.isNotEmpty()) {
            lines += "• ${team2.joinToString(" / ") { formatPlayerSelectionLabel(it) }}"
        }

        return lines.joinToString("\n")
    }

    private fun buildDoublesSelectionText(selectedPlayers: List<Player>): CharSequence {
        val builder = android.text.SpannableStringBuilder()

        fun appendTeamLine(text: String, colorRes: Int) {
            if (builder.isNotEmpty()) {
                builder.append('\n')
            }
            val start = builder.length
            builder.append(text)
            builder.setSpan(
                android.text.style.ForegroundColorSpan(ContextCompat.getColor(this, colorRes)),
                start,
                builder.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start,
                builder.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val team1 = selectedPlayers.take(2)
        val team2 = selectedPlayers.drop(2).take(2)
        if (team1.isNotEmpty()) {
            appendTeamLine("• ${team1.joinToString(" / ") { formatPlayerSelectionLabel(it) }}", R.color.team1_color)
        }
        if (team2.isNotEmpty()) {
            appendTeamLine("• ${team2.joinToString(" / ") { formatPlayerSelectionLabel(it) }}", R.color.team2_color)
        }
        if (isMixedDoublesSelection(selectedPlayers)) {
            if (builder.isNotEmpty()) {
                builder.append('\n')
            }
            val start = builder.length
            builder.append(getString(R.string.match_type_mixed))
            builder.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start,
                builder.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return builder
    }
    
    private fun filterPlayers(query: String) {
        val filteredPlayers = if (query.isEmpty()) {
            allPlayers
        } else {
            allPlayers.filter { player ->
                player.getFullName().contains(query, ignoreCase = true)
            }
        }
        
        adapter.submitList(filteredPlayers)
        
        // Pokaż przycisk dodawania TYLKO gdy nie ma wyników wyszukiwania
        val hasNoResults = query.isNotEmpty() && filteredPlayers.isEmpty()
        binding.buttonAddPlayer.visibility = if (hasNoResults) View.VISIBLE else View.GONE
        
        // Aktualizuj widoczność innych elementów
        if (hasNoResults) {
            binding.emptyView.text = getString(R.string.player_not_found)
            binding.textAddPlayerHint.visibility = View.VISIBLE
            binding.emptyViewContainer.visibility = View.VISIBLE
            binding.recyclerViewPlayers.visibility = View.GONE
        } else if (filteredPlayers.isEmpty() && query.isEmpty()) {
            // Brak zawodników w ogóle
            binding.emptyView.text = getString(R.string.no_players_available)
            binding.textAddPlayerHint.visibility = View.VISIBLE
            binding.emptyViewContainer.visibility = View.VISIBLE
            binding.recyclerViewPlayers.visibility = View.GONE
        } else {
            // Są wyniki
            binding.emptyViewContainer.visibility = View.GONE
            binding.recyclerViewPlayers.visibility = View.VISIBLE
        }
    }
}
