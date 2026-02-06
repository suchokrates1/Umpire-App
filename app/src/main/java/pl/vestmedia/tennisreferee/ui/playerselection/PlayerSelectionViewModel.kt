package pl.vestmedia.tennisreferee.ui.playerselection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.repository.TennisRepository

/**
 * ViewModel zarządzający wyborem graczy
 */
class PlayerSelectionViewModel : ViewModel() {
    
    private val repository = TennisRepository()
    
    private val _players = MutableLiveData<List<Player>>()
    val players: LiveData<List<Player>> = _players
    
    private val _selectedPlayers = MutableLiveData<MutableList<Player>>(mutableListOf())
    val selectedPlayers: LiveData<MutableList<Player>> = _selectedPlayers
    
    private val _isDoubles = MutableLiveData<Boolean>(false)
    val isDoubles: LiveData<Boolean> = _isDoubles
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _canProceed = MutableLiveData<Boolean>(false)
    val canProceed: LiveData<Boolean> = _canProceed
    
    // Nowo dodany gracz do automatycznego zaznaczenia
    private val _newlyAddedPlayer = MutableLiveData<Player?>()
    val newlyAddedPlayer: LiveData<Player?> = _newlyAddedPlayer
    
    /**
     * Ładuje listę zawodników z serwera
     */
    fun loadPlayers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.getPlayers()
            result.onSuccess { playersList ->
                _players.value = playersList
                _isLoading.value = false
            }.onFailure { exception ->
                _error.value = exception.message ?: "Nieznany błąd"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Zmienia tryb gry (singiel/debel)
     */
    fun setDoubles(isDoubles: Boolean) {
        _isDoubles.value = isDoubles
        // Wyczyść wybór jeśli zmieniono tryb
        _selectedPlayers.value = mutableListOf()
        updateCanProceed()
    }
    
    /**
     * Dodaje lub usuwa gracza z listy wybranych
     */
    fun togglePlayerSelection(player: Player) {
        val currentSelected = _selectedPlayers.value ?: mutableListOf()
        val maxPlayers = if (_isDoubles.value == true) 4 else 2
        
        if (currentSelected.contains(player)) {
            // Usuń gracza
            currentSelected.remove(player)
        } else {
            // Dodaj gracza jeśli nie osiągnięto limitu
            if (currentSelected.size < maxPlayers) {
                currentSelected.add(player)
            }
        }
        
        _selectedPlayers.value = currentSelected
        updateCanProceed()
    }
    
    /**
     * Sprawdza czy wybrano odpowiednią liczbę graczy
     */
    private fun updateCanProceed() {
        val selectedCount = _selectedPlayers.value?.size ?: 0
        val requiredCount = if (_isDoubles.value == true) 4 else 2
        _canProceed.value = selectedCount == requiredCount
    }
    
    /**
     * Sprawdza czy gracz jest wybrany
     */
    fun isPlayerSelected(player: Player): Boolean {
        return _selectedPlayers.value?.contains(player) == true
    }
    
    /**
     * Zwraca indeks wybranego gracza (0-3 dla debla, 0-1 dla singla)
     */
    fun getSelectionIndex(player: Player): Int {
        return _selectedPlayers.value?.indexOf(player) ?: -1
    }
    
    /**
     * Zwraca wybranych graczy jako listę
     */
    fun getSelectedPlayersList(): List<Player> {
        return _selectedPlayers.value ?: emptyList()
    }
    
    /**
     * Czyści błąd
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Czyści informację o nowo dodanym graczu
     */
    fun clearNewlyAddedPlayer() {
        _newlyAddedPlayer.value = null
    }
    
    /**
     * Dodaje nowego zawodnika do API
     * Po dodaniu automatycznie zaznacza go do meczu
     */
    fun addPlayer(name: String, flagCode: String, category: String = "B1", courtId: String = "", courtPin: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.addPlayer(name, flagCode, category, courtId, courtPin)
            result.onSuccess { newPlayer ->
                // Odśwież listę zawodników
                val playersResult = repository.getPlayers()
                playersResult.onSuccess { playersList ->
                    _players.value = playersList
                    
                    // Znajdź nowego gracza na liście (po ID lub nazwie)
                    val addedPlayer = playersList.find { it.id == newPlayer.id } 
                        ?: playersList.find { it.name == newPlayer.name }
                    
                    if (addedPlayer != null) {
                        // Automatycznie zaznacz nowego gracza
                        togglePlayerSelection(addedPlayer)
                        // Ustaw nowo dodanego gracza do przewinięcia listy
                        _newlyAddedPlayer.value = addedPlayer
                    }
                    
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message ?: "Błąd pobierania listy"
                    _isLoading.value = false
                }
            }.onFailure { exception ->
                _error.value = exception.message ?: "Błąd dodawania zawodnika"
                _isLoading.value = false
            }
        }
    }
}
