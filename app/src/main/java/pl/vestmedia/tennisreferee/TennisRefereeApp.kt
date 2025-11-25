package pl.vestmedia.tennisreferee

import android.app.Application
import pl.vestmedia.tennisreferee.data.database.TennisDatabase
import pl.vestmedia.tennisreferee.data.repository.MatchHistoryRepository

/**
 * Główna klasa Application
 */
class TennisRefereeApp : Application() {
    
    val database by lazy { TennisDatabase.getDatabase(this) }
    val matchHistoryRepository by lazy { MatchHistoryRepository(database.matchDao()) }
    
    override fun onCreate() {
        super.onCreate()
        // Inicjalizacja bibliotek, jeśli potrzebne
    }
}
