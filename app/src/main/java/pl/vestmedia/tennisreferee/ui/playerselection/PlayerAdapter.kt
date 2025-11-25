package pl.vestmedia.tennisreferee.ui.playerselection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ItemPlayerBinding
import pl.vestmedia.tennisreferee.data.model.Player

/**
 * Adapter dla listy zawodników
 */
class PlayerAdapter(
    private val onPlayerClick: (Player) -> Unit,
    private val isPlayerSelected: (Player) -> Boolean,
    private val getSelectionIndex: (Player) -> Int = { -1 },
    private val isDoublesMode: () -> Boolean = { false }
) : ListAdapter<Player, PlayerAdapter.PlayerViewHolder>(PlayerDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerViewHolder(binding, onPlayerClick, isPlayerSelected, getSelectionIndex, isDoublesMode)
    }
    
    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class PlayerViewHolder(
        private val binding: ItemPlayerBinding,
        private val onPlayerClick: (Player) -> Unit,
        private val isPlayerSelected: (Player) -> Boolean,
        private val getSelectionIndex: (Player) -> Int,
        private val isDoublesMode: () -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(player: Player) {
            binding.textPlayerName.text = player.getDisplayName()
            
            // Pokaż grupę jeśli dostępna
            if (player.group != null) {
                binding.textPlayerRanking.text = player.group
            } else {
                binding.textPlayerRanking.text = ""
            }
            
            // Pokaż flagę jeśli dostępna
            if (player.flag != null) {
                binding.textPlayerCountry.text = getFlagEmoji(player.flag)
            } else {
                binding.textPlayerCountry.text = ""
            }
            
            // Ustaw styl zaznaczenia: tło + powiększenie (bez checkboxa)
            val isSelected = isPlayerSelected(player)
            val selectionIndex = if (isSelected) getSelectionIndex(player) else -1
            val doubles = isDoublesMode()
            
            if (isSelected) {
                // Określ kolor drużyny w trybie debla
                val backgroundColor = when {
                    doubles && selectionIndex < 2 -> R.color.team1_color  // Drużyna 1 (gracze 0,1)
                    doubles && selectionIndex >= 2 -> R.color.team2_color // Drużyna 2 (gracze 2,3)
                    else -> R.color.player_selected // Singles lub brak info
                }
                
                binding.containerPlayer.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, backgroundColor)
                )
                
                // Lekkie powiększenie
                binding.cardPlayer.scaleX = 1.05f
                binding.cardPlayer.scaleY = 1.05f
                binding.cardPlayer.cardElevation = 8f
            } else {
                binding.containerPlayer.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.transparent)
                )
                
                // Normalna wielkość
                binding.cardPlayer.scaleX = 1.0f
                binding.cardPlayer.scaleY = 1.0f
                binding.cardPlayer.cardElevation = 2f
            }
            
            binding.cardPlayer.setOnClickListener {
                onPlayerClick(player)
            }
        }
        
        private fun getFlagEmoji(countryCode: String): String {
            val firstLetter = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
            val secondLetter = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
        }
    }
    
    private class PlayerDiffCallback : DiffUtil.ItemCallback<Player>() {
        override fun areItemsTheSame(oldItem: Player, newItem: Player): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean {
            return oldItem == newItem
        }
    }
}
