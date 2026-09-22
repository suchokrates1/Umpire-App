package pl.vestmedia.tennisreferee.startup

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProguardReleaseKeepRulesTest {

    @Test
    fun releaseRulesKeepGsonDtosAndRetrofitContinuation() {
        val rules = readProguardRules()
        assertTrue(
            "proguard-rules.pro must keep data.api.dto after the model/DTO split",
            rules.contains("pl.vestmedia.tennisreferee.data.api.dto")
        )
        assertTrue(
            "proguard-rules.pro must keep kotlin.coroutines.Continuation for suspend Retrofit",
            rules.contains("kotlin.coroutines.Continuation")
        )
        assertFalseAllowShrinkingOnContinuation(rules)
    }

    private fun assertFalseAllowShrinkingOnContinuation(rules: String) {
        val continuationLine = rules.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.contains("kotlin.coroutines.Continuation") && !it.startsWith("#") }
            ?: ""
        assertTrue(
            "Continuation keep must not use allowshrinking (R8 dropped it after language selection)",
            continuationLine.startsWith("-keep") && !continuationLine.contains("allowshrinking")
        )
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
