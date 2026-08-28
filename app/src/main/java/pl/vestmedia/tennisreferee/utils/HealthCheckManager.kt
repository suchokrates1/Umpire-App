package pl.vestmedia.tennisreferee.utils

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.*
import pl.vestmedia.tennisreferee.data.api.RetrofitClient

/**
 * Wysyła periodyczny heartbeat do serwera ze stanem baterii i statusem online.
 * Działa niezależnie od meczu — dopóki appka jest otwarta.
 */
class HealthCheckManager(private val app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    /** Aktualnie przypisany kort (ustawiany po autoryzacji PIN) */
    var courtId: String? = null

    /** Aktualny ekran — do telemetrii */
    var currentScreen: String = "unknown"

    var matchId: Int? = null
    var clientMatchUuid: String? = null
    var onDirectorCommands: ((List<pl.vestmedia.tennisreferee.data.api.dto.DirectorCommandDto>) -> Unit)? = null

    /**
     * Rozpocznij wysyłanie heartbeat co [intervalMs] ms.
     * Domyślnie co 2 minuty.
     */
    fun start(intervalMs: Long = 120_000L) {
        if (heartbeatJob?.isActive == true) return
        AppLogger.health("Starting heartbeat: interval=${intervalMs}ms")
        heartbeatJob = scope.launch {
            while (isActive) {
                sendHeartbeat()
                delay(intervalMs)
            }
        }
    }

    /** Zatrzymaj heartbeat */
    fun stop() {
        AppLogger.health("Stopping heartbeat")
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /** Wyślij pojedynczy heartbeat teraz (np. przy zmianie ekranu) */
    fun sendNow() {
        scope.launch { sendHeartbeat() }
    }

    private suspend fun sendHeartbeat() {
        try {
            val battery = getBatteryLevel()
            val charging = isBatteryCharging()
            val version = getAppVersion()

            val body = mutableMapOf(
                "court_id" to (courtId ?: ""),
                "battery_level" to (battery?.toString() ?: ""),
                "is_charging" to charging.toString(),
                "screen" to currentScreen,
                "app_version" to version,
                "timestamp" to System.currentTimeMillis().toString()
            )
            matchId?.let { body["match_id"] = it.toString() }
            clientMatchUuid?.takeIf { it.isNotBlank() }?.let { body["client_match_uuid"] = it }

            val response = RetrofitClient.apiService.sendHeartbeat(body)
            if (response.isSuccessful) {
                AppLogger.health("Heartbeat OK | court=$courtId battery=$battery% charging=$charging screen=$currentScreen")
                val commands = response.body()?.commands.orEmpty()
                if (commands.isNotEmpty()) {
                    onDirectorCommands?.invoke(commands)
                }
            } else {
                AppLogger.error("Heartbeat", "HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            AppLogger.error("Heartbeat", e)
        }
    }

    private fun getBatteryLevel(): Int? {
        val intent = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return intent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) (level * 100 / scale) else null
        }
    }

    private fun isBatteryCharging(): Boolean {
        val intent = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getAppVersion(): String {
        return try {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}
