package pl.vestmedia.tennisreferee.ui.courtselection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.repository.TennisRepository

/**
 * ViewModel dla ekranu wyboru kortu
 */
class CourtSelectionViewModel : ViewModel() {
    
    private val repository = TennisRepository()
    
    private val _courts = MutableLiveData<List<Court>>()
    val courts: LiveData<List<Court>> = _courts
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    /**
     * Ładuje listę kortów z serwera (obecnie mock data)
     */
    fun loadCourts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.getCourts()
            result.onSuccess { courtsList ->
                _courts.value = courtsList
                _isLoading.value = false
            }.onFailure { exception ->
                _error.value = exception.message ?: "Nieznany błąd"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Czyści błąd
     */
    fun clearError() {
        _error.value = null
    }
}
