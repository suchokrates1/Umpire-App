package pl.vestmedia.tennisreferee.ui.settings

data class SettingsDiagnosticsInfo(
    val appVersion: String,
    val backendUrl: String,
    val device: String,
    val locale: String,
    val timezone: String,
    val syncStatus: String,
    val lastSyncUpdate: String,
    val lastError: String
)

object SettingsDiagnosticsFormatter {
    fun clipboardText(info: SettingsDiagnosticsInfo): String {
        return listOf(
            "App version: ${info.appVersion}",
            "Backend URL: ${info.backendUrl}",
            "Device: ${info.device}",
            "Locale: ${info.locale}",
            "Timezone: ${info.timezone}",
            "Last sync status: ${info.syncStatus}",
            "Last sync update: ${info.lastSyncUpdate}",
            "Last sync error: ${info.lastError}"
        ).joinToString(separator = "\n")
    }
}