package pl.vestmedia.tennisreferee

import android.app.Application
import pl.vestmedia.tennisreferee.data.database.TennisDatabase
import pl.vestmedia.tennisreferee.data.repository.MatchHistoryRepository
import pl.vestmedia.tennisreferee.utils.ThemeManager

/**
 * Główna klasa Application
 */
class TennisRefereeApp : Application() {
    
    val database by lazy { TennisDatabase.getDatabase(this) }
    val matchHistoryRepository by lazy { MatchHistoryRepository(database.matchDao()) }
    val themeManager by lazy { ThemeManager(this) }
    
    override fun onCreate() {
        super.onCreate()
        // Apply saved theme on app start
        themeManager.applyCurrentTheme()
    }
}
