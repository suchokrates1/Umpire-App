package pl.vestmedia.tennisreferee.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Architectural guardrails for review items N5 and N6:
 * - N6: `domain` sources stay free of Android framework / Parcelable imports and UI emojis.
 * - N5: Tutorial state singletons (`TutorialSession`, `TutorialCatalog`,
 *   `TutorialOverlayController`, `TutorialSnapshots`) are encapsulated inside `ui/tutorial/`.
 */
class DomainAndTutorialArchitectureTest {

    private fun mainJavaRoot(): File {
        val candidates = listOf(
            File("src/main/java/pl/vestmedia/tennisreferee"),
            File("app/src/main/java/pl/vestmedia/tennisreferee"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/java/pl/vestmedia/tennisreferee from ${File(".").absolutePath}")
    }

    @Test
    fun `domain package has no Android framework Parcelable or emoji dependencies`() {
        val domainDir = File(mainJavaRoot(), "domain")
        val kotlinFiles = domainDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("Expected Kotlin files in domain/", kotlinFiles.isNotEmpty())

        val violations = mutableListOf<String>()
        val forbiddenTokens = listOf(
            "import android.",
            "import androidx.",
            "kotlinx.parcelize",
            "Parcelable",
            "@Parcelize",
            "ui.tutorial",
            "🎾",
        )
        for (file in kotlinFiles) {
            val text = file.readText()
            for (token in forbiddenTokens) {
                if (text.contains(token)) {
                    violations += "${file.relativeTo(domainDir).path}: contains `$token`"
                }
            }
        }
        assertTrue("Domain purity violations:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    @Test
    fun `tutorial state and overlay classes stay encapsulated inside ui tutorial package`() {
        val root = mainJavaRoot()
        val tutorialDir = File(root, "ui/tutorial").canonicalFile
        val nonTutorialFiles = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.canonicalFile.path.startsWith(tutorialDir.path) }
            .toList()
        assertTrue("Expected production Kotlin files outside ui/tutorial", nonTutorialFiles.isNotEmpty())

        val violations = mutableListOf<String>()
        val internalTutorialTypes = listOf(
            "TutorialSession",
            "TutorialCatalog",
            "TutorialOverlayController",
            "TutorialSnapshots",
        )
        for (file in nonTutorialFiles) {
            val text = file.readText()
            for (symbol in internalTutorialTypes) {
                if (Regex("\\b$symbol\\b").containsMatchIn(text)) {
                    violations += "${file.relativeTo(root).path}: references `$symbol` directly"
                }
            }
        }
        assertTrue(
            "Production files outside ui/tutorial must go through TutorialScreenHost / TutorialNavigator:\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }
}
