package pl.vestmedia.tennisreferee.ui.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AvailableLanguagesTest {
    @Test
    fun includesLithuanian() {
        val lithuanian = AvailableLanguages.all.single { it.code == "lt" }
        assertEquals("Lietuvių", lithuanian.name)
    }

    @Test
    fun everyLocaleHasTheSameTranslatableStrings() {
        val defaultNames = translatableNames("values")
        val locales = AvailableLanguages.all.map { "values-${it.code}" }
        val problems = locales.mapNotNull { folder ->
            val missing = defaultNames - translatableNames(folder)
            if (missing.isEmpty()) null else "$folder missing $missing"
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun translatableNames(folder: String): Set<String> {
        val candidates = listOf(
            File("src/main/res/$folder/strings.xml"),
            File("app/src/main/res/$folder/strings.xml"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("strings.xml not found for $folder in ${File(".").absolutePath}")
        return Regex("""<string\s+name="([^"]+)"([^>]*)>""")
            .findAll(file.readText())
            .filter { !it.groupValues[2].contains("""translatable="false"""") }
            .map { it.groupValues[1] }
            .toSet()
    }
}
