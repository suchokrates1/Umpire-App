package pl.vestmedia.tennisreferee.data.api

import okhttp3.Interceptor
import okhttp3.Response
import pl.vestmedia.tennisreferee.data.auth.CourtSessionStore

/**
 * Adds the current court's bearer token to API requests. The PIN authorization endpoint
 * intentionally remains unauthenticated so a new court session can always be established.
 */
class BearerAuthInterceptor(
    private val sessionStore: CourtSessionStore,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authenticatedRequest = if (request.url.encodedPath.endsWith(AUTHORIZATION_PATH)) {
            request
        } else {
            val session = sessionStore.current()
            when {
                session == null -> request
                session.hasValidToken(nowMillis()) -> request.newBuilder()
                    .header(AUTHORIZATION_HEADER, "Bearer ${session.token}")
                    .build()
                else -> {
                    sessionStore.clear()
                    request
                }
            }
        }

        val response = chain.proceed(authenticatedRequest)
        if (response.code == 401) {
            sessionStore.clear()
        }
        return response
    }

    private companion object {
        const val AUTHORIZATION_PATH = "/authorize"
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
