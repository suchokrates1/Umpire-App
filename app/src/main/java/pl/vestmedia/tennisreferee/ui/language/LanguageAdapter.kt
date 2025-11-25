package pl.vestmedia.tennisreferee.ui.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pl.vestmedia.tennisreferee.databinding.ItemLanguageBinding
import pl.vestmedia.tennisreferee.data.model.Language

/**
 * Adapter dla listy języków
 */
class LanguageAdapter(
    private val languages: List<Language>,
    private val onLanguageClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {
    
    inner class LanguageViewHolder(
        private val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(language: Language) {
            binding.textFlag.text = language.flagEmoji
            binding.textLanguageName.text = language.name
            
            binding.root.setOnClickListener {
                onLanguageClick(language)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }
    
    override fun getItemCount() = languages.size
}
