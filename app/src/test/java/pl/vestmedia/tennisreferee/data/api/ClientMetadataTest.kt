package pl.vestmedia.tennisreferee.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class ClientMetadataTest {
    @Test
    fun deviceInfoProviderBuildsBackendAuditHeaders() {
        val provider = DeviceInfoProvider(
            localeProvider = { Locale.forLanguageTag("pl-PL") },
            timeZoneProvider = { TimeZone.getTimeZone("Europe/Warsaw") },
            buildInfoProvider = {
                AndroidBuildInfo(
                    manufacturer = "Samsung",
                    model = "SM-X200",
                    sdkInt = 35,
                    release = "15"
                )
            },
            appVersionName = "1.0.0-dev.19",
            appVersionCode = "100019"
        )

        val headers = provider.current().toHeaders()

        assertEquals("android", headers["X-TennisReferee-Platform"])
        assertEquals("1.0.0-dev.19", headers["X-TennisReferee-App-Version"])
        assertEquals("100019", headers["X-TennisReferee-App-Code"])
        assertEquals("Samsung SM-X200", headers["X-TennisReferee-Device"])
        assertEquals("Samsung", headers["X-TennisReferee-Manufacturer"])
        assertEquals("SM-X200", headers["X-TennisReferee-Model"])
        assertEquals("35", headers["X-TennisReferee-Android-Sdk"])
        assertEquals("15", headers["X-TennisReferee-Android-Release"])
        assertEquals("pl-PL", headers["X-TennisReferee-Locale"])
        assertEquals("PL", headers["X-TennisReferee-Country"])
        assertEquals("Europe/Warsaw", headers["X-TennisReferee-Timezone"])
    }

    @Test
    fun clientMetadataLimitsHeaderValuesToBackendContract() {
        val metadata = ClientMetadata(
            platform = "p".repeat(60),
            appVersion = "v".repeat(60),
            appVersionCode = "c".repeat(30),
            device = "d".repeat(200),
            deviceManufacturer = "m".repeat(100),
            deviceModel = "o".repeat(150),
            androidSdk = "s".repeat(30),
            androidRelease = "r".repeat(60),
            locale = "l".repeat(60),
            country = "country-long",
            timezone = "t".repeat(100)
        )

        val headers = metadata.toHeaders()

        assertEquals(50, headers.getValue("X-TennisReferee-Platform").length)
        assertEquals(50, headers.getValue("X-TennisReferee-App-Version").length)
        assertEquals(20, headers.getValue("X-TennisReferee-App-Code").length)
        assertEquals(160, headers.getValue("X-TennisReferee-Device").length)
        assertEquals(80, headers.getValue("X-TennisReferee-Manufacturer").length)
        assertEquals(120, headers.getValue("X-TennisReferee-Model").length)
        assertEquals(20, headers.getValue("X-TennisReferee-Android-Sdk").length)
        assertEquals(40, headers.getValue("X-TennisReferee-Android-Release").length)
        assertEquals(40, headers.getValue("X-TennisReferee-Locale").length)
        assertEquals(10, headers.getValue("X-TennisReferee-Country").length)
        assertEquals(80, headers.getValue("X-TennisReferee-Timezone").length)
        assertTrue(headers.keys.all { it.startsWith("X-TennisReferee-") })
    }
}