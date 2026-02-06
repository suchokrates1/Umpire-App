package pl.vestmedia.tennisreferee.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.databinding.ActivitySettingsBinding
import pl.vestmedia.tennisreferee.utils.ThemeManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var themeManager: ThemeManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        themeManager = (application as TennisRefereeApp).themeManager
        
        setupToolbar()
        setupThemeSelection()
        setupVersionInfo()
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
}
