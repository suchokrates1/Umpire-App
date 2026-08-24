package pl.vestmedia.tennisreferee

import android.app.Application
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider
import pl.vestmedia.tennisreferee.data.database.TennisDatabase
import pl.vestmedia.tennisreferee.data.repository.MatchHistoryRepository
import pl.vestmedia.tennisreferee.utils.AppLogger
import pl.vestmedia.tennisreferee.utils.HealthCheckManager
import pl.vestmedia.tennisreferee.utils.StartupCrashLog
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
        StartupCrashLog.install(this)
        try {
            CourtSessionProvider.initialize(this)
            themeManager.applyCurrentTheme()
            AppLogger.info("App started")
            if (shouldStartHealthCheck()) {
                healthCheckManager.start()
            }
        } catch (error: Throwable) {
            StartupCrashLog.write(this, error)
            AppLogger.error("Application.onCreate", error)
        }
    }

    protected open fun shouldStartHealthCheck(): Boolean = true
}
