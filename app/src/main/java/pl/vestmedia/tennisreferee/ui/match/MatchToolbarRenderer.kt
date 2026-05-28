package pl.vestmedia.tennisreferee.ui.match

import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.R

class MatchToolbarRenderer(
    private val activity: AppCompatActivity
) {
    fun renderSyncStatus(status: SyncStatus) {
        activity.supportActionBar?.subtitle = when (status) {
            SyncStatus.IDLE -> null
            SyncStatus.SYNCING -> activity.getString(R.string.sync_status_syncing)
            SyncStatus.SYNCED -> activity.getString(R.string.sync_status_synced)
            SyncStatus.FAILED -> activity.getString(R.string.sync_status_failed)
            SyncStatus.OFFLINE -> activity.getString(R.string.sync_status_offline)
        }
    }
}