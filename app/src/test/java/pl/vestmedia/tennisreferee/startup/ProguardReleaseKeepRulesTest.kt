package pl.vestmedia.tennisreferee.startup

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProguardReleaseKeepRulesTest {

    @Test
    fun releaseRulesKeepRetrofitContinuation() {
        val rules = readProguardRules()
        assertTrue(
            "proguard-rules.pro must keep kotlin.coroutines.Continuation for suspend Retrofit",
            rules.contains("kotlin.coroutines.Continuation")
        )
        assertFalseAllowShrinkingOnContinuation(rules)
    }

    @Test
    fun releaseRulesKeepWhatGsonStillReflectsOver() {
        val rules = readProguardRules()
        // The API bodies moved to kotlinx.serialization, but Gson still writes the active
        // match and the stored set history as JSON keyed by these field names.
        assertTrue(
            "proguard-rules.pro must keep domain.match.model for the Gson-stored match state",
            rules.contains("pl.vestmedia.tennisreferee.domain.match.model")
        )
        assertTrue(
            "proguard-rules.pro must keep data.model for the Gson-stored player records",
            rules.contains("pl.vestmedia.tennisreferee.data.model")
        )
    }

    @Test
    fun releaseMappingKeepsTheFieldNamesGsonWritesToStorage() {
        val mapping = findReleaseMapping() ?: return
        val text = mapping.readText()
        for (className in listOf(
            "pl.vestmedia.tennisreferee.domain.match.model.MatchState",
            "pl.vestmedia.tennisreferee.domain.match.model.SetScore",
            "pl.vestmedia.tennisreferee.data.model.Player",
        )) {
            for ((field, replacement) in renamedFields(text, className)) {
                assertTrue(
                    "$className.$field was renamed to \"$replacement\": JSON written by one " +
                        "release would not be readable by the next",
                    field == replacement,
                )
            }
        }
    }

    /** Field mappings R8 recorded for a class; an unchanged name is usually omitted. */
    private fun renamedFields(mapping: String, className: String): List<Pair<String, String>> {
        val fieldLine = Regex("""^ {4}[\w.$\[\]]+ (\w+) -> (\w+)$""")
        return mapping.lineSequence()
            .dropWhile { !it.startsWith("$className ->") }
            .drop(1)
            .takeWhile { it.startsWith("    ") }
            .mapNotNull { line -> fieldLine.find(line.trimEnd())?.let { it.groupValues[1] to it.groupValues[2] } }
            .toList()
    }

    private fun assertFalseAllowShrinkingOnContinuation(rules: String) {
        val continuationLine = rules.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.contains("kotlin.coroutines.Continuation") && !it.startsWith("#") }
            ?: ""
        assertTrue(
            "Continuation keep must not use allowshrinking (R8 dropped it after language selection)",
            continuationLine.startsWith("-keep") && !continuationLine.contains("allowshrinking"),
        )
    }

    private fun findReleaseMapping(): File? {
        return locate("build/outputs/mapping/release/mapping.txt")
            ?: locate("app/build/outputs/mapping/release/mapping.txt")
    }

    private fun readProguardRules(): String {
        val file = locate("proguard-rules.pro")
            ?: throw AssertionError("proguard-rules.pro not found from ${File(".").absolutePath}")
        return file.readText()
    }

    private fun locate(relative: String): File? {
        val cwd = File(".").absoluteFile
        val candidates = listOf(
            File(cwd, relative),
            File(cwd, "app/$relative"),
            File(cwd.parentFile, relative),
            File(cwd.parentFile, "app/$relative")
        )
        return candidates.firstOrNull { it.isFile }
    }
}
