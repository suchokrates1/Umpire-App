package pl.vestmedia.tennisreferee.ui.match

import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.MatchApiPayloadFactory
import pl.vestmedia.tennisreferee.data.api.dto.MatchDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatisticsRequestDto
import pl.vestmedia.tennisreferee.data.api.dto.apiJson
import pl.vestmedia.tennisreferee.data.database.OutboxMutationEntity
import pl.vestmedia.tennisreferee.data.model.MatchEventFactory
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import retrofit2.Response

class OutboxSharedFixturesTest {
    private val fixtureJson = Json { ignoreUnknownKeys = true }
    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski", flag = "PL")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak", flag = "DE")

    @Serializable
    private data class FixtureSuite(val scenarios: List<FixtureScenario>)

    @Serializable
    private data class FixtureScenario(
        val id: String,
        val description: String,
        val enqueue: List<FixtureEnqueue>,
        val flushes: List<FixtureFlush>,
    )

    @Serializable
    private data class FixtureEnqueue(
        val clientMatchUuid: String,
        val type: String,
        val serverMatchId: Int? = null,
    )

    @Serializable
    private data class FixtureFlush(
        val responses: List<FixtureResponse>,
        val expectCalls: List<FixtureCall>,
        val expectResult: FixtureResult,
        val expectPendingTypes: List<String>,
    )

    @Serializable
    private data class FixtureResponse(
        val status: Int = 200,
        val createdId: Int? = null,
        val networkError: Boolean = false,
    )

    @Serializable
    private data class FixtureCall(
        val type: String,
        val serverMatchId: Int? = null,
    )

    @Serializable
    private data class FixtureResult(
        val flushed: Int,
        val failed: Int,
        val dropped: Int,
        val stoppedOnAuth: Boolean,
    )

    @Test
    fun allSharedOutboxFixturesPassInKotlin() = runBlocking {
        val raw = javaClass.classLoader!!.getResource("outboxFixtures.json")!!.readText()
        val suite = fixtureJson.decodeFromString<FixtureSuite>(raw)
        assertTrue(suite.scenarios.isNotEmpty())

        for (scenario in suite.scenarios) {
            val store = FixtureOutboxStore()
            val api = FixtureApiClient()
            val flusher = MatchOutboxFlusher(store, api)

            for (item in scenario.enqueue) {
                store.enqueue(
                    OutboxMutationEntity(
                        clientMatchUuid = item.clientMatchUuid,
                        type = item.type,
                        payloadJson = payloadFor(item.type, item.clientMatchUuid),
                        serverMatchId = item.serverMatchId,
                        createdAt = store.nextTimestamp(),
                    )
                )
            }

            scenario.flushes.forEachIndexed { pass, step ->
                api.reset(step.responses)
                val result = flusher.flushPending()
                assertEquals("${scenario.id} pass $pass calls", step.expectCalls, api.calls)
                assertEquals(
                    "${scenario.id} pass $pass result",
                    step.expectResult,
                    FixtureResult(
                        flushed = result.flushed,
                        failed = result.failed,
                        dropped = result.dropped,
                        stoppedOnAuth = result.stoppedOnAuth,
                    )
                )
                assertEquals(
                    "${scenario.id} pass $pass pending",
                    step.expectPendingTypes,
                    store.getPending().map { it.type }
                )
            }
        }
    }

    private fun payloadFor(type: String, uuid: String): String {
        val state = MatchState(
            clientMatchUuid = uuid,
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "1",
            courtName = "Court 1",
        ).apply {
            matchId = 1
            isMatchFinished = true
            player1Sets = 2
        }
        return when (type) {
            "CREATE", "UPDATE" -> apiJson.encodeToString(MatchDto.serializer(), MatchApiPayloadFactory.toMatch(state))
            "FINISH" -> apiJson.encodeToString(FinishMatchRequest.serializer(), FinishMatchRequest())
            "EVENT" -> apiJson.encodeToString(
                MatchEventDto.serializer(),
                MatchEventFactory.create(state, "point", batteryLevel = 80, isCharging = false)
            )
            "STATS" -> apiJson.encodeToString(
                MatchStatisticsRequestDto.serializer(),
                MatchApiPayloadFactory.toStatisticsRequest(state)!!
            )
            else -> error("Unsupported fixture mutation type: $type")
        }
    }

    private class FixtureOutboxStore : MatchOutboxStore {
        private val waiting = setOf("PENDING", "IN_FLIGHT", "FAILED_AUTH")
        private val entries = mutableListOf<OutboxMutationEntity>()
        private var nextId = 1L
        private var clock = 1_000L

        fun nextTimestamp(): Long = ++clock

        override suspend fun enqueue(mutation: OutboxMutationEntity): Long {
            val id = nextId++
            val row = mutation.copy(id = id)
            entries += row
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
            entries.filter { it.status in waiting }.sortedBy { it.createdAt }

        override suspend fun update(mutation: OutboxMutationEntity) {
            val index = entries.indexOfFirst { it.id == mutation.id }
            if (index >= 0) entries[index] = mutation
        }

        override suspend fun propagateServerMatchId(clientMatchUuid: String, serverMatchId: Int) {
            entries.replaceAll {
                if (it.clientMatchUuid == clientMatchUuid && it.status in waiting && it.serverMatchId == null) {
                    it.copy(serverMatchId = serverMatchId)
                } else {
                    it
                }
            }
        }

        override suspend fun deleteDone() {
            entries.removeAll { it.status == "DONE" }
        }

        override suspend fun hasPending(): Boolean = entries.any { it.status in waiting }

        override suspend fun dropPendingUpdates(clientMatchUuid: String) {
            entries.removeAll {
                it.clientMatchUuid == clientMatchUuid && it.type == "UPDATE" && it.status in waiting
            }
        }
    }

    private inner class FixtureApiClient : MatchApiClient {
        private val scripted = ArrayDeque<FixtureResponse>()
        val calls = mutableListOf<FixtureCall>()

        fun reset(responses: List<FixtureResponse>) {
            scripted.clear()
            scripted.addAll(responses)
            calls.clear()
        }

        private fun nextStep(type: String, serverMatchId: Int?): FixtureResponse {
            calls += FixtureCall(type = type, serverMatchId = serverMatchId)
            val step = scripted.removeFirstOrNull() ?: error("Unexpected extra network call: $type")
            if (step.networkError) throw IOException("offline")
            return step
        }

        override suspend fun createMatch(match: MatchDto): Response<MatchDto> {
            val step = nextStep("CREATE", null)
            return if (step.status in 200..299) {
                Response.success(step.status, match.copy(id = step.createdId ?: 1))
            } else {
                errorResponse(step.status)
            }
        }

        override suspend fun updateMatch(matchId: Int, match: MatchDto): Response<MatchDto> {
            val step = nextStep("UPDATE", matchId)
            return if (step.status in 200..299) {
                Response.success(step.status, match.copy(id = matchId))
            } else {
                errorResponse(step.status)
            }
        }

        override suspend fun finishMatch(matchId: Int, request: FinishMatchRequest): Response<MatchDto> {
            val step = nextStep("FINISH", matchId)
            return if (step.status in 200..299) {
                val sample = MatchApiPayloadFactory.toMatch(
                    MatchState(player1 = playerOne, player2 = playerTwo, courtId = "1", courtName = "Court 1")
                ).copy(id = matchId)
                Response.success(step.status, sample)
            } else {
                errorResponse(step.status)
            }
        }

        override suspend fun logMatchEvent(event: MatchEventDto): Response<MatchEventResponseDto> {
            val step = nextStep("EVENT", event.matchId)
            return if (step.status in 200..299) {
                Response.success(step.status, MatchEventResponseDto(success = true))
            } else {
                errorResponse(step.status)
            }
        }

        override suspend fun sendMatchStatistics(statistics: MatchStatisticsRequestDto): Response<Unit> {
            val step = nextStep("STATS", statistics.matchId)
            return if (step.status in 200..299) {
                Response.success(step.status, Unit)
            } else {
                errorResponse(step.status)
            }
        }

        private fun <T> errorResponse(code: Int): Response<T> =
            Response.error(code, "error".toResponseBody("text/plain".toMediaType()))
    }
}
