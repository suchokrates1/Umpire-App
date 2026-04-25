package pl.vestmedia.tennisreferee.ui.history

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.database.MatchEntity
import pl.vestmedia.tennisreferee.databinding.ActivityMatchDetailBinding
import pl.vestmedia.tennisreferee.utils.AppLogger
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity wyświetlające szczegóły zakończonego meczu
 */
class MatchDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMatchDetailBinding
    
    companion object {
        const val EXTRA_MATCH_ID = "match_id"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.screen("MatchDetail")
        binding = ActivityMatchDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.match_details)
        
        val matchId = intent.getLongExtra(EXTRA_MATCH_ID, -1L)
        if (matchId <= 0L) {
            binding.textViewMatchDetails.setText(R.string.match_details_not_found)
            return
        }

        val repository = (application as TennisRefereeApp).matchHistoryRepository
        lifecycleScope.launch {
            val match = repository.getMatchById(matchId)
            if (match == null) {
                binding.textViewMatchDetails.setText(R.string.match_details_not_found)
            } else {
                setupViews(match)
            }
        }
    }
    
    private fun setupViews(match: MatchEntity) {
        binding.textViewMatchDetails.text = buildString {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            appendLine(getString(R.string.match_details_players, match.getPlayer1SideFullName(), match.getPlayer2SideFullName()))
            appendLine(getString(R.string.match_details_score, match.player1Sets, match.player2Sets))
            appendLine(getString(R.string.match_details_court, match.courtName))
            appendLine(getString(R.string.match_details_started, dateFormat.format(Date(match.matchStartTime))))
            appendLine(getString(R.string.match_details_duration, match.getFormattedDuration()))
            match.umpireName?.takeIf { it.isNotBlank() }?.let {
                appendLine(getString(R.string.match_details_umpire, it))
            }
            match.getWinnerName()?.let {
                appendLine(getString(R.string.winner_format, it))
            }

            appendLine()
            appendLine(getString(R.string.match_details_sets))
            match.setsHistory.forEach { set ->
                val suffix = if (set.isSuperTiebreak) " STB" else ""
                val tiebreak = set.tiebreakLoserPoints?.let { " ($it)" }.orEmpty()
                appendLine("${set.setNumber}. ${set.player1Games}:${set.player2Games}$tiebreak$suffix")
            }

            appendLine()
            appendLine(getString(R.string.statistics))
            appendLine(getString(R.string.match_details_stat_line, match.getPlayer1SideDisplayName(), match.player1Aces, match.player1DoubleFaults, match.player1Winners, match.getFirstServePercentage(true)))
            appendLine(getString(R.string.match_details_stat_line, match.getPlayer2SideDisplayName(), match.player2Aces, match.player2DoubleFaults, match.player2Winners, match.getFirstServePercentage(false)))
        }
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
