package pl.vestmedia.tennisreferee.ui.playerselection

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ActivityPlayerSelectionBinding
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.ui.match.MatchActivity

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
    }
    
    private var courtId: String = ""
    private var courtName: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Pobierz dane kortu z Intent
        courtId = intent.getStringExtra(EXTRA_COURT_ID) ?: ""
        courtName = intent.getStringExtra(EXTRA_COURT_NAME) ?: ""
        
        if (courtId.isEmpty()) {
            Toast.makeText(this, "Błąd: Brak danych kortu", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        
        // Załaduj zawodników
        viewModel.loadPlayers()
    }
    
    private fun setupUI() {
        binding.textCourtInfo.text = getString(R.string.court_label, courtName)
    }
    
    private fun setupRecyclerView() {
        adapter = PlayerAdapter(
            onPlayerClick = { player ->
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
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerViewPlayers.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerViewPlayers.visibility = View.VISIBLE
            }
        }
        
        viewModel.selectedPlayers.observe(this) { selectedPlayers ->
            updateSelectedPlayersInfo(selectedPlayers)
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
            viewModel.setDoubles(isChecked)
        }
        
        binding.buttonAddPlayer.setOnClickListener {
            showAddPlayerDialog()
        }
        
        binding.buttonNext.setOnClickListener {
            proceedToNextScreen()
        }
        
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
    
    private fun updateSelectedPlayersInfo(selectedPlayers: List<Player>) {
        val isDoubles = viewModel.isDoubles.value ?: false
        val requiredCount = if (isDoubles) 4 else 2
        val selectedCount = selectedPlayers.size
        
        binding.textSelectedInfo.text = getString(R.string.selected_info, selectedCount, requiredCount)
        
        // Pokaż listę wybranych graczy
        if (selectedPlayers.isNotEmpty()) {
            val names = selectedPlayers.joinToString(", ") { it.getDisplayName() }
            binding.textSelectedPlayers.text = names
            binding.textSelectedPlayers.visibility = View.VISIBLE
        } else {
            binding.textSelectedPlayers.visibility = View.GONE
        }
    }
    
    private fun updateRequiredPlayersText(isDoubles: Boolean) {
        binding.textGameType.text = if (isDoubles) {
            getString(R.string.game_type_doubles)
        } else {
            getString(R.string.game_type_singles)
        }
    }
    
    private fun proceedToNextScreen() {
        val selectedPlayers = viewModel.getSelectedPlayersList()
        
        if (selectedPlayers.size < 2) {
            Toast.makeText(this, "Wybierz odpowiednią liczbę graczy", Toast.LENGTH_SHORT).show()
            return
        }
        
        val isDoublesMatch = viewModel.isDoubles.value ?: false
        
        // Utwórz stan meczu
        val matchState = MatchState(
            player1 = selectedPlayers[0],
            player2 = selectedPlayers[1],
            courtId = courtId,
            courtName = courtName
        )
        
        // Przejdź do ekranu meczu
        val intent = Intent(this, MatchActivity::class.java).apply {
            putExtra(MatchActivity.EXTRA_MATCH_STATE, matchState)
            
            // Przekaż kolory drużyn dla debla
            if (isDoublesMatch) {
                putExtra(MatchActivity.EXTRA_IS_DOUBLES, true)
                putExtra(MatchActivity.EXTRA_TEAM1_COLOR, R.color.team1_color)
                putExtra(MatchActivity.EXTRA_TEAM2_COLOR, R.color.team2_color)
            }
        }
        startActivity(intent)
    }
    
    private fun filterPlayers(query: String) {
        val filteredPlayers = if (query.isEmpty()) {
            allPlayers
        } else {
            allPlayers.filter { player ->
                player.name.contains(query, ignoreCase = true)
            }
        }
        
        adapter.submitList(filteredPlayers)
        
        // Pokaż przycisk dodawania TYLKO gdy nie ma wyników wyszukiwania
        val hasNoResults = query.isNotEmpty() && filteredPlayers.isEmpty()
        binding.buttonAddPlayer.visibility = if (hasNoResults) View.VISIBLE else View.GONE
        
        // Aktualizuj widoczność innych elementów
        if (hasNoResults) {
            binding.emptyView.text = getString(R.string.player_not_found)
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerViewPlayers.visibility = View.GONE
        } else if (filteredPlayers.isEmpty() && query.isEmpty()) {
            // Brak zawodników w ogóle
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerViewPlayers.visibility = View.GONE
        } else {
            // Są wyniki
            binding.emptyView.visibility = View.GONE
            binding.recyclerViewPlayers.visibility = View.VISIBLE
        }
    }
    
    private fun showAddPlayerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_player, null)
        val editName = dialogView.findViewById<TextInputEditText>(R.id.editPlayerName)
        val editFlag = dialogView.findViewById<TextInputEditText>(R.id.editPlayerFlag)
        
        // Wypełnij nazwę z aktualnego wyszukiwania
        editName.setText(currentSearchQuery)
        
        AlertDialog.Builder(this)
            .setTitle(R.string.add_player)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = editName.text.toString().trim()
                val flagCode = editFlag.text.toString().trim().uppercase()
                
                if (name.isEmpty()) {
                    Toast.makeText(this, "Wprowadź nazwisko zawodnika", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (flagCode.isEmpty() || flagCode.length != 2) {
                    Toast.makeText(this, "Wprowadź prawidłowy kod flagi (2 litery)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                viewModel.addPlayer(courtId, name, flagCode)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
