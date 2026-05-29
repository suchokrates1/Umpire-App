package pl.vestmedia.tennisreferee.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDiagnosticsFormatterTest {
    @Test
    fun clipboardTextContainsSupportDiagnosticsInStableOrder() {
        val text = SettingsDiagnosticsFormatter.clipboardText(
            SettingsDiagnosticsInfo(
                appVersion = "1.0.0 (100)",
                backendUrl = "https://score.vestmedia.pl/",
                device = "Samsung SM-X200",
                locale = "pl-PL",
                timezone = "Europe/Warsaw",
                syncStatus = "Synced",
                lastSyncUpdate = "29.05.2026, 14:30:00",
                lastError = "No error"
            )
        )

        assertEquals(
            "App version: 1.0.0 (100)\n" +
                "Backend URL: https://score.vestmedia.pl/\n" +
                "Device: Samsung SM-X200\n" +
                "Locale: pl-PL\n" +
                "Timezone: Europe/Warsaw\n" +
                "Last sync status: Synced\n" +
                "Last sync update: 29.05.2026, 14:30:00\n" +
                "Last sync error: No error",
            text
        )
    }
}
