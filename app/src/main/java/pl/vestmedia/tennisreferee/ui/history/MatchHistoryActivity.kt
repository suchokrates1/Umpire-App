package pl.vestmedia.tennisreferee.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ActivityMatchHistoryBinding

/**
 * Ekran historii rozegranych meczów
 */
class MatchHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMatchHistoryBinding
    private val viewModel: MatchHistoryViewModel by viewModels()
    private lateinit var adapter: MatchHistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        observeMatches()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.match_history_title)
    }
    
    private fun setupRecyclerView() {
        adapter = MatchHistoryAdapter(
            onMatchClick = { match ->
                // Otwórz szczegóły meczu
                val intent = Intent(this, MatchDetailActivity::class.java).apply {
                    putExtra("MATCH_ID", match.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { match ->
                showDeleteConfirmation(match)
            }
        )
        
        binding.recyclerViewMatches.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMatches.adapter = adapter
    }
    
    private fun observeMatches() {
        viewModel.allMatches.observe(this) { matches ->
            adapter.submitList(matches)
            
            if (matches.isEmpty()) {
                binding.textViewEmpty.visibility = android.view.View.VISIBLE
                binding.recyclerViewMatches.visibility = android.view.View.GONE
            } else {
                binding.textViewEmpty.visibility = android.view.View.GONE
                binding.recyclerViewMatches.visibility = android.view.View.VISIBLE
            }
        }
    }
    
    private fun showDeleteConfirmation(match: pl.vestmedia.tennisreferee.data.database.MatchEntity) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_match)
            .setMessage(R.string.delete_match_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteMatch(match)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_match_history, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_delete_all -> {
                showDeleteAllConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showDeleteAllConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_all_matches)
            .setMessage(R.string.delete_all_matches_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteAllMatches()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
