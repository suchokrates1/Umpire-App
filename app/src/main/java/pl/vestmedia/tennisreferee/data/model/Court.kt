package pl.vestmedia.tennisreferee.data.model

import android.content.Context
import com.google.gson.annotations.SerializedName
import pl.vestmedia.tennisreferee.R

/**
 * Model reprezentujący kort tenisowy
 */
data class Court(
    @SerializedName("kort_id")
    val id: String,
    
    @SerializedName("overlay_id")
    val overlayId: String? = null,
    
    @SerializedName("name")
    val name: String? = null,
    
    @SerializedName("is_available")
    val isAvailable: Boolean = true,
    
    @SerializedName("current_match_id")
    val currentMatchId: Int? = null
) {
    fun getDisplayName(context: Context): String {
        val explicitLabel = normalizeCourtLabel(name, context)
        if (explicitLabel != null) {
            return explicitLabel
        }

        val fallbackLabel = extractCourtOrdinal(id)
        return context.getString(R.string.court_name, fallbackLabel ?: id)
    }

    private fun normalizeCourtLabel(rawValue: String?, context: Context): String? {
        val sanitized = rawValue
            ?.substringAfterLast('•', rawValue)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        extractCourtOrdinal(sanitized)?.let { ordinal ->
            return context.getString(R.string.court_name, ordinal)
        }

        return sanitized
    }

    private fun extractCourtOrdinal(rawValue: String?): String? {
        val value = rawValue?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val plainValue = value
            .replace(Regex("^(?i)(court|kort)\\s+"), "")
            .trim()

        if (plainValue.matches(Regex("\\d+"))) {
            return plainValue
        }

        return Regex("^t\\d+-(\\d+)$", RegexOption.IGNORE_CASE)
            .matchEntire(plainValue)
            ?.groupValues
            ?.getOrNull(1)
    }
}
