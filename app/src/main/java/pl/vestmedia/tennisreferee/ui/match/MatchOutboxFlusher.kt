package pl.vestmedia.tennisreferee.ui.match

import com.google.gson.Gson
import pl.vestmedia.tennisreferee.data.api.dto.MatchDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatisticsRequestDto
import pl.vestmedia.tennisreferee.data.database.OutboxMutationEntity
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import retrofit2.Response

data class FlushResult(val flushed: Int, val failed: Int, val stoppedOnAuth: Boolean = false)

class MatchOutboxFlusher(
    private val outboxStore: MatchOutboxStore,
    private val apiClient: MatchApiClient,
    private val logger: MatchSyncLogger = AppLoggerMatchSyncLogger,
    private val gson: Gson = Gson()
) {

    suspend fun flushPending(): FlushResult {
        val pending = outboxStore.getPending()
        if (pending.isEmpty()) return FlushResult(0, 0)

        val sorted = pending.sortedWith(compareBy(
            { if (it.type == "CREATE") 0 else 1 },
            { it.createdAt }
        ))

        val resolvedIds = mutableMapOf<String, Int>()
        var flushed = 0
        var failed = 0

        for (original in sorted) {
            val resolved = original.serverMatchId ?: resolvedIds[original.clientMatchUuid]
            val mutation = if (resolved != null) original.copy(serverMatchId = resolved) else original

            outboxStore.update(mutation.copy(
                status = "IN_FLIGHT",
                attempts = mutation.attempts + 1
            ))

            try {
                val result = executeMutation(mutation, resolvedIds)

                if (result == null) {
                    outboxStore.update(mutation.copy(
                        status = "PENDING",
                        attempts = mutation.attempts + 1,
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
                    return FlushResult(flushed, failed, stoppedOnAuth = true)
                } else {
                    outboxStore.update(mutation.copy(
                        status = "PENDING",
                        attempts = mutation.attempts + 1,
                        lastError = "HTTP ${result.code()}"
                    ))
                    failed++
                }
            } catch (e: Exception) {
                outboxStore.update(mutation.copy(
                    status = "PENDING",
                    attempts = mutation.attempts + 1,
                    lastError = e.message ?: "unknown"
                ))
                failed++
            }
        }

        outboxStore.deleteDone()
        return FlushResult(flushed, failed)
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
            "CREATE" -> {
                val dto = gson.fromJson(mutation.payloadJson, MatchDto::class.java)
                apiClient.createMatch(dto)
            }
            "UPDATE" -> {
                val serverId = mutation.serverMatchId
                    ?: resolvedIds[mutation.clientMatchUuid]
                    ?: return null
                val dto = gson.fromJson(mutation.payloadJson, MatchDto::class.java)
                apiClient.updateMatch(serverId, dto)
            }
            "FINISH" -> {
                val serverId = mutation.serverMatchId
                    ?: resolvedIds[mutation.clientMatchUuid]
                    ?: return null
                val request = gson.fromJson(mutation.payloadJson, FinishMatchRequest::class.java)
                apiClient.finishMatch(serverId, request)
            }
            "EVENT" -> {
                val dto = gson.fromJson(mutation.payloadJson, MatchEventDto::class.java)
                apiClient.logMatchEvent(dto)
            }
            "STATS" -> {
                val dto = gson.fromJson(mutation.payloadJson, MatchStatisticsRequestDto::class.java)
                apiClient.sendMatchStatistics(dto)
            }
            else -> null
        }
    }
}
