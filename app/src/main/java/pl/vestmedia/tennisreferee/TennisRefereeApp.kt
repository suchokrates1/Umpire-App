package pl.vestmedia.tennisreferee

import android.app.Application
import pl.vestmedia.tennisreferee.BuildConfig
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider
import pl.vestmedia.tennisreferee.data.database.TennisDatabase
import pl.vestmedia.tennisreferee.data.repository.MatchHistoryRepository
import pl.vestmedia.tennisreferee.utils.AppLogger
import pl.vestmedia.tennisreferee.utils.HealthCheckManager
import pl.vestmedia.tennisreferee.utils.ThemeManager

/**
 * Główna klasa Application
 */
open class TennisRefereeApp : Application() {
    
    val database by lazy { TennisDatabase.getDatabase(this) }
    val matchHistoryRepository by lazy { MatchHistoryRepository(database.matchDao()) }
    val themeManager by lazy { ThemeManager(this) }
    val healthCheckManager by lazy { HealthCheckManager(this) }
    
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            RetrofitClient.overrideBaseUrl("https://test.blindtennis.app/")
        }
        CourtSessionProvider.initialize(this)
        // Apply saved theme on app start
        themeManager.applyCurrentTheme()
        // Start health check heartbeat
        AppLogger.info("App started")
        if (shouldStartHealthCheck()) {
            healthCheckManager.start()
        }
    }

    protected open fun shouldStartHealthCheck(): Boolean = true
}
