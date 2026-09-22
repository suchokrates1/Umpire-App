package pl.vestmedia.tennisreferee.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.database.MatchEntity

/**
 * ViewModel dla ekranu historii meczów
 */
class MatchHistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = (application as TennisRefereeApp).matchHistoryRepository
    
    val allMatches: LiveData<List<MatchEntity>> = repository.allMatches.asLiveData()
    
    fun deleteMatch(match: MatchEntity) = viewModelScope.launch {
        repository.deleteMatch(match)
    }
    
    fun deleteAllMatches() = viewModelScope.launch {
        repository.deleteAllMatches()
    }
}
