package pl.vestmedia.tennisreferee.ui.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AvailableLanguagesTest {
    @Test
    fun includesLithuanian() {
        val lithuanian = AvailableLanguages.byCode("lt")
        assertEquals("lt", lithuanian.code)
        assertEquals("Lietuvių", lithuanian.name)
        assertTrue(AvailableLanguages.all.any { it.code == "lt" })
    }

    @Test
    fun lithuanianStringsCoverPolishKeys() {
        val polish = stringNames("values-pl")
        val lithuanian = stringNames("values-lt")
        val missing = polish - lithuanian
        assertTrue("Missing LT keys: $missing", missing.isEmpty())
    }

    private fun stringNames(folder: String): Set<String> {
        val candidates = listOf(
            File("src/main/res/$folder/strings.xml"),
            File("app/src/main/res/$folder/strings.xml"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("strings.xml not found for $folder in ${File(".").absolutePath}")
        return Regex("""<string\s+name="([^"]+)"""").findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()
    }
}
