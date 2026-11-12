package pl.vestmedia.tennisreferee.data.repository

import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.api.TennisApiService
import pl.vestmedia.tennisreferee.data.api.dto.CourtAuthResponseDto
import pl.vestmedia.tennisreferee.data.auth.InMemoryCourtSessionStore
import pl.vestmedia.tennisreferee.data.model.CourtPinFailure
import retrofit2.Response

class TennisRepositoryCourtPinTest {

    @Test
    fun wrongPinReturnsInvalidPinWithoutRetry() = runBlocking {
        var calls = 0
        val api = fakeApi {
            calls++
            Response.error(
                403,
                """{"ok":false,"authorized":false,"error":"invalid-pin"}"""
                    .toResponseBody("application/json".toMediaType())
            )
        }
        val repo = TennisRepository(InMemoryCourtSessionStore(), api)

        val result = repo.verifyCourtPin("t9-4", "1111")

        assertEquals(1, calls)
        assertTrue(result.exceptionOrNull() is CourtPinFailure.InvalidPin)
    }

    @Test
    fun throttledPinReturnsThrottledMinutesInSingleCall() = runBlocking {
        var calls = 0
        val raw = okhttp3.Response.Builder()
            .code(429)
            .message("")
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .headers(Headers.headersOf("Retry-After", "835"))
            .request(okhttp3.Request.Builder().url("https://test.blindtennis.app/api/courts/t9-4/authorize").build())
            .build()
        val api = fakeApi {
            calls++
            Response.error(
                """{"error":"Too many attempts"}""".toResponseBody("application/json".toMediaType()),
                raw
            )
        }
        val repo = TennisRepository(InMemoryCourtSessionStore(), api)

        val result = repo.verifyCourtPin("t9-4", "1111")

        assertEquals(1, calls)
        val failure = result.exceptionOrNull() as CourtPinFailure.Throttled
        assertEquals(14, failure.retryAfterMinutes)
    }

    private fun fakeApi(onVerify: () -> Response<CourtAuthResponseDto>): TennisApiService {
        return Proxy.newProxyInstance(
            TennisApiService::class.java.classLoader,
            arrayOf(TennisApiService::class.java)
        ) { _, method, _ ->
            if (method.name == "verifyCourtPin") {
                onVerify()
            } else {
                error("Unexpected call to ${method.name}")
            }
        } as TennisApiService
    }
}
