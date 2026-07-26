package pl.vestmedia.tennisreferee.ui.match

import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.MatchApiPayloadFactory
import pl.vestmedia.tennisreferee.data.api.dto.MatchDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatisticsRequestDto
import pl.vestmedia.tennisreferee.data.database.OutboxMutationEntity
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import retrofit2.Response

class MatchOutboxFlusherTest {
    private val gson = Gson()
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "DE")

    // ── Flusher tests ──

    @Test
    fun flushEmptyOutboxReturnsZero() = runBlocking {
        val store = InMemoryOutboxStore()
        val flusher = MatchOutboxFlusher(store, TestApiClient(), SilentLogger)

        val result = flusher.flushPending()

        assertEquals(0, result.flushed)
        assertEquals(0, result.failed)
        assertFalse(result.stoppedOnAuth)
    }

    @Test
    fun flushSuccessfulCreateMarksDone() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            createResults += Response.success(apiMatch(id = 42))
        }
        val flusher = MatchOutboxFlusher(store, api, SilentLogger)
        flusher.enqueue("uuid-1", "CREATE", null, gson.toJson(apiMatch()))

        val result = flusher.flushPending()

        assertEquals(1, result.flushed)
        assertEquals(0, result.failed)
        assertTrue(store.allEntries().none { it.status == "PENDING" })
    }

    @Test
    fun flushCreateThenUpdateResolvesServerMatchId() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            createResults += Response.success(apiMatch(id = 42))
            updateResults += Response.success(apiMatch(id = 42))
        }
        val flusher = MatchOutboxFlusher(store, api, SilentLogger)
        flusher.enqueue("uuid-1", "CREATE", null, gson.toJson(apiMatch()))
        flusher.enqueue("uuid-1", "UPDATE", null, gson.toJson(apiMatch()))

        val result = flusher.flushPending()

        assertEquals(2, result.flushed)
        assertEquals(0, result.failed)
        assertEquals(listOf("create", "update:42"), api.operations)
    }

    @Test
    fun flushProcessesCreateBeforeOtherTypes() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            createResults += Response.success(apiMatch(id = 42))
            updateResults += Response.success(apiMatch(id = 42))
        }
        val flusher = MatchOutboxFlusher(store, api, SilentLogger)
        flusher.enqueue("uuid-1", "UPDATE", null, gson.toJson(apiMatch()))
        flusher.enqueue("uuid-1", "CREATE", null, gson.toJson(apiMatch()))

        val result = flusher.flushPending()

        assertEquals(2, result.flushed)
        assertEquals(listOf("create", "update:42"), api.operations)
    }

    @Test
    fun flushStopsOn401AndMarksFailed() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            createResults += httpError(401)
        }
        val flusher = MatchOutboxFlusher(store, api, SilentLogger)
        flusher.enqueue("uuid-1", "CREATE", null, gson.toJson(apiMatch()))

        val result = flusher.flushPending()

        assertEquals(0, result.flushed)
        assertEquals(1, result.failed)
        assertTrue(result.stoppedOnAuth)
        assertEquals("FAILED_AUTH", store.allEntries().single().status)
    }

    @Test
    fun flushRetriesOnNetworkError() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            createResults += null
        }
        val flusher = MatchOutboxFlusher(store, api, SilentLogger)
        flusher.enqueue("uuid-1", "CREATE", null, gson.toJson(apiMatch()))

        val result = flusher.flushPending()

        assertEquals(0, result.flushed)
        assertEquals(1, result.failed)
        val entry = store.allEntries().single()
        assertEquals("PENDING", entry.status)
        assertEquals(1, entry.attempts)
    }

    @Test
    fun flushSkipsUpdateWithoutServerMatchId() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient()
        val flusher = MatchOutboxFlusher(store, api, SilentLogger)
        flusher.enqueue("uuid-1", "UPDATE", null, gson.toJson(apiMatch()))

        val result = flusher.flushPending()

        assertEquals(0, result.flushed)
        assertEquals(1, result.failed)
        assertEquals("PENDING", store.allEntries().single().status)
        assertTrue(api.operations.isEmpty())
    }

    @Test
    fun enqueueCoalescesUpdatesForSameUuid() = runBlocking {
        val store = InMemoryOutboxStore()
        val flusher = MatchOutboxFlusher(store, TestApiClient(), SilentLogger)

        flusher.enqueue("uuid-1", "UPDATE", 10, "payload1")
        flusher.enqueue("uuid-1", "UPDATE", 10, "payload2")
        flusher.enqueue("uuid-1", "UPDATE", 10, "payload3")

        val pending = store.getPending()
        assertEquals(1, pending.size)
        assertEquals("payload3", pending.single().payloadJson)
    }

    @Test
    fun flushIdempotencyViaClientMatchUuid() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            createResults += Response.success(apiMatch(id = 42))
            createResults += Response.success(apiMatch(id = 42))
        }
        val flusher = MatchOutboxFlusher(store, api, SilentLogger)
        flusher.enqueue("uuid-1", "CREATE", null, gson.toJson(apiMatch()))

        flusher.flushPending()
        val result2 = flusher.flushPending()

        assertEquals(0, result2.flushed)
        assertEquals(1, api.operations.count { it == "create" })
    }

    @Test
    fun flushPropagatesServerMatchIdToRemainingPending() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            createResults += Response.success(apiMatch(id = 55))
            finishResults += httpError(500)
        }
        val flusher = MatchOutboxFlusher(store, api, SilentLogger)
        flusher.enqueue("uuid-1", "CREATE", null, gson.toJson(apiMatch()))
        flusher.enqueue("uuid-1", "FINISH", null, gson.toJson(FinishMatchRequest()))

        flusher.flushPending()

        val remaining = store.allEntries().filter { it.status == "PENDING" }
        assertEquals(1, remaining.size)
        assertEquals(55, remaining.single().serverMatchId)
    }

    // ── Coordinator integration tests ──

    @Test
    fun coordinatorEnqueuesSyncMatchOnExhaustedException() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            createResults += null
            createResults += null
            createResults += null
        }
        val flusher = MatchOutboxFlusher(store, TestApiClient(), SilentLogger)
        val coordinator = MatchSyncCoordinator(
            apiClient = api,
            matchHistorySaver = StubHistorySaver,
            batteryInfoProvider = { MatchBatteryInfo(65, true) },
            onSyncStatus = {},
            onBracketWarning = { _, _ -> },
            retryDelay = ImmediateRetryDelay,
            logger = SilentLogger,
            outboxFlusher = flusher
        )
        val state = matchState()

        coordinator.syncMatch(state)

        val pending = store.getPending()
        assertEquals(1, pending.size)
        assertEquals("CREATE", pending.single().type)
        assertEquals(state.clientMatchUuid, pending.single().clientMatchUuid)
    }

    @Test
    fun coordinatorEnqueuesSyncMatchOnRetryableHttpExhausted() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            createResults += httpError(500)
            createResults += httpError(502)
            createResults += httpError(503)
        }
        val flusher = MatchOutboxFlusher(store, TestApiClient(), SilentLogger)
        val coordinator = MatchSyncCoordinator(
            apiClient = api,
            matchHistorySaver = StubHistorySaver,
            batteryInfoProvider = { MatchBatteryInfo(65, true) },
            onSyncStatus = {},
            onBracketWarning = { _, _ -> },
            retryDelay = ImmediateRetryDelay,
            logger = SilentLogger,
            outboxFlusher = flusher
        )
        val state = matchState()

        coordinator.syncMatch(state)

        val pending = store.getPending()
        assertEquals(1, pending.size)
        assertEquals("CREATE", pending.single().type)
    }

    @Test
    fun coordinatorDoesNotEnqueueOnNonRetryableError() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            createResults += httpError(400)
        }
        val flusher = MatchOutboxFlusher(store, TestApiClient(), SilentLogger)
        val coordinator = MatchSyncCoordinator(
            apiClient = api,
            matchHistorySaver = StubHistorySaver,
            batteryInfoProvider = { MatchBatteryInfo(65, true) },
            onSyncStatus = {},
            onBracketWarning = { _, _ -> },
            retryDelay = ImmediateRetryDelay,
            logger = SilentLogger,
            outboxFlusher = flusher
        )

        coordinator.syncMatch(matchState())

        assertTrue(store.getPending().isEmpty())
    }

    @Test
    fun coordinatorEnqueuesUpdateWhenMatchIdExists() = runBlocking {
        val store = InMemoryOutboxStore()
        val api = TestApiClient().apply {
            updateResults += null
            updateResults += null
            updateResults += null
        }
        val flusher = MatchOutboxFlusher(store, TestApiClient(), SilentLogger)
        val coordinator = MatchSyncCoordinator(
            apiClient = api,
            matchHistorySaver = StubHistorySaver,
            batteryInfoProvider = { MatchBatteryInfo(65, true) },
            onSyncStatus = {},
            onBracketWarning = { _, _ -> },
            retryDelay = ImmediateRetryDelay,
            logger = SilentLogger,
            outboxFlusher = flusher
        )
        val state = matchState().apply { matchId = 99 }

        coordinator.syncMatch(state)

        val pending = store.getPending()
        assertEquals(1, pending.size)
        assertEquals("UPDATE", pending.single().type)
        assertEquals(99, pending.single().serverMatchId)
    }

    // ── Helpers ──

    private fun matchState(): MatchState {
        return MatchState(
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "1",
            courtName = "Court 1"
        )
    }

    private fun apiMatch(id: Int = 0): MatchDto {
        return MatchApiPayloadFactory.toMatch(matchState()).copy(id = id)
    }
}

// ── Test doubles ──

private class InMemoryOutboxStore : MatchOutboxStore {
    private val entries = mutableListOf<OutboxMutationEntity>()
    private var nextId = 1L

    override suspend fun enqueue(mutation: OutboxMutationEntity): Long {
        val id = nextId++
        val entry = mutation.copy(id = id)
        entries += entry
        if (mutation.type == "UPDATE") {
            entries.removeAll {
                it.clientMatchUuid == mutation.clientMatchUuid &&
                it.type == "UPDATE" &&
                it.status == "PENDING" &&
                it.id < id
            }
        }
        return id
    }

    override suspend fun getPending(): List<OutboxMutationEntity> =
        entries.filter { it.status == "PENDING" }.sortedBy { it.createdAt }

    override suspend fun update(mutation: OutboxMutationEntity) {
        val index = entries.indexOfFirst { it.id == mutation.id }
        if (index >= 0) entries[index] = mutation
    }

    override suspend fun propagateServerMatchId(clientMatchUuid: String, serverMatchId: Int) {
        entries.replaceAll {
            if (it.clientMatchUuid == clientMatchUuid &&
                it.status in listOf("PENDING", "IN_FLIGHT") &&
                it.serverMatchId == null
            ) it.copy(serverMatchId = serverMatchId)
            else it
        }
    }

    override suspend fun deleteDone() {
        entries.removeAll { it.status == "DONE" }
    }

    override suspend fun hasPending(): Boolean =
        entries.any { it.status in listOf("PENDING", "IN_FLIGHT") }

    fun allEntries(): List<OutboxMutationEntity> = entries.toList()
}

private class TestApiClient : MatchApiClient {
    val createResults = mutableListOf<Response<MatchDto>?>()
    val updateResults = mutableListOf<Response<MatchDto>?>()
    val finishResults = mutableListOf<Response<MatchDto>?>()
    val eventResults = mutableListOf<Response<MatchEventResponseDto>?>()
    val statisticsResults = mutableListOf<Response<Unit>?>()
    val operations = mutableListOf<String>()

    override suspend fun createMatch(match: MatchDto): Response<MatchDto> {
        operations += "create"
        return createResults.removeAt(0) ?: throw IOException("network error")
    }

    override suspend fun updateMatch(matchId: Int, match: MatchDto): Response<MatchDto> {
        operations += "update:$matchId"
        return updateResults.removeAt(0) ?: throw IOException("network error")
    }

    override suspend fun finishMatch(matchId: Int, request: FinishMatchRequest): Response<MatchDto> {
        operations += "finish:$matchId"
        return finishResults.removeAt(0) ?: throw IOException("network error")
    }

    override suspend fun logMatchEvent(event: MatchEventDto): Response<MatchEventResponseDto> {
        operations += "event:${event.eventType}"
        return eventResults.removeAt(0) ?: throw IOException("network error")
    }

    override suspend fun sendMatchStatistics(statistics: MatchStatisticsRequestDto): Response<Unit> {
        operations += "statistics"
        return statisticsResults.removeAt(0) ?: throw IOException("network error")
    }
}

private object StubHistorySaver : MatchHistorySaver {
    override suspend fun saveMatch(state: MatchState): Long = 1
}

private object ImmediateRetryDelay : RetryDelay {
    override suspend fun waitBeforeNextAttempt(attemptNumber: Int) {}
}

private object SilentLogger : MatchSyncLogger {
    override fun api(endpoint: String, result: String) = Unit
    override fun error(context: String, error: Throwable) = Unit
    override fun error(context: String, message: String) = Unit
}

private fun <T> httpError(code: Int): Response<T> {
    return Response.error(code, "error".toResponseBody("text/plain".toMediaType()))
}
