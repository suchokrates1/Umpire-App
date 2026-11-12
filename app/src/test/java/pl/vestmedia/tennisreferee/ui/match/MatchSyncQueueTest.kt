package pl.vestmedia.tennisreferee.ui.match

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.MatchApiPayloadFactory
import pl.vestmedia.tennisreferee.data.api.dto.MatchDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatisticsRequestDto
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.ActionType
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchAction
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.MatchStatistics
import pl.vestmedia.tennisreferee.domain.match.model.SetScore
import retrofit2.Response

class MatchSyncQueueTest {
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "DE")

    @Test
    fun detachedSnapshotsPreservePointSequenceAndPropagateResolvedMatchId() = runBlocking {
        val createGate = CompletableDeferred<Unit>()
        val createStarted = CompletableDeferred<Unit>()
        val api = QueueTestApiClient(createGate = createGate, createStarted = createStarted)
        val coordinator = MatchSyncCoordinator(
            apiClient = api,
            matchHistorySaver = object : MatchHistorySaver {
                override suspend fun saveMatch(state: MatchState): Long = 1L
            },
            batteryInfoProvider = { MatchBatteryInfo(level = 75, isCharging = false) },
            onSyncStatus = {},
            onBracketWarning = { _, _ -> },
            retryDelay = object : RetryDelay {
                override suspend fun waitBeforeNextAttempt(attemptNumber: Int) = Unit
            },
            logger = object : MatchSyncLogger {
                override fun api(endpoint: String, result: String) = Unit
                override fun error(context: String, error: Throwable) = Unit
                override fun error(context: String, message: String) = Unit
            },
        )
        val resolvedCallbacks = mutableListOf<Pair<String, Int>>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val queue = MatchSyncQueue(
                scope = scope,
                coordinator = coordinator,
                onServerMatchIdResolved = { uuid, id -> resolvedCallbacks += uuid to id },
            )
            val liveState = MatchState(
                clientMatchUuid = "seq-1",
                player1 = playerOne,
                player2 = playerTwo,
                courtId = "1",
                courtName = "Court 1",
            ).apply {
                actionsHistory += MatchAction(
                    actionType = ActionType.ACE,
                    previousPlayer1Points = 0,
                    previousPlayer2Points = 0,
                    previousPlayer1Games = 0,
                    previousPlayer2Games = 0,
                    previousPlayer1Sets = 0,
                    previousPlayer2Sets = 0,
                    previousIsPlayer1Serving = true,
                    previousIsFirstServe = true,
                    previousIsTiebreak = false,
                    previousIsSuperTiebreak = false,
                    previousSetsHistorySize = 0,
                    previousPlayer1Stats = MatchStatistics(),
                    previousPlayer2Stats = MatchStatistics(),
                    description = "Ace",
                )
            }

            // Enqueue initial CREATE while matchId is still null.
            val startSnapshot = liveState.detachedCopy(includeActionsHistory = false)
            assertTrue(startSnapshot.actionsHistory.isEmpty())
            queue.enqueueSync(startSnapshot)
            withTimeout(5_000) { createStarted.await() }

            // Mutate live UI state aggressively while CREATE is blocked on IO.
            liveState.player1Points = 1
            liveState.player1Stats.aces = 1
            queue.enqueueEvent(liveState.detachedCopy(includeActionsHistory = false), "point")

            liveState.player1Points = 0
            liveState.player1Games = 1
            liveState.setsHistory += SetScore(setNumber = 1, player1Games = 1, player2Games = 0)
            queue.enqueueSync(liveState.detachedCopy(includeActionsHistory = false))

            liveState.player1Sets = 2
            liveState.isMatchFinished = true
            val firstDone = queue.enqueueFinalizeOnce(liveState.detachedCopy(includeActionsHistory = false))
            // Second finalize for the same UUID must be coalesced into a harmless flush.
            val secondDone = queue.enqueueFinalizeOnce(liveState.detachedCopy(includeActionsHistory = false))

            // Unblock CREATE; everything behind it must run in exact order with resolved id = 501.
            createGate.complete(Unit)
            withTimeout(5_000) {
                firstDone.await()
                secondDone.await()
            }

            assertEquals(listOf("seq-1" to 501), resolvedCallbacks)
            assertEquals(
                listOf(
                    "create:pts=0,games=0",
                    "event:point:id=501,pts=1,aces=1",
                    "update:501:pts=0,games=1,setsHist=1",
                    "update:501:pts=0,games=1,setsHist=1",
                    "event:match_end:id=501,pts=0,aces=1",
                    "finish:501",
                    "stats:501",
                ),
                api.calls,
            )
        } finally {
            scope.cancel()
        }
    }

    private inner class QueueTestApiClient(
        private val createGate: CompletableDeferred<Unit>,
        private val createStarted: CompletableDeferred<Unit>,
    ) : MatchApiClient {
        val calls = mutableListOf<String>()

        override suspend fun createMatch(match: MatchDto): Response<MatchDto> {
            createStarted.complete(Unit)
            createGate.await()
            calls += "create:pts=${match.score.player1Points},games=${match.score.player1Games}"
            return Response.success(match.copy(id = 501))
        }

        override suspend fun updateMatch(matchId: Int, match: MatchDto): Response<MatchDto> {
            calls += "update:$matchId:pts=${match.score.player1Points},games=${match.score.player1Games},setsHist=${match.score.setsHistory.size}"
            return Response.success(match.copy(id = matchId))
        }

        override suspend fun finishMatch(matchId: Int, request: FinishMatchRequest): Response<MatchDto> {
            calls += "finish:$matchId"
            val sample = MatchApiPayloadFactory.toMatch(
                MatchState(player1 = playerOne, player2 = playerTwo, courtId = "1", courtName = "Court 1")
            ).copy(id = matchId)
            return Response.success(sample)
        }

        override suspend fun logMatchEvent(event: MatchEventDto): Response<MatchEventResponseDto> {
            calls += "event:${event.eventType}:id=${event.matchId},pts=${event.score.player1Points},aces=${event.stats?.player1Aces}"
            return Response.success(MatchEventResponseDto(success = true))
        }

        override suspend fun sendMatchStatistics(statistics: MatchStatisticsRequestDto): Response<Unit> {
            calls += "stats:${statistics.matchId}"
            return Response.success(Unit)
        }
    }
}
