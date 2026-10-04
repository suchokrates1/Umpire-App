package pl.vestmedia.tennisreferee.ui.match

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.dto.apiJson
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.domain.match.model.MatchFinishReason

/**
 * A finish that could not be sent waits in the outbox as JSON, possibly across an app
 * update. Builds before 1.0.0-dev.53 wrote it with Gson; this one reads it with kotlinx.
 */
class OutboxPayloadFormatTest {

    private val retirement = FinishMatchRequest(
        finishReason = MatchFinishReason.RETIREMENT,
        winnerName = "Jan Kowalski",
        injuredPlayerName = "Adam Nowak",
        resultNote = "kontuzja kolana",
    )

    @Test
    fun aFinishQueuedByTheGsonBuildIsReadAfterTheUpdate() {
        val writtenBefore = Gson().toJson(retirement)
        assertEquals(retirement, apiJson.decodeFromString<FinishMatchRequest>(writtenBefore))
        assertEquals(FinishMatchRequest(), apiJson.decodeFromString<FinishMatchRequest>(Gson().toJson(FinishMatchRequest())))
    }

    @Test
    fun theOutboxNowWritesTheSameShapeGsonDid() {
        assertEquals(
            Gson().toJsonTree(retirement),
            Gson().fromJson(apiJson.encodeToString(FinishMatchRequest.serializer(), retirement), com.google.gson.JsonElement::class.java),
        )
    }
}
