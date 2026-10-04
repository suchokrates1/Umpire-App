package pl.vestmedia.tennisreferee.ui.tutorial

import java.io.File
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.vestmedia.tennisreferee.data.localJson
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

/** The tutorial ships as JSON in the assets; every file has to read with kotlinx. */
class TutorialAssetsFormatTest {

    private val assets = listOf("src/main/assets/tutorial", "app/src/main/assets/tutorial")
        .map(::File).first { it.isDirectory }

    @Test
    fun theScriptReadsAndEverySnapshotItNamesExists() {
        val script = TutorialScript.parse(File(assets, "script.json").readText())
        assertTrue(script.steps.isNotEmpty())
        script.steps.mapNotNull { it.snapshot }.forEach { id ->
            assertTrue("snapshot $id", File(assets, "snapshots/$id.json").isFile)
        }
    }

    @Test
    fun everyTextTheScriptNamesExistsAndSurvivesTheReleaseShrinker() {
        val res = assets.parentFile!!.parentFile!!.resolve("res")
        val strings = res.resolve("values/strings.xml").readText()
        val keepRule = res.resolve("raw/keep.xml").readText()
        val script = TutorialScript.parse(File(assets, "script.json").readText())
        script.steps.flatMap { listOf(it.titleKey, it.bodyKey) }.map(TutorialScript::titleRes).forEach { name ->
            assertTrue("strings.xml has $name", "name=\"$name\"" in strings)
            assertTrue("$name is looked up by name, so keep.xml must keep it", name.startsWith("tutorial_"))
        }
        assertTrue("keep.xml keeps the tutorial texts", "@string/tutorial_*" in keepRule)
    }

    @Test
    fun everySnapshotHoldsAReadableMatch() {
        val files = File(assets, "snapshots").listFiles { file -> file.extension == "json" }.orEmpty()
        assertTrue(files.isNotEmpty())
        files.forEach { file ->
            val root = localJson.parseToJsonElement(file.readText()).jsonObject
            val state = localJson.decodeFromJsonElement(MatchState.serializer(), root.getValue("state"))
            assertTrue("${file.name} has players", state.player1.name.isNotBlank() || state.player1.firstName.isNotBlank())
        }
    }
}
