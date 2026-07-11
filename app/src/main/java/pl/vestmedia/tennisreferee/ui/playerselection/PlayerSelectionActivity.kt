package pl.vestmedia.tennisreferee.ui.playerselection

import android.app.DatePickerDialog
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ActivityPlayerSelectionBinding
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.data.model.ScheduleSuggestion
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.utils.AppLogger
import pl.vestmedia.tennisreferee.ui.match.ActiveMatchStore
import pl.vestmedia.tennisreferee.ui.match.MatchActivity
import pl.vestmedia.tennisreferee.ui.tournamentselection.TournamentSelectionStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    private var currentSuggestion: ScheduleSuggestion? = null
    private var selectedScheduleId: Int? = null
    private var savedMatchConfig: MatchConfig? = null
    private var lastStartedMatchConfig: MatchConfig? = null
    private lateinit var activeMatchStore: ActiveMatchStore
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    private val matchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleMatchResult(result.resultCode, result.data)
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
        
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        
        // Załaduj zawodników
        viewModel.loadPlayers(courtId)
        viewModel.loadSuggestedMatch(courtId, selectedTournamentId)
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
            currentSuggestion = suggestion
            updateSuggestedMatchCard(suggestion)
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
            AppLogger.button("PlayerSelection", "Doubles", isChecked.toString())
            selectedScheduleId = null
            viewModel.setDoubles(isChecked)
        }

        binding.buttonUseSuggestedMatch.setOnClickListener {
            val suggestion = currentSuggestion ?: return@setOnClickListener
            selectedScheduleId = suggestion.id
            if (viewModel.applySuggestedMatch(suggestion)) {
                AppLogger.button("PlayerSelection", "UseSuggestedMatch", "schedule=${suggestion.id}")
                binding.cardSuggestedMatch.visibility = View.GONE
                Toast.makeText(this, getString(R.string.suggested_match_applied), Toast.LENGTH_SHORT).show()
            } else {
                selectedScheduleId = null
            }
        }

        binding.buttonManualPlayers.setOnClickListener {
            AppLogger.button("PlayerSelection", "ManualPlayersDespiteSuggestion")
            selectedScheduleId = null
            binding.cardSuggestedMatch.visibility = View.GONE
        }
        
        // Przycisk "+" obok pola wyszukiwania
        binding.buttonAddPlayerTop.setOnClickListener {
            AppLogger.button("PlayerSelection", "AddPlayer")
            selectedScheduleId = null
            showAddPlayerDialog()
        }
        
        // Przycisk dodawania przy braku wyników (zachowany dla kompatybilności)
        binding.buttonAddPlayer.setOnClickListener {
            selectedScheduleId = null
            showAddPlayerDialog()
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

    private fun handleMatchResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (MatchActivity.resultAction(data)) {
            MatchActivity.RESULT_NEXT_MATCH_SAME_SETUP -> prepareForNextMatch(reuseSetup = true)
            MatchActivity.RESULT_NEXT_MATCH_NEW_SETUP -> prepareForNextMatch(reuseSetup = false)
        }
    }

    private fun prepareForNextMatch(reuseSetup: Boolean) {
        selectedScheduleId = null
        currentSuggestion = null
        if (reuseSetup) {
            savedMatchConfig = lastStartedMatchConfig
        } else {
            savedMatchConfig = null
        }
        viewModel.clearSelection()
        binding.cardSuggestedMatch.visibility = View.GONE
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

    private fun updateSuggestedMatchCard(suggestion: ScheduleSuggestion?) {
        if (suggestion == null) {
            binding.cardSuggestedMatch.visibility = View.GONE
            return
        }

        binding.textSuggestedMatchPlayers.text = "${suggestion.player1Name} vs ${suggestion.player2Name}"
        binding.textSuggestedMatchMeta.text = listOf(
            suggestion.scheduledTime,
            suggestion.categoryName,
            suggestion.phase
        ).mapNotNull { value -> value?.takeIf { it.isNotBlank() } }
            .joinToString(" • ")
        binding.cardSuggestedMatch.visibility = View.VISIBLE
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
            startMatchWithConfig(selectedPlayers, config)
            return
        }
                // Pokaż dialog wyboru trybu statystyk
        AppLogger.dialog("MatchConfig", "show")
        showMatchConfigDialog(selectedPlayers)
    }
    
    private fun showMatchConfigDialog(selectedPlayers: List<Player>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_match_config, null)
        val dialogContent = dialogView.findViewById<View>(R.id.dialogContent)
        val editUmpireName = dialogView.findViewById<TextInputEditText>(R.id.editUmpireName)
        val layoutMixedDoubles = dialogView.findViewById<View>(R.id.layoutMixedDoubles)
        val textMixedStatus = dialogView.findViewById<android.widget.TextView>(R.id.textMixedStatus)
        val textMixedDoublesSummary = dialogView.findViewById<android.widget.TextView>(R.id.textMixedDoublesSummary)
        val textManualStartTime = dialogView.findViewById<android.widget.TextView>(R.id.textManualStartTime)
        val buttonSelectManualDateTime = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonSelectManualDateTime)
        val buttonClearManualDateTime = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonClearManualDateTime)
        val isDoublesMatch = viewModel.isDoubles.value == true
        val isMixedDoublesMatch = isDoublesMatch && isMixedDoublesSelection(selectedPlayers)
        var manualStartTime: Long? = null
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        applyBottomNavigationInset(dialogContent)
        
        // === Toggle groups ===
        val toggleGamesPerSet = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleGamesPerSet)
        val toggleSetsToWin = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleSetsToWin)
        val toggleTiebreakPoints = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleTiebreakPoints)
        val toggleSuperTiebreakPoints = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleSuperTiebreakPoints)
        val switchNoAdvantage = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchNoAdvantage)
        val switchTiebreakOnly = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchTiebreakOnly)
        val layoutMatchFormat = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutMatchFormat)
        val layoutTbOnlyPoints = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutTbOnlyPoints)
        val toggleTbOnlyPoints = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleTbOnlyPoints)
        layoutMixedDoubles.visibility = if (isDoublesMatch) View.VISIBLE else View.GONE
        if (isDoublesMatch) {
            textMixedStatus.text = if (isMixedDoublesMatch) getString(R.string.match_type_mixed) else getString(R.string.match_type_doubles)
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
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    TimePickerDialog(
                        this,
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
        
        // Defaults: 4 games/set, 2 sets to win, TB to 7, super TB to 10
        toggleGamesPerSet.check(R.id.btnGames4)
        toggleSetsToWin.check(R.id.btnSets2)
        toggleTiebreakPoints.check(R.id.btnTB7)
        toggleSuperTiebreakPoints.check(R.id.btnSTB10)
        toggleTbOnlyPoints.check(R.id.btnTbOnly10)
        
        // TB Only toggle: show/hide format controls
        switchTiebreakOnly.setOnCheckedChangeListener { _, isChecked ->
            layoutMatchFormat.visibility = if (isChecked) View.GONE else View.VISIBLE
            layoutTbOnlyPoints.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // Helper: build MatchConfig from current toggle state
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
                startMatchWithConfig(
                    selectedPlayers = selectedPlayers,
                    config = config,
                    umpireName = editUmpireName.text?.toString()?.trim().orEmpty(),
                    manualStartTime = manualStartTime
                )
            }
        
        dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAdvancedMode)
            .setOnClickListener {
                dialog.dismiss()
                val config = buildMatchConfig(StatsMode.ADVANCED)
                AppLogger.dialog("MatchConfig", "ADVANCED | $config")
                startMatchWithConfig(
                    selectedPlayers = selectedPlayers,
                    config = config,
                    umpireName = editUmpireName.text?.toString()?.trim().orEmpty(),
                    manualStartTime = manualStartTime
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
    
    private fun startMatchWithConfig(
        selectedPlayers: List<Player>,
        config: MatchConfig,
        umpireName: String = "",
        manualStartTime: Long? = null
    ) {
        val isDoublesMatch = viewModel.isDoubles.value ?: false
        val isMixedDoubles = isDoublesMatch && isMixedDoublesSelection(selectedPlayers)
        val playerNames = selectedPlayers.joinToString(", ") { it.getDisplayName() }
        lastStartedMatchConfig = config
        AppLogger.navigate(
            "PlayerSelection",
            "Match",
            "players=[$playerNames] doubles=$isDoublesMatch mixed=$isMixedDoubles schedule=${selectedScheduleId ?: "-"} umpire=${umpireName.ifBlank { "-" }} config=$config"
        )
        
        // Utwórz stan meczu
        val matchState = if (isDoublesMatch && selectedPlayers.size == 4) {
            // Debel - 4 graczy
            MatchState(
                player1 = selectedPlayers[0],
                player2 = selectedPlayers[2],
                player3 = selectedPlayers[1],
                player4 = selectedPlayers[3],
                courtId = courtId,
                courtName = courtName,
                scheduleId = selectedScheduleId,
                isDoubles = true,
                isMixedDoubles = isMixedDoubles,
                umpireName = umpireName.ifBlank { null },
                manualStartTime = manualStartTime,
                currentServer = 1,
                statsMode = config.statsMode,
                noAdvantage = config.noAdvantage,
                matchConfig = config
            )
        } else {
            // Singiel - 2 graczy
            MatchState(
                player1 = selectedPlayers[0],
                player2 = selectedPlayers[1],
                courtId = courtId,
                courtName = courtName,
                scheduleId = selectedScheduleId,
                isDoubles = false,
                umpireName = umpireName.ifBlank { null },
                manualStartTime = manualStartTime,
                statsMode = config.statsMode,
                noAdvantage = config.noAdvantage,
                matchConfig = config
            )
        }
        
        // Tryb TB Only - od razu rozpocznij super tiebreak
        if (config.tiebreakOnly) {
            matchState.isSuperTiebreak = true
        }
        
        activeMatchStore.save(matchState)
        matchLauncher.launch(MatchActivity.createIntent(this, matchState.clientMatchUuid, isDoublesMatch))
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
    
    private fun showAddPlayerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_player, null)
        val editFirstName = dialogView.findViewById<TextInputEditText>(R.id.editPlayerFirstName)
        val editLastName = dialogView.findViewById<TextInputEditText>(R.id.editPlayerLastName)
        val spinnerCountry = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCountry)
        val spinnerCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)
        
        // Wypełnij nazwisko z aktualnego wyszukiwania
        if (currentSearchQuery.contains(" ")) {
            val parts = currentSearchQuery.split(" ", limit = 2)
            editFirstName.setText(parts[0])
            editLastName.setText(parts[1])
        } else {
            editLastName.setText(currentSearchQuery)
        }
        
        // Setup dropdowns
        val countries = resources.getStringArray(R.array.countries)
        val countryCodes = resources.getStringArray(R.array.country_codes)
        val categories = resources.getStringArray(R.array.player_categories)
        
        val countryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countries)
        spinnerCountry.setAdapter(countryAdapter)
        spinnerCountry.setText(countries[0], false) // Default: Poland
        
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerCategory.setAdapter(categoryAdapter)
        spinnerCategory.setText(categories[0], false) // Default: Open
        
        AlertDialog.Builder(this)
            .setTitle(R.string.add_player)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val firstName = editFirstName.text.toString().trim()
                val lastName = editLastName.text.toString().trim()
                val selectedCountry = spinnerCountry.text.toString()
                val selectedCategory = spinnerCategory.text.toString()
                
                if (firstName.isEmpty()) {
                    Toast.makeText(this, getString(R.string.error_enter_first_name), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (lastName.isEmpty()) {
                    Toast.makeText(this, getString(R.string.error_enter_last_name), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Pobierz kod kraju z wybranej pozycji
                val countryIndex = countries.indexOf(selectedCountry)
                val flagCode = if (countryIndex >= 0) countryCodes[countryIndex] else "PL"
                
                // Dodaj zawodnika do serwera (automatycznie zaznaczy i przewinie)
                viewModel.addPlayer(firstName, lastName, flagCode, selectedCategory, courtId)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
