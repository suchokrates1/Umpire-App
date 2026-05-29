package pl.vestmedia.tennisreferee.data.api

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import pl.vestmedia.tennisreferee.BuildConfig
import java.util.Locale
import java.util.TimeZone

data class AndroidBuildInfo(
    val manufacturer: String,
    val model: String,
    val sdkInt: Int,
    val release: String
) {
    companion object {
        fun current(): AndroidBuildInfo {
            return AndroidBuildInfo(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                sdkInt = Build.VERSION.SDK_INT,
                release = Build.VERSION.RELEASE
            )
        }
    }
}

data class ClientMetadata(
    val platform: String,
    val appVersion: String,
    val appVersionCode: String,
    val device: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidSdk: String,
    val androidRelease: String,
    val locale: String,
    val country: String,
    val timezone: String
) {
    fun toHeaders(): Map<String, String> {
        return linkedMapOf(
            "X-TennisReferee-Platform" to platform.limit(50),
            "X-TennisReferee-App-Version" to appVersion.limit(50),
            "X-TennisReferee-App-Code" to appVersionCode.limit(20),
            "X-TennisReferee-Device" to device.limit(160),
            "X-TennisReferee-Manufacturer" to deviceManufacturer.limit(80),
            "X-TennisReferee-Model" to deviceModel.limit(120),
            "X-TennisReferee-Android-Sdk" to androidSdk.limit(20),
            "X-TennisReferee-Android-Release" to androidRelease.limit(40),
            "X-TennisReferee-Locale" to locale.limit(40),
            "X-TennisReferee-Country" to country.limit(10),
            "X-TennisReferee-Timezone" to timezone.limit(80)
        )
    }

    private fun String.limit(maxLength: Int): String = take(maxLength)
}

class DeviceInfoProvider(
    private val localeProvider: () -> Locale = { Locale.getDefault() },
    private val timeZoneProvider: () -> TimeZone = { TimeZone.getDefault() },
    private val buildInfoProvider: () -> AndroidBuildInfo = { AndroidBuildInfo.current() },
    private val appVersionName: String = BuildConfig.VERSION_NAME,
    private val appVersionCode: String = BuildConfig.VERSION_CODE.toString()
) {
    fun current(): ClientMetadata {
        val locale = localeProvider()
        val buildInfo = buildInfoProvider()
        val device = listOf(buildInfo.manufacturer, buildInfo.model)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return ClientMetadata(
            platform = "android",
            appVersion = appVersionName,
            appVersionCode = appVersionCode,
            device = device,
            deviceManufacturer = buildInfo.manufacturer,
            deviceModel = buildInfo.model,
            androidSdk = buildInfo.sdkInt.toString(),
            androidRelease = buildInfo.release,
            locale = locale.toLanguageTag(),
            country = locale.country,
            timezone = timeZoneProvider().id
        )
    }
}

class ClientMetadataInterceptor(
    private val metadataProvider: () -> ClientMetadata = { DeviceInfoProvider().current() }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        metadataProvider().toHeaders().forEach { (name, value) ->
            builder.header(name, value)
        }
        return chain.proceed(builder.build())
    }
}