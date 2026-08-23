package pl.vestmedia.tennisreferee.startup

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * Guards the cold-start path: EncryptedSharedPreferences / Tink / Keystore
 * must not be referenced from production startup code. A broken Keystore
 * native-crashes the process on the launcher logo.
 */
class ProguardReleaseKeepRulesTest {

    @Test
    fun startupSourcesDoNotTouchEncryptedSharedPreferencesOrTink() {
        val sources = listOf(
            "CourtSessionStore.kt",
            "TennisRefereeApp.kt"
        ).map { locateSource(it) }

        sources.forEach { file ->
            val text = stripComments(file.readText())
            assertFalse(
                "${file.name} must not reference EncryptedSharedPreferences",
                text.contains("EncryptedSharedPreferences")
            )
            assertFalse(
                "${file.name} must not reference MasterKey",
                text.contains("MasterKey")
            )
            assertFalse(
                "${file.name} must not reference androidx.security.crypto",
                text.contains("androidx.security.crypto")
            )
            assertFalse(
                "${file.name} must not reference com.google.crypto.tink",
                text.contains("com.google.crypto.tink")
            )
        }
    }

    @Test
    fun gradleDoesNotDependOnSecurityCrypto() {
        val gradle = locate("app/build.gradle")
            ?: locate("build.gradle")
            ?: throw AssertionError("app/build.gradle not found from ${File(".").absolutePath}")
        val text = gradle.readText()
        assertFalse(
            "app/build.gradle must not depend on androidx.security:security-crypto",
            text.contains("security-crypto")
        )
    }

    private fun locateSource(fileName: String): File {
        return locate("app/src/main/java/pl/vestmedia/tennisreferee/data/auth/$fileName")
            ?: locate("app/src/main/java/pl/vestmedia/tennisreferee/$fileName")
            ?: locate("src/main/java/pl/vestmedia/tennisreferee/data/auth/$fileName")
            ?: locate("src/main/java/pl/vestmedia/tennisreferee/$fileName")
            ?: throw AssertionError("$fileName not found from ${File(".").absolutePath}")
    }

    private fun stripComments(text: String): String {
        return text
            .replace(Regex("/\\*.*?\\*/", setOf(RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("//.*"), "")
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
