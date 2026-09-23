package pl.vestmedia.tennisreferee.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pl.vestmedia.tennisreferee.BuildConfig
import pl.vestmedia.tennisreferee.data.api.dto.ApiJsonConverterFactory
import pl.vestmedia.tennisreferee.data.auth.CourtSessionStore
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * HTTP client for one [pl.vestmedia.tennisreferee.AppContainer].
 * The session store is read on each request, not when this object is created.
 */
class RetrofitClient(
    private val sessionStore: () -> CourtSessionStore,
    initialBaseUrl: String? = null,
) {

    @Volatile
    private var baseUrlOverride: String? = initialBaseUrl
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { normalizeBaseUrl(it) }

    @Volatile
    private var cachedUrl: String? = null

    @Volatile
    private var cachedService: TennisApiService? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(ClientMetadataInterceptor())
        .addInterceptor(BearerAuthInterceptor(sessionStore))
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Effective backend base URL (always trailing slash). */
    val baseUrl: String
        get() = normalizeBaseUrl(baseUrlOverride ?: DEFAULT_BASE_URL)

    /**
     * Point this client at another host (E2E / staging).
     * Pass null to restore [DEFAULT_BASE_URL].
     */
    @Synchronized
    fun overrideBaseUrl(url: String?) {
        baseUrlOverride = url?.trim()?.takeIf { it.isNotEmpty() }?.let { normalizeBaseUrl(it) }
        cachedUrl = null
        cachedService = null
    }

    val apiService: TennisApiService
        get() {
            val url = baseUrl
            cachedService?.let { existing ->
                if (cachedUrl == url) return existing
            }
            synchronized(this) {
                cachedService?.let { existing ->
                    if (cachedUrl == url) return existing
                }
                val retrofit = Retrofit.Builder()
                    .baseUrl(url)
                    .client(okHttpClient)
                    .addConverterFactory(ApiJsonConverterFactory())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val created = retrofit.create(TennisApiService::class.java)
                cachedService = created
                cachedUrl = url
                return created
            }
        }

    private fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://score.vestmedia.pl/"
    }
}
