package pl.vestmedia.tennisreferee.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.vestmedia.tennisreferee.data.auth.CourtSession
import pl.vestmedia.tennisreferee.data.auth.CourtSessionStore

class BearerAuthInterceptorTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun addsBearerTokenToNonAuthorizationRequests() {
        val store = FakeCourtSessionStore(
            CourtSession("1", token = "court-token", expiresAtMillis = 2_000L)
        )

        execute(store, "/api/matches", responseCode = 200)

        assertEquals("Bearer court-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun excludesAuthorizationEndpointFromBearerHeader() {
        val store = FakeCourtSessionStore(
            CourtSession("1", token = "court-token", expiresAtMillis = 2_000L)
        )

        execute(store, "/api/courts/1/authorize", responseCode = 200)

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun clearsExpiredSessionWithoutSendingToken() {
        val store = FakeCourtSessionStore(
            CourtSession("1", token = "court-token", expiresAtMillis = 999L)
        )

        execute(store, "/api/matches", responseCode = 200)

        assertNull(server.takeRequest().getHeader("Authorization"))
        assertNull(store.current())
    }

    @Test
    fun clearsSessionOnUnauthorizedResponseWithoutRetrying() {
        val store = FakeCourtSessionStore(
            CourtSession("1", token = "court-token", expiresAtMillis = 2_000L)
        )

        execute(store, "/api/matches", responseCode = 401)

        assertEquals(1, server.requestCount)
        assertNull(store.current())
    }

    @Test
    fun keepsSessionSavedWhileAnOlderRequestWasInFlight() {
        val store = FakeCourtSessionStore(null)
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()
        OkHttpClient.Builder()
            .addInterceptor(BearerAuthInterceptor(store, nowMillis = { 1_000L }))
            .addInterceptor { chain ->
                // a PIN authorization finishes while the tokenless request is on the wire
                store.save(CourtSession("1", token = "new-token", expiresAtMillis = 2_000L))
                chain.proceed(chain.request())
            }
            .build()
            .newCall(Request.Builder().url(server.url("/api/umpire-heartbeat")).build())
            .execute()
            .close()

        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals("new-token", store.current()?.token)
    }

    @Test
    fun keepsNewerTokenWhenAnOldTokenIsRejected() {
        val store = FakeCourtSessionStore(
            CourtSession("1", token = "old-token", expiresAtMillis = 2_000L)
        )
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()
        OkHttpClient.Builder()
            .addInterceptor(BearerAuthInterceptor(store, nowMillis = { 1_000L }))
            .addInterceptor { chain ->
                store.save(CourtSession("1", token = "new-token", expiresAtMillis = 2_000L))
                chain.proceed(chain.request())
            }
            .build()
            .newCall(Request.Builder().url(server.url("/api/matches")).build())
            .execute()
            .close()

        assertEquals("Bearer old-token", server.takeRequest().getHeader("Authorization"))
        assertEquals("new-token", store.current()?.token)
    }

    private fun execute(store: CourtSessionStore, path: String, responseCode: Int) {
        server.enqueue(MockResponse().setResponseCode(responseCode))
        server.start()
        OkHttpClient.Builder()
            .addInterceptor(BearerAuthInterceptor(store, nowMillis = { 1_000L }))
            .build()
            .newCall(Request.Builder().url(server.url(path)).build())
            .execute()
            .close()
    }

    private class FakeCourtSessionStore(
        private var session: CourtSession?
    ) : CourtSessionStore {
        override fun current(): CourtSession? = session

        override fun save(session: CourtSession) {
            this.session = session
        }

        override fun clear() {
            session = null
        }
    }
}
