package pl.vestmedia.tennisreferee.data.api

import android.os.Build
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pl.vestmedia.tennisreferee.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.TimeZone

/**
 * Object zapewniający singleton Retrofit client
 */
object RetrofitClient {
    
    private const val BASE_URL = "https://score.vestmedia.pl/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val clientMetadataInterceptor = Interceptor { chain ->
        val locale = Locale.getDefault()
        val device = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .take(160)

        val request = chain.request().newBuilder()
            .header("X-TennisReferee-Platform", "android")
            .header("X-TennisReferee-App-Version", BuildConfig.VERSION_NAME)
            .header("X-TennisReferee-App-Code", BuildConfig.VERSION_CODE.toString())
            .header("X-TennisReferee-Device", device)
            .header("X-TennisReferee-Manufacturer", Build.MANUFACTURER.take(80))
            .header("X-TennisReferee-Model", Build.MODEL.take(120))
            .header("X-TennisReferee-Android-Sdk", Build.VERSION.SDK_INT.toString())
            .header("X-TennisReferee-Android-Release", Build.VERSION.RELEASE.take(40))
            .header("X-TennisReferee-Locale", locale.toLanguageTag())
            .header("X-TennisReferee-Country", locale.country.take(10))
            .header("X-TennisReferee-Timezone", TimeZone.getDefault().id.take(80))
            .build()

        chain.proceed(request)
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(clientMetadataInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: TennisApiService = retrofit.create(TennisApiService::class.java)
}
