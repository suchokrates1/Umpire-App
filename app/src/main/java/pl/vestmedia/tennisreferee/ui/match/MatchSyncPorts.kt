package pl.vestmedia.tennisreferee.ui.match

import kotlinx.coroutines.delay
import pl.vestmedia.tennisreferee.data.api.TennisApiService
import pl.vestmedia.tennisreferee.data.model.Match
import pl.vestmedia.tennisreferee.data.model.MatchEvent
import pl.vestmedia.tennisreferee.data.model.MatchEventResponse
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.data.model.MatchStatisticsRequest
import pl.vestmedia.tennisreferee.data.repository.MatchHistoryRepository
import pl.vestmedia.tennisreferee.utils.AppLogger
import retrofit2.Response

data class MatchBatteryInfo(
    val level: Int?,
    val isCharging: Boolean?
)

interface MatchApiClient {
    suspend fun createMatch(match: Match): Response<Match>
    suspend fun updateMatch(matchId: Int, match: Match): Response<Match>
    suspend fun finishMatch(matchId: Int): Response<Match>
    suspend fun logMatchEvent(event: MatchEvent): Response<MatchEventResponse>
    suspend fun sendMatchStatistics(statistics: MatchStatisticsRequest): Response<Unit>
}

interface MatchHistorySaver {
    suspend fun saveMatch(state: MatchState): Long
}

interface RetryDelay {
    suspend fun waitBeforeNextAttempt(attemptNumber: Int)
}

interface MatchSyncLogger {
    fun api(endpoint: String, result: String)
    fun error(context: String, error: Throwable)
    fun error(context: String, message: String)
}

class RetrofitMatchApiClient(
    private val apiService: TennisApiService
) : MatchApiClient {
    override suspend fun createMatch(match: Match): Response<Match> {
        return apiService.createMatch(match)
    }

    override suspend fun updateMatch(matchId: Int, match: Match): Response<Match> {
        return apiService.updateMatch(matchId, match)
    }

    override suspend fun finishMatch(matchId: Int): Response<Match> {
        return apiService.finishMatch(matchId)
    }

    override suspend fun logMatchEvent(event: MatchEvent): Response<MatchEventResponse> {
        return apiService.logMatchEvent(event)
    }

    override suspend fun sendMatchStatistics(statistics: MatchStatisticsRequest): Response<Unit> {
        return apiService.sendMatchStatistics(statistics)
    }
}

class RoomMatchHistorySaver(
    private val matchHistoryRepository: MatchHistoryRepository
) : MatchHistorySaver {
    override suspend fun saveMatch(state: MatchState): Long {
        return matchHistoryRepository.saveMatch(state)
    }
}

object CoroutineRetryDelay : RetryDelay {
    override suspend fun waitBeforeNextAttempt(attemptNumber: Int) {
        delay(500L * attemptNumber)
    }
}

object AppLoggerMatchSyncLogger : MatchSyncLogger {
    override fun api(endpoint: String, result: String) {
        AppLogger.api(endpoint, result)
    }

    override fun error(context: String, error: Throwable) {
        AppLogger.error(context, error)
    }

    override fun error(context: String, message: String) {
        AppLogger.error(context, message)
    }
}