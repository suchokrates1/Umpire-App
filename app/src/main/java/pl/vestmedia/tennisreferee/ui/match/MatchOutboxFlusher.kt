package pl.vestmedia.tennisreferee.ui.match

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pl.vestmedia.tennisreferee.data.api.dto.MatchDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatisticsRequestDto
import pl.vestmedia.tennisreferee.data.api.dto.apiJson
import pl.vestmedia.tennisreferee.data.database.OutboxMutationEntity
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import retrofit2.Response

data class FlushResult(
    val flushed: Int,
    val failed: Int,
    val dropped: Int = 0,
    val stoppedOnAuth: Boolean = false,
)

class MatchOutboxFlusher(
    private val outboxStore: MatchOutboxStore,
    private val apiClient: MatchApiClient,
    private val json: Json = apiJson,
) {
    companion object {
        /** Permanent client/server mismatches — retrying forever blocks match finalize. */
        private val PERMANENT_DROP_CODES = setOf(403, 404)

        /** How many times a request the server answers with a plain refusal (a 400) is tried. */
        const val MAX_REFUSED_ATTEMPTS = 5

        /** Refused for good: kept in the table for diagnosis, never sent again. */
        const val STATUS_FAILED = "FAILED"

        private fun isRetryable(code: Int): Boolean = code in 500..599 || code == 408 || code == 429

        /**
         * One flush at a time in the whole process. The match screen and the network
         * watcher each have a flusher over the same table; two passes at once would send
         * every row twice.
         */
        private val flushLock = Mutex()
    }

    suspend fun flushPending(): FlushResult = flushLock.withLock { flushPendingLocked() }

    private suspend fun flushPendingLocked(): FlushResult {
        val pending = outboxStore.getPending()
        if (pending.isEmpty()) return FlushResult(0, 0)

        val sorted = pending.sortedWith(compareBy(
            { if (it.type == "CREATE") 0 else 1 },
            { it.createdAt }
        ))

        val resolvedIds = mutableMapOf<String, Int>()
        var flushed = 0
        var failed = 0
        var dropped = 0

        for (original in sorted) {
            val resolved = original.serverMatchId ?: resolvedIds[original.clientMatchUuid]
            val mutation = if (resolved != null) original.copy(serverMatchId = resolved) else original

            // No waiting between rows: a flush runs in front of the next point, and every
            // flush is started by something the umpire did, never by a timer.
            outboxStore.update(mutation.copy(
                status = "IN_FLIGHT",
                attempts = mutation.attempts + 1
            ))

            try {
                val result = executeMutation(mutation, resolvedIds)

                if (result == null) {
                    // Not sent at all (its CREATE has not gone through yet): not an attempt.
                    outboxStore.update(mutation.copy(
                        status = "PENDING",
                        lastError = "no server match id"
                    ))
                    failed++
                    continue
                }

                if (result.isSuccessful) {
                    handleSuccess(mutation, result, resolvedIds)
                    flushed++
                } else if (result.code() == 401) {
                    outboxStore.update(mutation.copy(
                        status = "FAILED_AUTH",
                        lastError = "HTTP 401"
                    ))
                    failed++
                    outboxStore.deleteDone()
                    return FlushResult(flushed, failed, dropped, stoppedOnAuth = true)
                } else if (result.code() in PERMANENT_DROP_CODES) {
                    // Same as PWA outbox: 403/404 will never succeed (wrong court token,
                    // already-finished foreign match). Keep retrying only blocks finalize.
                    outboxStore.update(mutation.copy(
                        status = "DONE",
                        lastError = "HTTP ${result.code()} dropped"
                    ))
                    dropped++
                } else {
                    val attempts = mutation.attempts + 1
                    // The server refuses this request itself (a 400): the same answer would
                    // come every time, so it stops after a few tries instead of staying forever.
                    val givenUp = !isRetryable(result.code()) && attempts >= MAX_REFUSED_ATTEMPTS
                    outboxStore.update(mutation.copy(
                        status = if (givenUp) STATUS_FAILED else "PENDING",
                        attempts = attempts,
                        lastError = "HTTP ${result.code()}"
                    ))
                    failed++
                }
            } catch (e: CancellationException) {
                // the caller went away mid-request: the row must not stay IN_FLIGHT unnoticed
                withContext(NonCancellable) {
                    outboxStore.update(mutation.copy(status = "PENDING", lastError = "cancelled"))
                }
                throw e
            } catch (e: Exception) {
                outboxStore.update(mutation.copy(
                    status = "PENDING",
                    attempts = mutation.attempts + 1,
                    lastError = e.message ?: "unknown"
                ))
                // No answer at all means no network: the rows behind this one would each wait
                // for the same timeout. They go out with the next flush.
                failed += sorted.size - sorted.indexOf(original)
                break
            }
        }

        outboxStore.deleteDone()
        return FlushResult(flushed, failed, dropped)
    }

    suspend fun enqueue(
        clientMatchUuid: String,
        type: String,
        serverMatchId: Int?,
        payloadJson: String
    ): Long {
        return outboxStore.enqueue(OutboxMutationEntity(
            clientMatchUuid = clientMatchUuid,
            type = type,
            payloadJson = payloadJson,
            serverMatchId = serverMatchId
        ))
    }

    private suspend fun handleSuccess(
        mutation: OutboxMutationEntity,
        result: Response<*>,
        resolvedIds: MutableMap<String, Int>
    ) {
        if (mutation.type == "CREATE") {
            val serverId = (result.body() as? MatchDto)?.id
            outboxStore.update(mutation.copy(status = "DONE", serverMatchId = serverId))
            if (serverId != null) {
                resolvedIds[mutation.clientMatchUuid] = serverId
                outboxStore.propagateServerMatchId(mutation.clientMatchUuid, serverId)
            }
        } else {
            outboxStore.update(mutation.copy(status = "DONE"))
        }
    }

    private suspend fun executeMutation(
        mutation: OutboxMutationEntity,
        resolvedIds: Map<String, Int>
    ): Response<*>? {
        return when (mutation.type) {
            "CREATE" -> apiClient.createMatch(json.decodeFromString<MatchDto>(mutation.payloadJson))
            "UPDATE" -> {
                val serverId = mutation.serverMatchId
                    ?: resolvedIds[mutation.clientMatchUuid]
                    ?: return null
                apiClient.updateMatch(serverId, json.decodeFromString<MatchDto>(mutation.payloadJson))
            }
            "FINISH" -> {
                val serverId = mutation.serverMatchId
                    ?: resolvedIds[mutation.clientMatchUuid]
                    ?: return null
                val request = json.decodeFromString<FinishMatchRequest>(mutation.payloadJson)
                apiClient.finishMatch(serverId, request)
            }
            "EVENT" -> apiClient.logMatchEvent(json.decodeFromString<MatchEventDto>(mutation.payloadJson))
            "STATS" -> apiClient.sendMatchStatistics(json.decodeFromString<MatchStatisticsRequestDto>(mutation.payloadJson))
            else -> null
        }
    }
}
