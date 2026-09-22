package pl.vestmedia.tennisreferee.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.database.MatchEntity
import pl.vestmedia.tennisreferee.databinding.ItemMatchHistoryBinding

/**
 * Adapter dla listy meczów w historii
 */
class MatchHistoryAdapter(
    private val onMatchClick: (MatchEntity) -> Unit,
    private val onDeleteClick: (MatchEntity) -> Unit
) : ListAdapter<MatchEntity, MatchHistoryAdapter.MatchViewHolder>(MatchDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemMatchHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MatchViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class MatchViewHolder(
        private val binding: ItemMatchHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(match: MatchEntity) {
            binding.apply {
                // Gracze
                textViewPlayer1.text = match.getPlayer1SideDisplayName()
                textViewPlayer2.text = match.getPlayer2SideDisplayName()
                
                // Wynik
                textViewScore.text = root.context.getString(
                    R.string.match_score_format,
                    match.player1Sets,
                    match.player2Sets
                )
                
                // Data
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                textViewDate.text = dateFormat.format(Date(match.matchStartTime))
                
                // Czas trwania
                textViewDuration.text = match.getFormattedDuration()
                
                // Kort
                textViewCourt.text = match.courtName
                
                // Zwycięzca
                val winnerId = match.winnerId
                if (winnerId != null) {
                    val winnerName = match.getWinnerName()
                    textViewWinner.text = root.context.getString(R.string.winner_format, winnerName)
                    textViewWinner.visibility = View.VISIBLE
                } else {
                    textViewWinner.visibility = View.GONE
                }
                
                // Click listeners
                root.setOnClickListener { onMatchClick(match) }
                buttonDelete.setOnClickListener { onDeleteClick(match) }
            }
        }
    }
    
    private class MatchDiffCallback : DiffUtil.ItemCallback<MatchEntity>() {
        override fun areItemsTheSame(oldItem: MatchEntity, newItem: MatchEntity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: MatchEntity, newItem: MatchEntity): Boolean {
            return oldItem == newItem
        }
    }
}
