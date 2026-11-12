package pl.vestmedia.tennisreferee.ui.match

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import java.util.concurrent.ConcurrentHashMap

/**
 * Single sequential queue for every outgoing match network call (events, score updates,
 * finalize, outbox flush).
 *
 * Each operation receives an already-detached [MatchState] snapshot captured on the main
 * thread when the point was played. Because only one worker drains the channel:
 * - point events and PUT updates arrive on the server in the exact order the umpire tapped;
 * - a `CREATE` that resolves `serverMatchId` propagates it to every snapshot queued behind
 *   it for the same `clientMatchUuid`, so follow-up calls update the created row instead of
 *   minting duplicates or racing on `state.matchId`;
 * - the queue lives on an application-backed scope, so closing the match screen never cuts
 *   an in-flight point or finalize short.
 */
class MatchSyncQueue(
    scope: CoroutineScope,
    private val coordinator: MatchSyncCoordinator,
    private val onServerMatchIdResolved: (clientMatchUuid: String, serverMatchId: Int) -> Unit = { _, _ -> },
) {
    private sealed interface Task {
        data class Event(val snapshot: MatchState, val eventType: String) : Task
        data class Sync(val snapshot: MatchState) : Task
        data class Finalize(
            val snapshot: MatchState,
            val request: FinishMatchRequest?,
            val done: CompletableDeferred<Unit>,
        ) : Task
        data class Flush(val done: CompletableDeferred<Unit>) : Task
    }

    private val channel = Channel<Task>(Channel.UNLIMITED)
    private val resolvedMatchIds = ConcurrentHashMap<String, Int>()
    private val finalizedUuids = ConcurrentHashMap.newKeySet<String>()

    init {
        scope.launch {
            for (task in channel) {
                process(task)
            }
        }
    }

    fun enqueueEvent(snapshot: MatchState, eventType: String) {
        channel.trySend(Task.Event(snapshot, eventType))
    }

    fun enqueueSync(snapshot: MatchState) {
        channel.trySend(Task.Sync(snapshot))
    }

    /**
     * Enqueues finalize once per `clientMatchUuid`. If finalize was already enqueued for
     * this match, flushes any remaining outbox rows instead of sending a duplicate finish,
     * and completes the returned deferred once the queue has caught up to that point.
     */
    fun enqueueFinalizeOnce(
        snapshot: MatchState,
        request: FinishMatchRequest? = null,
    ): CompletableDeferred<Unit> {
        val done = CompletableDeferred<Unit>()
        val task = if (finalizedUuids.add(snapshot.clientMatchUuid)) {
            Task.Finalize(snapshot, request, done)
        } else {
            Task.Flush(done)
        }
        // a closed queue takes nothing more; the caller must not wait for it forever
        if (channel.trySend(task).isFailure) done.complete(Unit)
        return done
    }

    fun enqueueFlush(): CompletableDeferred<Unit> {
        val done = CompletableDeferred<Unit>()
        if (channel.trySend(Task.Flush(done)).isFailure) done.complete(Unit)
        return done
    }

    /**
     * No more work will be queued: the match screen is gone. What is already queued is
     * still sent, then the worker ends. Without this the worker waits on the channel for
     * as long as the app lives and keeps its ViewModel with it, one per match played.
     */
    fun close() {
        channel.close()
    }

    private suspend fun process(task: Task) {
        when (task) {
            is Task.Event -> {
                val prepared = attachKnownMatchId(task.snapshot)
                runSafely { coordinator.logMatchEvent(prepared, task.eventType) }
            }
            is Task.Sync -> {
                val prepared = attachKnownMatchId(task.snapshot)
                runSafely { coordinator.syncMatch(prepared) }
                rememberResolvedMatchId(prepared)
            }
            is Task.Finalize -> {
                try {
                    val prepared = attachKnownMatchId(task.snapshot)
                    if (task.request != null) {
                        coordinator.finalizeMatch(prepared, task.request)
                    } else {
                        coordinator.finalizeMatch(prepared)
                    }
                    rememberResolvedMatchId(prepared)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // MatchSyncCoordinator already logs and enqueues to Room outbox.
                } finally {
                    task.done.complete(Unit)
                }
            }
            is Task.Flush -> {
                try {
                    coordinator.flushOutbox()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Outbox flush errors are already logged inside coordinator.
                } finally {
                    task.done.complete(Unit)
                }
            }
        }
    }

    private fun attachKnownMatchId(snapshot: MatchState): MatchState {
        if (snapshot.matchId != null) {
            resolvedMatchIds[snapshot.clientMatchUuid] = snapshot.matchId!!
            return snapshot
        }
        resolvedMatchIds[snapshot.clientMatchUuid]?.let { knownId ->
            snapshot.matchId = knownId
        }
        return snapshot
    }

    private fun rememberResolvedMatchId(snapshot: MatchState) {
        val id = snapshot.matchId ?: return
        val previous = resolvedMatchIds.put(snapshot.clientMatchUuid, id)
        if (previous != id) {
            onServerMatchIdResolved(snapshot.clientMatchUuid, id)
        }
    }

    private suspend inline fun runSafely(block: () -> Unit) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // MatchSyncCoordinator handles logging and outbox fallback.
        }
    }
}
