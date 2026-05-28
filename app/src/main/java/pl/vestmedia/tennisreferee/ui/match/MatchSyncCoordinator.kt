package pl.vestmedia.tennisreferee.ui.match

import kotlinx.coroutines.delay
import pl.vestmedia.tennisreferee.data.api.TennisApiService
import pl.vestmedia.tennisreferee.data.model.MatchEventFactory
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.data.repository.MatchHistoryRepository
import pl.vestmedia.tennisreferee.utils.AppLogger
import retrofit2.Response

class MatchSyncCoordinator(
    private val apiService: TennisApiService,
    private val matchHistoryRepository: MatchHistoryRepository,
    private val batteryInfoProvider: () -> MatchBatteryInfo,
    private val onSyncStatus: (SyncStatus) -> Unit,
    private val onBracketWarning: (warning: String, matchId: Int) -> Unit
) {
    suspend fun logMatchEvent(state: MatchState, eventType: String) {
        try {
            val batteryInfo = batteryInfoProvider()
            val event = MatchEventFactory.create(
                state = state,
                eventType = eventType,
                batteryLevel = batteryInfo.level,
                isCharging = batteryInfo.isCharging
            )

            val response = requestWithRetry("log $eventType") { apiService.logMatchEvent(event) }
            if (!response.isSuccessful) {
                AppLogger.error("logMatchEvent", "HTTP ${response.code()} for $eventType")
            }
        } catch (e: Exception) {
            AppLogger.error("logMatchEvent", "$eventType: ${e.message}")
        }
    }

    suspend fun syncMatch(state: MatchState) {
        try {
            if (state.matchId == null) {
                val response = requestWithRetry("create match") { apiService.createMatch(state.toMatch()) }

                if (response.isSuccessful && response.body() != null) {
                    val created = response.body()!!
                    state.matchId = created.id
                    AppLogger.api("createMatch", "OK id=${state.matchId} phase=${created.phase}")

                    created.bracketWarning?.let { warning ->
                        onBracketWarning(warning, created.id)
                    }
                } else {
                    AppLogger.api("createMatch", "FAIL ${response.code()}")
                }
            } else {
                val response = requestWithRetry("update match") {
                    apiService.updateMatch(state.matchId!!, state.toMatch())
                }

                if (!response.isSuccessful) {
                    AppLogger.api("updateMatch", "FAIL ${response.code()}")
                }
            }
        } catch (e: Exception) {
            AppLogger.error("syncMatchWithServer", e)
        }
    }

    suspend fun finalizeMatch(state: MatchState) {
        try {
            try {
                if (state.matchId == null) {
                    val response = requestWithRetry("create final match") { apiService.createMatch(state.toMatch()) }
                    if (response.isSuccessful && response.body() != null) {
                        state.matchId = response.body()!!.id
                        AppLogger.api("createFinalMatch", "OK id=${state.matchId}")
                    }
                }

                state.matchId?.let { matchId ->
                    requestWithRetry("sync final state") { apiService.updateMatch(matchId, state.toMatch()) }
                }
            } catch (e: Exception) {
                AppLogger.error("finalizeMatch", "sync final state: ${e.message}")
            }

            if (state.matchId == null) {
                AppLogger.error("finalizeMatch", "match id missing after final sync")
            }

            try {
                val batteryInfo = batteryInfoProvider()
                val event = MatchEventFactory.create(
                    state = state,
                    eventType = "match_end",
                    batteryLevel = batteryInfo.level,
                    isCharging = batteryInfo.isCharging
                )
                requestWithRetry("match_end event") { apiService.logMatchEvent(event) }
            } catch (e: Exception) {
                AppLogger.error("finalizeMatch", "match_end event: ${e.message}")
            }

            state.matchId?.let { matchId ->
                try {
                    requestWithRetry("finish match") { apiService.finishMatch(matchId) }
                } catch (e: Exception) {
                    AppLogger.error("finalizeMatch", "finish: ${e.message}")
                }
            }

            state.toMatchStatisticsRequest()?.let { statisticsRequest ->
                try {
                    requestWithRetry("send statistics") { apiService.sendMatchStatistics(statisticsRequest) }
                } catch (e: Exception) {
                    AppLogger.error("finalizeMatch", "statistics: ${e.message}")
                }
            }
        } catch (e: Exception) {
            AppLogger.error("finalizeMatch", "overall: ${e.message}")
        }

        try {
            matchHistoryRepository.saveMatch(state)
        } catch (e: Exception) {
            AppLogger.error("finalizeMatch", "local save: ${e.message}")
        }
    }

    suspend fun finishMatch(state: MatchState) {
        state.matchId?.let { matchId ->
            try {
                val response = requestWithRetry("finish match") { apiService.finishMatch(matchId) }
                if (!response.isSuccessful) {
                    AppLogger.api("finishMatch", "FAIL ${response.code()}")
                }
            } catch (e: Exception) {
                AppLogger.error("finishMatchOnServer", e)
            }
        }
    }

    suspend fun sendStatistics(state: MatchState) {
        val statisticsRequest = state.toMatchStatisticsRequest() ?: return
        try {
            val response = requestWithRetry("send statistics") { apiService.sendMatchStatistics(statisticsRequest) }
            if (response.isSuccessful) {
                AppLogger.api("sendStatistics", "OK")
            } else {
                AppLogger.api("sendStatistics", "FAIL ${response.code()}")
            }
        } catch (e: Exception) {
            AppLogger.error("sendMatchStatistics", e)
        }
    }

    private suspend fun <T> requestWithRetry(
        operation: String,
        maxAttempts: Int = 3,
        call: suspend () -> Response<T>
    ): Response<T> {
        onSyncStatus(SyncStatus.SYNCING)
        var lastException: Exception? = null
        var lastResponse: Response<T>? = null

        repeat(maxAttempts) { attempt ->
            try {
                val response = call()
                if (response.isSuccessful) {
                    onSyncStatus(SyncStatus.SYNCED)
                    return response
                }

                lastResponse = response
                if (!response.shouldRetry()) {
                    onSyncStatus(SyncStatus.FAILED)
                    return response
                }
                AppLogger.api(operation, "retryable HTTP ${response.code()} attempt=${attempt + 1}")
            } catch (e: Exception) {
                lastException = e
                AppLogger.error(operation, "attempt=${attempt + 1}: ${e.message}")
            }

            if (attempt < maxAttempts - 1) {
                delay(500L * (attempt + 1))
            }
        }

        onSyncStatus(SyncStatus.OFFLINE)
        lastResponse?.let { return it }
        throw lastException ?: IllegalStateException("$operation failed")
    }

    private fun Response<*>.shouldRetry(): Boolean {
        return code() in 500..599 || code() == 408 || code() == 429
    }
}

data class MatchBatteryInfo(
    val level: Int?,
    val isCharging: Boolean?
)