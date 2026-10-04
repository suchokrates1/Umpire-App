package pl.vestmedia.tennisreferee

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import pl.vestmedia.tennisreferee.BuildConfig
import pl.vestmedia.tennisreferee.data.database.TennisDatabase
import pl.vestmedia.tennisreferee.data.repository.MatchHistoryRepository
import pl.vestmedia.tennisreferee.utils.AppLogger
import pl.vestmedia.tennisreferee.utils.HealthCheckManager
import pl.vestmedia.tennisreferee.utils.ThemeManager

/**
 * Główna klasa Application
 */
open class TennisRefereeApp : Application() {
    
    lateinit var container: AppContainer
        private set

    /**
     * Work that must outlive the screen that started it, such as sending a finished match:
     * the umpire may leave the match screen before the server has answered.
     */
    val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { TennisDatabase.getDatabase(this) }
    val matchHistoryRepository by lazy { MatchHistoryRepository(database.matchDao()) }
    val themeManager by lazy { ThemeManager(this) }
    val healthCheckManager by lazy { HealthCheckManager(this) }
    
    override fun onCreate() {
        super.onCreate()
        val debugBackend = if (BuildConfig.DEBUG) "https://test.blindtennis.app/" else null
        container = AppContainer(this, debugBackend)
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
