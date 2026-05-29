package pl.vestmedia.tennisreferee.ui.match

import kotlinx.coroutines.delay
import pl.vestmedia.tennisreferee.data.api.TennisApiService
import pl.vestmedia.tennisreferee.data.api.dto.MatchDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatisticsRequestDto
import pl.vestmedia.tennisreferee.data.api.dto.toDto
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.data.repository.MatchHistoryRepository
import pl.vestmedia.tennisreferee.utils.AppLogger
import retrofit2.Response

data class MatchBatteryInfo(
    val level: Int?,
    val isCharging: Boolean?
)

interface MatchApiClient {
    suspend fun createMatch(match: MatchDto): Response<MatchDto>
    suspend fun updateMatch(matchId: Int, match: MatchDto): Response<MatchDto>
    suspend fun finishMatch(matchId: Int, request: FinishMatchRequest): Response<MatchDto>
    suspend fun logMatchEvent(event: MatchEventDto): Response<MatchEventResponseDto>
    suspend fun sendMatchStatistics(statistics: MatchStatisticsRequestDto): Response<Unit>
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
    override suspend fun createMatch(match: MatchDto): Response<MatchDto> {
        return apiService.createMatch(match)
    }

    override suspend fun updateMatch(matchId: Int, match: MatchDto): Response<MatchDto> {
        return apiService.updateMatch(matchId, match)
    }

    override suspend fun finishMatch(matchId: Int, request: FinishMatchRequest): Response<MatchDto> {
        return apiService.finishMatch(matchId, request.toDto())
    }

    override suspend fun logMatchEvent(event: MatchEventDto): Response<MatchEventResponseDto> {
        return apiService.logMatchEvent(event)
    }

    override suspend fun sendMatchStatistics(statistics: MatchStatisticsRequestDto): Response<Unit> {
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