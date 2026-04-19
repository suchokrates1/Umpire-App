package pl.vestmedia.tennisreferee.ui.language

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ActivityLanguageSelectionBinding
import pl.vestmedia.tennisreferee.data.model.Language
import pl.vestmedia.tennisreferee.ui.courtselection.CourtSelectionActivity
import pl.vestmedia.tennisreferee.utils.AppLogger
import java.util.Locale

/**
 * Activity do wyboru języka aplikacji
 */
class LanguageSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLanguageSelectionBinding
    private lateinit var adapter: LanguageAdapter
    
    companion object {
        private const val PREFS_NAME = "TennisRefereePrefs"
        private const val KEY_LANGUAGE = "selected_language"
        
        fun getSelectedLanguage(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        }
        
        fun setLanguage(context: Context, languageCode: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
            
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
        
        fun hasLanguageSelected(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.contains(KEY_LANGUAGE)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.screen("LanguageSelection")
        binding = ActivityLanguageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.title = getString(R.string.app_name)
        
        setupRecyclerView()
    }
    
    private fun setupRecyclerView() {
        val languages = listOf(
            Language("de", "Deutsch", "🇩🇪"),
            Language("en", "English", "🇬🇧"),
            Language("es", "Español", "🇪🇸"),
            Language("fr", "Français", "🇫🇷"),
            Language("it", "Italiano", "🇮🇹"),
            Language("pl", "Polski", "🇵🇱")
        )
        
        adapter = LanguageAdapter(languages) { language ->
            onLanguageSelected(language)
        }
        
        binding.recyclerViewLanguages.apply {
            layoutManager = LinearLayoutManager(this@LanguageSelectionActivity)
            adapter = this@LanguageSelectionActivity.adapter
        }
    }
    
    private fun onLanguageSelected(language: Language) {
        AppLogger.button("LanguageSelection", "language", language.code)
        setLanguage(this, language.code)
        
        // Przejdź do ekranu wyboru kortów
        val intent = Intent(this, CourtSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
