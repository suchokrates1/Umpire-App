package pl.vestmedia.tennisreferee.ui.match

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.MatchApiPayloadFactory
import pl.vestmedia.tennisreferee.data.api.dto.MatchDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatisticsRequestDto
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player
import retrofit2.Response

class MatchSyncCoordinatorTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "DE")

    @Test
    fun syncMatchRetriesCreateStoresMatchIdAndEmitsBracketWarning() = runBlocking {
        val apiClient = FakeMatchApiClient().apply {
            createResults += queuedResponse(httpError(500))
            createResults += queuedResponse(Response.success(apiMatch(id = 42, bracketWarning = "manual_advance_required")))
        }
        val retryDelay = RecordingRetryDelay()
        val statuses = mutableListOf<SyncStatus>()
        val warnings = mutableListOf<Pair<String, Int>>()
        val coordinator = coordinator(apiClient, statuses, warnings, retryDelay = retryDelay)
        val state = matchState()

        coordinator.syncMatch(state)

        assertEquals(42, state.matchId)
        assertEquals(2, apiClient.createCalls)
        assertEquals(listOf(1), retryDelay.attemptNumbers)
        assertEquals(listOf(SyncStatus.SYNCING, SyncStatus.SYNCED), statuses)
        assertEquals(listOf("manual_advance_required" to 42), warnings)
    }

    @Test
    fun syncMatchMarksFailedWithoutRetryForNonRetryableHttpError() = runBlocking {
        val apiClient = FakeMatchApiClient().apply {
            createResults += queuedResponse(httpError(400))
        }
        val retryDelay = RecordingRetryDelay()
        val statuses = mutableListOf<SyncStatus>()
        val diagnostics = mutableListOf<Pair<SyncStatus, String?>>()
        val coordinator = coordinator(apiClient, statuses, retryDelay = retryDelay, diagnostics = diagnostics)
        val state = matchState()

        coordinator.syncMatch(state)

        assertNull(state.matchId)
        assertEquals(1, apiClient.createCalls)
        assertTrue(retryDelay.attemptNumbers.isEmpty())
        assertEquals(listOf(SyncStatus.SYNCING, SyncStatus.FAILED), statuses)
        assertEquals(
            listOf(SyncStatus.SYNCING to null, SyncStatus.FAILED to "create match: HTTP 400"),
            diagnostics
        )
    }

    @Test
    fun syncMatchMarksOfflineAfterRetryableHttpFailuresAreExhausted() = runBlocking {
        val apiClient = FakeMatchApiClient().apply {
            createResults += queuedResponse(httpError(500))
            createResults += queuedResponse(httpError(502))
            createResults += queuedResponse(httpError(503))
        }
        val retryDelay = RecordingRetryDelay()
        val statuses = mutableListOf<SyncStatus>()
        val diagnostics = mutableListOf<Pair<SyncStatus, String?>>()
        val coordinator = coordinator(apiClient, statuses, retryDelay = retryDelay, diagnostics = diagnostics)
        val state = matchState()

        coordinator.syncMatch(state)

        assertNull(state.matchId)
        assertEquals(3, apiClient.createCalls)
        assertEquals(listOf(1, 2), retryDelay.attemptNumbers)
        assertEquals(listOf(SyncStatus.SYNCING, SyncStatus.OFFLINE), statuses)
        assertEquals(SyncStatus.OFFLINE to "create match: HTTP 503", diagnostics.last())
    }

    @Test
    fun syncMatchMarksOfflineAfterExceptionsAreExhausted() = runBlocking {
        val apiClient = FakeMatchApiClient().apply {
            createResults += queuedFailure(IOException("offline 1"))
            createResults += queuedFailure(IOException("offline 2"))
            createResults += queuedFailure(IOException("offline 3"))
        }
        val retryDelay = RecordingRetryDelay()
        val statuses = mutableListOf<SyncStatus>()
        val coordinator = coordinator(apiClient, statuses, retryDelay = retryDelay)

        coordinator.syncMatch(matchState())

        assertEquals(3, apiClient.createCalls)
        assertEquals(listOf(1, 2), retryDelay.attemptNumbers)
        assertEquals(listOf(SyncStatus.SYNCING, SyncStatus.OFFLINE), statuses)
    }

    @Test
    fun finalizeMatchCreatesUpdatesLogsFinishesSendsStatisticsAndSavesLocally() = runBlocking {
        val apiClient = FakeMatchApiClient().apply {
            createResults += queuedResponse(Response.success(apiMatch(id = 77)))
            updateResults += queuedResponse(Response.success(apiMatch(id = 77)))
            eventResults += queuedResponse(Response.success(MatchEventResponseDto(success = true, message = null)))
            finishResults += queuedResponse(Response.success(apiMatch(id = 77)))
            statisticsResults += queuedResponse(Response.success(Unit))
        }
        val historySaver = RecordingHistorySaver()
        val coordinator = coordinator(apiClient, historySaver = historySaver)
        val state = matchState().apply {
            isMatchFinished = true
            player1Sets = 2
            player2Sets = 0
            matchDuration = 123_000L
        }

        coordinator.finalizeMatch(state)

        assertEquals(77, state.matchId)
        assertEquals(listOf("create", "update:77", "event:match_end", "finish:77", "statistics"), apiClient.operations)
        assertEquals(MatchFinishReason.NORMAL, apiClient.finishRequests.single().finishReason)
        assertEquals(1, apiClient.loggedEvents.size)
        assertEquals(65, apiClient.loggedEvents.single().batteryLevel)
        assertEquals(true, apiClient.loggedEvents.single().isCharging)
        assertEquals(1, apiClient.statisticsRequests.size)
        assertSame(state, historySaver.savedStates.single())
    }

    @Test
    fun finalizeTestMatchSkipsStatisticsAndLocalHistory() = runBlocking {
        val apiClient = FakeMatchApiClient().apply {
            createResults += queuedResponse(Response.success(apiMatch(id = 88)))
            updateResults += queuedResponse(Response.success(apiMatch(id = 88)))
            eventResults += queuedResponse(Response.success(MatchEventResponseDto(success = true, message = null)))
            finishResults += queuedResponse(Response.success(apiMatch(id = 88)))
        }
        val historySaver = RecordingHistorySaver()
        val coordinator = coordinator(apiClient, historySaver = historySaver)
        val state = matchState().apply {
            isMatchFinished = true
            finishReason = MatchFinishReason.TEST
        }

        coordinator.finalizeMatch(state)

        assertEquals(listOf("create", "update:88", "event:match_end", "finish:88"), apiClient.operations)
        assertEquals(MatchFinishReason.TEST, apiClient.finishRequests.single().finishReason)
        assertTrue(apiClient.statisticsRequests.isEmpty())
        assertTrue(historySaver.savedStates.isEmpty())
    }

    private fun coordinator(
        apiClient: FakeMatchApiClient,
        statuses: MutableList<SyncStatus> = mutableListOf(),
        warnings: MutableList<Pair<String, Int>> = mutableListOf(),
        historySaver: MatchHistorySaver = RecordingHistorySaver(),
        retryDelay: RetryDelay = RecordingRetryDelay(),
        diagnostics: MutableList<Pair<SyncStatus, String?>> = mutableListOf()
    ): MatchSyncCoordinator {
        return MatchSyncCoordinator(
            apiClient = apiClient,
            matchHistorySaver = historySaver,
            batteryInfoProvider = { MatchBatteryInfo(level = 65, isCharging = true) },
            onSyncStatus = { statuses += it },
            onBracketWarning = { warning, matchId -> warnings += warning to matchId },
            retryDelay = retryDelay,
            logger = NoOpMatchSyncLogger,
            onSyncDiagnostics = { status, error -> diagnostics += status to error }
        )
    }

    private fun matchState(): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "1",
            courtName = "Court 1"
        )
    }

    private fun apiMatch(id: Int, bracketWarning: String? = null): MatchDto {
        return MatchApiPayloadFactory.toMatch(matchState()).copy(
            id = id,
            bracketWarning = bracketWarning
        )
    }
}

private class FakeMatchApiClient : MatchApiClient {
    val createResults = mutableListOf<QueuedResponse<MatchDto>>()
    val updateResults = mutableListOf<QueuedResponse<MatchDto>>()
    val finishResults = mutableListOf<QueuedResponse<MatchDto>>()
    val eventResults = mutableListOf<QueuedResponse<MatchEventResponseDto>>()
    val statisticsResults = mutableListOf<QueuedResponse<Unit>>()
    val operations = mutableListOf<String>()
    val loggedEvents = mutableListOf<MatchEventDto>()
    val finishRequests = mutableListOf<FinishMatchRequest>()
    val statisticsRequests = mutableListOf<MatchStatisticsRequestDto>()
    var createCalls = 0

    override suspend fun createMatch(match: MatchDto): Response<MatchDto> {
        createCalls++
        operations += "create"
        return createResults.next()
    }

    override suspend fun updateMatch(matchId: Int, match: MatchDto): Response<MatchDto> {
        operations += "update:$matchId"
        return updateResults.next()
    }

    override suspend fun finishMatch(matchId: Int, request: FinishMatchRequest): Response<MatchDto> {
        operations += "finish:$matchId"
        finishRequests += request
        return finishResults.next()
    }

    override suspend fun logMatchEvent(event: MatchEventDto): Response<MatchEventResponseDto> {
        operations += "event:${event.eventType}"
        loggedEvents += event
        return eventResults.next()
    }

    override suspend fun sendMatchStatistics(statistics: MatchStatisticsRequestDto): Response<Unit> {
        operations += "statistics"
        statisticsRequests += statistics
        return statisticsResults.next()
    }
}

private class RecordingHistorySaver : MatchHistorySaver {
    val savedStates = mutableListOf<MatchState>()

    override suspend fun saveMatch(state: MatchState): Long {
        savedStates += state
        return savedStates.size.toLong()
    }
}

private class RecordingRetryDelay : RetryDelay {
    val attemptNumbers = mutableListOf<Int>()

    override suspend fun waitBeforeNextAttempt(attemptNumber: Int) {
        attemptNumbers += attemptNumber
    }
}

private object NoOpMatchSyncLogger : MatchSyncLogger {
    override fun api(endpoint: String, result: String) = Unit
    override fun error(context: String, error: Throwable) = Unit
    override fun error(context: String, message: String) = Unit
}

private sealed class QueuedResponse<T> {
    data class Value<T>(val response: Response<T>) : QueuedResponse<T>()
    data class Failure<T>(val exception: Exception) : QueuedResponse<T>()
}

private fun <T> queuedResponse(response: Response<T>): QueuedResponse<T> {
    return QueuedResponse.Value(response)
}

private fun <T> queuedFailure(exception: Exception): QueuedResponse<T> {
    return QueuedResponse.Failure(exception)
}

private fun <T> MutableList<QueuedResponse<T>>.next(): Response<T> {
    val result = removeAt(0)
    return when (result) {
        is QueuedResponse.Value -> result.response
        is QueuedResponse.Failure -> throw result.exception
    }
}

private fun <T> httpError(code: Int): Response<T> {
    return Response.error(code, "error".toResponseBody("text/plain".toMediaType()))
}