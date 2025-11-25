package pl.vestmedia.tennisreferee.ui.history

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.database.MatchEntity
import pl.vestmedia.tennisreferee.databinding.ActivityMatchDetailBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity wyświetlające szczegóły zakończonego meczu
 */
class MatchDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMatchDetailBinding
    private lateinit var match: MatchEntity
    
    companion object {
        const val EXTRA_MATCH_ID = "match_id"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.match_details)
        
        // TODO: Załaduj mecz z bazy danych po ID
        // Na razie będziemy wyświetlać tylko podstawowe informacje
        // match = repository.getMatchById(matchId)
        
        setupViews()
    }
    
    private fun setupViews() {
        // TODO: Wypełnij widoki danymi meczu
        // Po zaimplementowaniu pobierania meczu z bazy
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
