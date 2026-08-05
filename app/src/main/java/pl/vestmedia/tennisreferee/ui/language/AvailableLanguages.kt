package pl.vestmedia.tennisreferee.ui.language

import pl.vestmedia.tennisreferee.data.model.Language

object AvailableLanguages {
    val all: List<Language> = listOf(
        Language("de", "Deutsch", "🇩🇪"),
        Language("en", "English", "🇬🇧"),
        Language("es", "Español", "🇪🇸"),
        Language("fr", "Français", "🇫🇷"),
        Language("it", "Italiano", "🇮🇹"),
        Language("pl", "Polski", "🇵🇱"),
    )

    fun byCode(code: String): Language {
        return all.firstOrNull { it.code == code } ?: all.first { it.code == "en" }
    }
}
