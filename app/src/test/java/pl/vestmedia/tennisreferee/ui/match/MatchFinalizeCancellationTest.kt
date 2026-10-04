package pl.vestmedia.tennisreferee.ui.match

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

/**
 * The umpire closes the screen while the finished match is still being sent.
 *
 * Whatever was in flight must not be lost: it goes to the outbox, which sends it later.
 * The outbox is Room, and Room refuses a write from a cancelled coroutine, so the
 * store here does the same.
 */
class MatchFinalizeCancellationTest {

    @Test
    fun aFinishCutShortByClosingTheScreenWaitsInTheOutbox() = runBlocking {
        val finishStarted = CompletableDeferred<Unit>()
        val api = HangingFinishApiClient(finishStarted)
        val store = RoomLikeOutboxStore()
        val errors = mutableListOf<String>()
        val coordinator = MatchSyncCoordinator(
            apiClient = api,
            matchHistorySaver = object : MatchHistorySaver {
                override suspend fun saveMatch(state: MatchState): Long = 1
            },
            batteryInfoProvider = { MatchBatteryInfo(level = 50, isCharging = false) },
            onSyncStatus = {},
            onBracketWarning = { _, _ -> },
            retryDelay = object : RetryDelay {
                override suspend fun waitBeforeNextAttempt(attemptNumber: Int) = Unit
            },
            logger = object : MatchSyncLogger {
                override fun api(endpoint: String, result: String) = Unit
                override fun error(context: String, error: Throwable) { errors += "$context: ${error.message}" }
                override fun error(context: String, message: String) { errors += "$context: $message" }
            },
            outboxFlusher = MatchOutboxFlusher(store, api),
        )
        val state = MatchState(
            player1 = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL"),
            player2 = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "DE"),
            courtId = "1",
            courtName = "Court 1",
        ).apply { matchId = 7 }

        val screen = CoroutineScope(Job() + Dispatchers.Default)
        val finalize = screen.launch { coordinator.finalizeMatch(state) }
        finishStarted.await()
        finalize.cancelAndJoin()

        assertEquals(listOf("FINISH" to 7), store.saved.map { it.type to it.serverMatchId })
        assertTrue("closing the screen is not a network error: $errors", errors.none { "finish" in it })
    }
}

private class HangingFinishApiClient(private val finishStarted: CompletableDeferred<Unit>) : MatchApiClient {
    override suspend fun createMatch(match: MatchDto): Response<MatchDto> = error("the match already has an id")

    override suspend fun updateMatch(matchId: Int, match: MatchDto): Response<MatchDto> = Response.success(match)

    override suspend fun finishMatch(matchId: Int, request: FinishMatchRequest): Response<MatchDto> {
        finishStarted.complete(Unit)
        awaitCancellation()
    }

    override suspend fun logMatchEvent(event: MatchEventDto): Response<MatchEventResponseDto> =
        Response.success(MatchEventResponseDto(success = true))

    override suspend fun sendMatchStatistics(statistics: MatchStatisticsRequestDto): Response<Unit> =
        Response.success(Unit)
}

private class RoomLikeOutboxStore : MatchOutboxStore {
    val saved = mutableListOf<OutboxMutationEntity>()

    override suspend fun enqueue(mutation: OutboxMutationEntity): Long {
        currentCoroutineContext().ensureActive()
        saved += mutation
        return saved.size.toLong()
    }

    override suspend fun getPending(): List<OutboxMutationEntity> = emptyList()
    override suspend fun update(mutation: OutboxMutationEntity) = Unit
    override suspend fun propagateServerMatchId(clientMatchUuid: String, serverMatchId: Int) = Unit
    override suspend fun deleteDone() = Unit
    override suspend fun hasPending(): Boolean = false
    override suspend fun dropPendingUpdates(clientMatchUuid: String) = Unit
}
