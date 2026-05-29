package pl.vestmedia.tennisreferee.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.api.DeviceInfoProvider
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.databinding.ActivitySettingsBinding
import pl.vestmedia.tennisreferee.ui.match.SyncDiagnosticsStore
import pl.vestmedia.tennisreferee.ui.match.SyncStatus
import pl.vestmedia.tennisreferee.utils.AppLogger
import pl.vestmedia.tennisreferee.utils.ThemeManager
import java.text.DateFormat
import java.util.Date

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var themeManager: ThemeManager
    private lateinit var syncDiagnosticsStore: SyncDiagnosticsStore
    private lateinit var diagnosticsInfo: SettingsDiagnosticsInfo
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.screen("Settings")
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        themeManager = (application as TennisRefereeApp).themeManager
        syncDiagnosticsStore = SyncDiagnosticsStore(this)
        
        setupToolbar()
        setupThemeSelection()
        setupVersionInfo()
        setupDiagnostics()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupThemeSelection() {
        // Set current theme selection
        when (themeManager.getTheme()) {
            ThemeManager.THEME_LIGHT -> binding.radioLight.isChecked = true
            ThemeManager.THEME_DARK -> binding.radioDark.isChecked = true
            ThemeManager.THEME_SYSTEM -> binding.radioSystem.isChecked = true
        }
        
        // Listen for theme changes
        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radioLight -> ThemeManager.THEME_LIGHT
                R.id.radioDark -> ThemeManager.THEME_DARK
                R.id.radioSystem -> ThemeManager.THEME_SYSTEM
                else -> ThemeManager.THEME_SYSTEM
            }
            AppLogger.button("Settings", "Theme", theme)
            themeManager.setTheme(theme)
        }
    }
    
    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.textVersion.text = getString(R.string.version_format, packageInfo.versionName)
        } catch (e: Exception) {
            binding.textVersion.text = getString(R.string.version_unknown)
        }
    }

    private fun setupDiagnostics() {
        val metadata = DeviceInfoProvider().current()
        val syncSnapshot = syncDiagnosticsStore.current()
        val appVersion = "${metadata.appVersion} (${metadata.appVersionCode})"
        val syncStatus = syncStatusLabel(syncSnapshot.status)
        val lastSyncUpdate = formatLastSyncUpdate(syncSnapshot.lastUpdatedAtMillis)
        val lastError = syncSnapshot.lastError ?: getString(R.string.diagnostics_no_error)

        diagnosticsInfo = SettingsDiagnosticsInfo(
            appVersion = appVersion,
            backendUrl = RetrofitClient.BASE_URL,
            device = metadata.device,
            locale = metadata.locale,
            timezone = metadata.timezone,
            syncStatus = syncStatus,
            lastSyncUpdate = lastSyncUpdate,
            lastError = lastError
        )

        binding.textDiagnosticsAppVersion.text = appVersion
        binding.textDiagnosticsBackend.text = RetrofitClient.BASE_URL
        binding.textDiagnosticsDevice.text = metadata.device
        binding.textDiagnosticsLocale.text = metadata.locale
        binding.textDiagnosticsTimezone.text = metadata.timezone
        binding.textDiagnosticsSyncStatus.text = syncStatus
        binding.textDiagnosticsLastUpdate.text = lastSyncUpdate
        binding.textDiagnosticsLastError.text = lastError
        binding.buttonCopyDiagnostics.setOnClickListener { copyDiagnostics() }
    }

    private fun copyDiagnostics() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                getString(R.string.diagnostics),
                SettingsDiagnosticsFormatter.clipboardText(diagnosticsInfo)
            )
        )
        Toast.makeText(this, R.string.diagnostics_copied, Toast.LENGTH_SHORT).show()
        AppLogger.button("Settings", "CopyDiagnostics")
    }

    private fun syncStatusLabel(status: SyncStatus): String {
        return when (status) {
            SyncStatus.IDLE -> getString(R.string.sync_status_idle)
            SyncStatus.SYNCING -> getString(R.string.sync_status_syncing)
            SyncStatus.SYNCED -> getString(R.string.sync_status_synced)
            SyncStatus.FAILED -> getString(R.string.sync_status_failed)
            SyncStatus.OFFLINE -> getString(R.string.sync_status_offline)
        }
    }

    private fun formatLastSyncUpdate(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return getString(R.string.diagnostics_never)
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(timestampMillis))
    }
}
