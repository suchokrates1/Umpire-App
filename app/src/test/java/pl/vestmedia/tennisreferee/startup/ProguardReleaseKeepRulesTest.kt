package pl.vestmedia.tennisreferee.startup

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProguardReleaseKeepRulesTest {

    @Test
    fun releaseRulesKeepEncryptedSharedPreferencesAndTink() {
        val rules = readProguardRules()
        assertTrue(
            "proguard-rules.pro must keep androidx.security.crypto for EncryptedSharedPreferences",
            rules.contains("androidx.security.crypto")
        )
        assertTrue(
            "proguard-rules.pro must keep com.google.crypto.tink used by MasterKey",
            rules.contains("com.google.crypto.tink")
        )
    }

    @Test
    fun releaseMappingKeepsSecurityCryptoWhenMinifyHasRun() {
        val mapping = findReleaseMapping() ?: return
        val text = mapping.readText()
        assertTrue(
            "R8 mapping.txt is missing EncryptedSharedPreferences after minifyRelease",
            text.contains("androidx.security.crypto.EncryptedSharedPreferences")
        )
        assertTrue(
            "R8 mapping.txt is missing MasterKey after minifyRelease",
            text.contains("androidx.security.crypto.MasterKey")
        )
        assertTrue(
            "R8 mapping.txt is missing com.google.crypto.tink after minifyRelease",
            text.contains("com.google.crypto.tink")
        )
    }

    private fun readProguardRules(): String {
        val file = locate("proguard-rules.pro")
            ?: throw AssertionError("proguard-rules.pro not found from ${File(".").absolutePath}")
        return file.readText()
    }

    private fun findReleaseMapping(): File? {
        return locate("build/outputs/mapping/release/mapping.txt")
            ?: locate("app/build/outputs/mapping/release/mapping.txt")
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
