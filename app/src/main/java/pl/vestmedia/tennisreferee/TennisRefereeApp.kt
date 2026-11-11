package pl.vestmedia.tennisreferee

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.data.database.RoomMatchOutboxStore
import pl.vestmedia.tennisreferee.ui.match.MatchOutboxFlusher
import pl.vestmedia.tennisreferee.ui.match.RetrofitMatchApiClient
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
        if (shouldSendOutboxWhenOnline()) {
            sendOutboxWhenOnline()
        }
    }

    protected open fun shouldStartHealthCheck(): Boolean = true

    protected open fun shouldSendOutboxWhenOnline(): Boolean = true

    /**
     * What a match could not deliver waits in the outbox. Until now it went out only with
     * the next point of the next match, so a result finished offline stayed on the tablet
     * for as long as the umpire did not score again. The network coming back (and the app
     * starting with a network) now sends it, whatever screen is open.
     */
    private fun sendOutboxWhenOnline() {
        val connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        try {
            connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    backgroundScope.launch { flushOutbox() }
                }
            })
        } catch (e: RuntimeException) {
            // the system refuses the callback on some devices (too many registered); the
            // outbox is still sent with the next point, as before
            AppLogger.error("OutboxNetworkWatch", e)
        }
    }

    private suspend fun flushOutbox() {
        try {
            val result = MatchOutboxFlusher(
                outboxStore = RoomMatchOutboxStore(database.outboxMutationDao()),
                apiClient = RetrofitMatchApiClient(container.apiService),
            ).flushPending()
            if (result.flushed > 0 || result.failed > 0) {
                AppLogger.info("Outbox after network return: sent=${result.flushed} waiting=${result.failed}")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.error("OutboxNetworkFlush", e)
        }
    }
}
