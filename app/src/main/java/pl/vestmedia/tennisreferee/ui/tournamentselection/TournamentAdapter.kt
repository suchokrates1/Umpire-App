package pl.vestmedia.tennisreferee.ui.tournamentselection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.model.TournamentOption
import pl.vestmedia.tennisreferee.databinding.ItemTournamentBinding

class TournamentAdapter(
    private val tournaments: List<TournamentOption>,
    private val onTournamentClick: (TournamentOption) -> Unit,
) : RecyclerView.Adapter<TournamentAdapter.TournamentViewHolder>() {

    inner class TournamentViewHolder(
        private val binding: ItemTournamentBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tournament: TournamentOption) {
            binding.textTournamentName.text = tournament.name

            val locationText = tournament.location?.takeIf { it.isNotBlank() }
                ?: listOfNotNull(
                    tournament.city?.takeIf { it.isNotBlank() },
                    tournament.country?.takeIf { it.isNotBlank() },
                ).joinToString(", ")
            binding.textTournamentLocation.isVisible = locationText.isNotBlank()
            binding.textTournamentLocation.text = locationText

            val startDate = tournament.startDate.orEmpty()
            val endDate = tournament.endDate.orEmpty()
            val hasDates = startDate.isNotBlank() || endDate.isNotBlank()
            binding.textTournamentDates.isVisible = hasDates
            binding.textTournamentDates.text = binding.root.context.getString(
                R.string.tournament_dates,
                startDate.ifBlank { "?" },
                endDate.ifBlank { "?" },
            )

            binding.root.setOnClickListener { onTournamentClick(tournament) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TournamentViewHolder {
        val binding = ItemTournamentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TournamentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TournamentViewHolder, position: Int) {
        holder.bind(tournaments[position])
    }

    override fun getItemCount(): Int = tournaments.size
}