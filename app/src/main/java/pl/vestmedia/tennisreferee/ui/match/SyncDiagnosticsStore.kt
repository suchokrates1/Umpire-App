package pl.vestmedia.tennisreferee.ui.match

import android.content.Context

data class SyncDiagnosticsSnapshot(
    val status: SyncStatus,
    val lastError: String?,
    val lastUpdatedAtMillis: Long
)

class SyncDiagnosticsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun record(status: SyncStatus, errorMessage: String? = null) {
        preferences.edit()
            .putString(KEY_STATUS, status.name)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply {
                val sanitizedError = errorMessage?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_ERROR_LENGTH)
                if (sanitizedError == null) remove(KEY_LAST_ERROR) else putString(KEY_LAST_ERROR, sanitizedError)
            }
            .apply()
    }

    fun current(): SyncDiagnosticsSnapshot {
        val status = preferences.getString(KEY_STATUS, SyncStatus.IDLE.name)
            ?.let { rawStatus -> runCatching { SyncStatus.valueOf(rawStatus) }.getOrDefault(SyncStatus.IDLE) }
            ?: SyncStatus.IDLE

        return SyncDiagnosticsSnapshot(
            status = status,
            lastError = preferences.getString(KEY_LAST_ERROR, null),
            lastUpdatedAtMillis = preferences.getLong(KEY_UPDATED_AT, 0L)
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "sync_diagnostics"
        const val KEY_STATUS = "status"
        const val KEY_LAST_ERROR = "last_error"
        const val KEY_UPDATED_AT = "updated_at"
        const val MAX_ERROR_LENGTH = 500
    }
}