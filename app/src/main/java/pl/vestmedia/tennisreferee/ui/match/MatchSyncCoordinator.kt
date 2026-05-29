package pl.vestmedia.tennisreferee.ui.match

import pl.vestmedia.tennisreferee.data.api.MatchApiPayloadFactory
import pl.vestmedia.tennisreferee.data.api.TennisApiService
import pl.vestmedia.tennisreferee.data.model.MatchEventFactory
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.data.repository.MatchHistoryRepository
import retrofit2.Response

class MatchSyncCoordinator(
    private val apiClient: MatchApiClient,
    private val matchHistorySaver: MatchHistorySaver,
    private val batteryInfoProvider: () -> MatchBatteryInfo,
    private val onSyncStatus: (SyncStatus) -> Unit,
    private val onBracketWarning: (warning: String, matchId: Int) -> Unit,
    private val retryDelay: RetryDelay = CoroutineRetryDelay,
    private val logger: MatchSyncLogger = AppLoggerMatchSyncLogger,
    private val onSyncDiagnostics: (SyncStatus, String?) -> Unit = { _, _ -> }
) {
    constructor(
        apiService: TennisApiService,
        matchHistoryRepository: MatchHistoryRepository,
        batteryInfoProvider: () -> MatchBatteryInfo,
        onSyncStatus: (SyncStatus) -> Unit,
        onBracketWarning: (warning: String, matchId: Int) -> Unit,
        onSyncDiagnostics: (SyncStatus, String?) -> Unit = { _, _ -> }
    ) : this(
        apiClient = RetrofitMatchApiClient(apiService),
        matchHistorySaver = RoomMatchHistorySaver(matchHistoryRepository),
        batteryInfoProvider = batteryInfoProvider,
        onSyncStatus = onSyncStatus,
        onBracketWarning = onBracketWarning,
        onSyncDiagnostics = onSyncDiagnostics
    )

    suspend fun logMatchEvent(state: MatchState, eventType: String) {
        try {
            val batteryInfo = batteryInfoProvider()
            val event = MatchEventFactory.create(
                state = state,
                eventType = eventType,
                batteryLevel = batteryInfo.level,
                isCharging = batteryInfo.isCharging
            )

            val response = requestWithRetry("log $eventType") { apiClient.logMatchEvent(event) }
            if (!response.isSuccessful) {
                logger.error("logMatchEvent", "HTTP ${response.code()} for $eventType")
            }
        } catch (e: Exception) {
            logger.error("logMatchEvent", "$eventType: ${e.message}")
        }
    }

    suspend fun syncMatch(state: MatchState) {
        try {
            if (state.matchId == null) {
                val response = requestWithRetry("create match") { apiClient.createMatch(MatchApiPayloadFactory.toMatch(state)) }

                if (response.isSuccessful && response.body() != null) {
                    val created = response.body()!!
                    state.matchId = created.id
                    logger.api("createMatch", "OK id=${state.matchId} phase=${created.phase}")

                    created.bracketWarning?.let { warning ->
                        onBracketWarning(warning, created.id)
                    }
                } else {
                    logger.api("createMatch", "FAIL ${response.code()}")
                }
            } else {
                val response = requestWithRetry("update match") {
                    apiClient.updateMatch(state.matchId!!, MatchApiPayloadFactory.toMatch(state))
                }

                if (!response.isSuccessful) {
                    logger.api("updateMatch", "FAIL ${response.code()}")
                }
            }
        } catch (e: Exception) {
            logger.error("syncMatchWithServer", e)
        }
    }

    suspend fun finalizeMatch(
        state: MatchState,
        finishRequest: FinishMatchRequest = MatchApiPayloadFactory.toFinishRequest(state)
    ) {
        try {
            try {
                if (state.matchId == null) {
                    val response = requestWithRetry("create final match") { apiClient.createMatch(MatchApiPayloadFactory.toMatch(state)) }
                    if (response.isSuccessful && response.body() != null) {
                        state.matchId = response.body()!!.id
                        logger.api("createFinalMatch", "OK id=${state.matchId}")
                    }
                }

                state.matchId?.let { matchId ->
                    requestWithRetry("sync final state") { apiClient.updateMatch(matchId, MatchApiPayloadFactory.toMatch(state)) }
                }
            } catch (e: Exception) {
                logger.error("finalizeMatch", "sync final state: ${e.message}")
            }

            if (state.matchId == null) {
                logger.error("finalizeMatch", "match id missing after final sync")
            }

            try {
                val batteryInfo = batteryInfoProvider()
                val event = MatchEventFactory.create(
                    state = state,
                    eventType = "match_end",
                    batteryLevel = batteryInfo.level,
                    isCharging = batteryInfo.isCharging
                )
                requestWithRetry("match_end event") { apiClient.logMatchEvent(event) }
            } catch (e: Exception) {
                logger.error("finalizeMatch", "match_end event: ${e.message}")
            }

            state.matchId?.let { matchId ->
                try {
                    requestWithRetry("finish match") { apiClient.finishMatch(matchId, finishRequest) }
                } catch (e: Exception) {
                    logger.error("finalizeMatch", "finish: ${e.message}")
                }
            }

            if (finishRequest.finishReason != MatchFinishReason.TEST) MatchApiPayloadFactory.toStatisticsRequest(state)?.let { statisticsRequest ->
                try {
                    requestWithRetry("send statistics") { apiClient.sendMatchStatistics(statisticsRequest) }
                } catch (e: Exception) {
                    logger.error("finalizeMatch", "statistics: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.error("finalizeMatch", "overall: ${e.message}")
        }

        if (finishRequest.finishReason != MatchFinishReason.TEST) {
            try {
                matchHistorySaver.saveMatch(state)
            } catch (e: Exception) {
                logger.error("finalizeMatch", "local save: ${e.message}")
            }
        }
    }

    suspend fun finishMatch(
        state: MatchState,
        finishRequest: FinishMatchRequest = MatchApiPayloadFactory.toFinishRequest(state)
    ) {
        state.matchId?.let { matchId ->
            try {
                val response = requestWithRetry("finish match") { apiClient.finishMatch(matchId, finishRequest) }
                if (!response.isSuccessful) {
                    logger.api("finishMatch", "FAIL ${response.code()}")
                }
            } catch (e: Exception) {
                logger.error("finishMatchOnServer", e)
            }
        }
    }

    suspend fun sendStatistics(state: MatchState) {
        val statisticsRequest = MatchApiPayloadFactory.toStatisticsRequest(state) ?: return
        try {
            val response = requestWithRetry("send statistics") { apiClient.sendMatchStatistics(statisticsRequest) }
            if (response.isSuccessful) {
                logger.api("sendStatistics", "OK")
            } else {
                logger.api("sendStatistics", "FAIL ${response.code()}")
            }
        } catch (e: Exception) {
            logger.error("sendMatchStatistics", e)
        }
    }

    private suspend fun <T> requestWithRetry(
        operation: String,
        maxAttempts: Int = 3,
        call: suspend () -> Response<T>
    ): Response<T> {
        updateSyncStatus(SyncStatus.SYNCING)
        var lastException: Exception? = null
        var lastResponse: Response<T>? = null

        repeat(maxAttempts) { attempt ->
            try {
                val response = call()
                if (response.isSuccessful) {
                    updateSyncStatus(SyncStatus.SYNCED)
                    return response
                }

                lastResponse = response
                if (!response.shouldRetry()) {
                    updateSyncStatus(SyncStatus.FAILED, "$operation: HTTP ${response.code()}")
                    return response
                }
                logger.api(operation, "retryable HTTP ${response.code()} attempt=${attempt + 1}")
            } catch (e: Exception) {
                lastException = e
                logger.error(operation, "attempt=${attempt + 1}: ${e.message}")
            }

            if (attempt < maxAttempts - 1) {
                retryDelay.waitBeforeNextAttempt(attempt + 1)
            }
        }

        val errorMessage = lastException?.message
            ?: lastResponse?.let { "HTTP ${it.code()}" }
            ?: "$operation failed"
        updateSyncStatus(SyncStatus.OFFLINE, "$operation: $errorMessage")
        lastResponse?.let { return it }
        throw lastException ?: IllegalStateException("$operation failed")
    }

    private fun updateSyncStatus(status: SyncStatus, errorMessage: String? = null) {
        onSyncStatus(status)
        onSyncDiagnostics(status, errorMessage)
    }

    private fun Response<*>.shouldRetry(): Boolean {
        return code() in 500..599 || code() == 408 || code() == 429
    }
}
