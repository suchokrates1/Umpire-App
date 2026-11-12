package pl.vestmedia.tennisreferee.ui.courtselection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.repository.TennisRepository
import pl.vestmedia.tennisreferee.utils.AppLogger

/**
 * ViewModel dla ekranu wyboru kortu
 */
class CourtSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as TennisRefereeApp).container.repository()
    
    private val _courts = MutableLiveData<List<Court>>()
    val courts: LiveData<List<Court>> = _courts
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    /**
     * Ładuje listę kortów z serwera (obecnie mock data)
     */
    fun loadCourts(tournamentId: Int? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.getCourts(tournamentId)
            result.onSuccess { courtsList ->
                _courts.value = courtsList
                _isLoading.value = false
            }.onFailure { exception ->
                AppLogger.error("CourtSelection", exception)
                _error.value = getApplication<Application>().getString(R.string.error_loading_courts)
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
