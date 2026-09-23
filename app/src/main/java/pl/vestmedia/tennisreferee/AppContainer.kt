package pl.vestmedia.tennisreferee

import android.content.Context
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.api.TennisApiService
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider
import pl.vestmedia.tennisreferee.data.auth.CourtSessionStore
import pl.vestmedia.tennisreferee.data.repository.TennisRepository

/**
 * Process-wide API client and court session, created once from [TennisRefereeApp.onCreate].
 * Screens read both from here. Tests retarget the backend through [overrideBaseUrl].
 */
class AppContainer(
    context: Context,
    initialBaseUrl: String?,
) {
    private val apiClient: RetrofitClient

    val sessionStore: CourtSessionStore
        get() = CourtSessionProvider.get()

    val apiService: TennisApiService
        get() = apiClient.apiService

    val baseUrl: String
        get() = apiClient.baseUrl

    init {
        CourtSessionProvider.initialize(context)
        apiClient = RetrofitClient(
            sessionStore = { CourtSessionProvider.get() },
            initialBaseUrl = initialBaseUrl,
        )
    }

    fun overrideBaseUrl(url: String?) {
        apiClient.overrideBaseUrl(url)
    }

    fun repository(): TennisRepository = TennisRepository(sessionStore, apiService)
}
