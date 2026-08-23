package pl.vestmedia.tennisreferee.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pl.vestmedia.tennisreferee.data.auth.CourtSession
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider
import pl.vestmedia.tennisreferee.data.auth.InMemoryCourtSessionStore

class RetrofitClientStartupTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
        CourtSessionProvider.resetForTests()
        RetrofitClient.overrideBaseUrl(null)
    }

    @Test
    fun loadingRetrofitClientDoesNotRequireSessionStoreYet() {
        CourtSessionProvider.resetForTests()
        RetrofitClient.overrideBaseUrl("http://127.0.0.1:1/")
        assertEquals("http://127.0.0.1:1/", RetrofitClient.BASE_URL)
    }

    @Test
    fun apiServiceCanBeCreatedAfterSessionInitialize() {
        CourtSessionProvider.initializeForTests(InMemoryCourtSessionStore())
        assertNotNull(RetrofitClient.apiService)
    }

    @Test
    fun bearerInterceptorLooksUpStoreLazilyOnEachRequest() {
        server.start()
        server.enqueue(MockResponse().setResponseCode(200))

        CourtSessionProvider.resetForTests()
        val interceptor = BearerAuthInterceptor({ CourtSessionProvider.get() }, nowMillis = { 1_000L })
        CourtSessionProvider.initializeForTests(
            InMemoryCourtSessionStore(
                CourtSession("1", token = "lazy-token", expiresAtMillis = 2_000L)
            )
        )

        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
            .newCall(Request.Builder().url(server.url("/api/matches")).build())
            .execute()
            .close()

        assertEquals("Bearer lazy-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun bearerInterceptorDoesNotCaptureAStoreFromBeforeInitialize() {
        server.start()
        server.enqueue(MockResponse().setResponseCode(200))

        val interceptor = BearerAuthInterceptor({ CourtSessionProvider.get() }, nowMillis = { 1_000L })
        CourtSessionProvider.initializeForTests(InMemoryCourtSessionStore())

        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
            .newCall(Request.Builder().url(server.url("/api/matches")).build())
            .execute()
            .close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
