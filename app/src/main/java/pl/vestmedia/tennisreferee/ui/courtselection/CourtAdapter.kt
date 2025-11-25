package pl.vestmedia.tennisreferee.ui.courtselection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ItemCourtBinding
import pl.vestmedia.tennisreferee.data.model.Court

/**
 * Adapter dla listy kortów
 */
class CourtAdapter(
    private val onCourtClick: (Court) -> Unit
) : ListAdapter<Court, CourtAdapter.CourtViewHolder>(CourtDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourtViewHolder {
        val binding = ItemCourtBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourtViewHolder(binding, onCourtClick)
    }
    
    override fun onBindViewHolder(holder: CourtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class CourtViewHolder(
        private val binding: ItemCourtBinding,
        private val onCourtClick: (Court) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(court: Court) {
            binding.textCourtName.text = court.getDisplayName(binding.root.context)
            
            // Ustaw status kortu
            if (court.isAvailable) {
                binding.textCourtStatus.text = binding.root.context.getString(R.string.court_available)
                binding.cardCourt.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.court_available)
                )
            } else {
                binding.textCourtStatus.text = binding.root.context.getString(R.string.court_occupied)
                binding.cardCourt.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.court_occupied)
                )
            }
            
            binding.cardCourt.setOnClickListener {
                onCourtClick(court)
            }
        }
    }
    
    private class CourtDiffCallback : DiffUtil.ItemCallback<Court>() {
        override fun areItemsTheSame(oldItem: Court, newItem: Court): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Court, newItem: Court): Boolean {
            return oldItem == newItem
        }
    }
}
