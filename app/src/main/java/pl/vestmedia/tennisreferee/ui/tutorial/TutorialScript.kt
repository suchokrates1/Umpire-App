package pl.vestmedia.tennisreferee.ui.tutorial

import android.content.Context
import kotlinx.serialization.Serializable
import pl.vestmedia.tennisreferee.data.localJson

@Serializable
data class TutorialStep(
    val id: String,
    val scene: String,
    val target: String,
    val titleKey: String,
    val bodyKey: String,
    val requireAction: String? = null,
    val snapshot: String? = null,
)

@Serializable
data class TutorialScriptFile(
    val version: Int = 1,
    val pin: String = TutorialCatalog.PIN,
    val steps: List<TutorialStep> = emptyList(),
)

object TutorialScript {
    private var cached: TutorialScriptFile? = null

    fun load(context: Context): TutorialScriptFile {
        cached?.let { return it }
        val parsed = context.assets.open("tutorial/script.json").bufferedReader().use { reader ->
            parse(reader.readText())
        }
        cached = parsed
        return parsed
    }

    fun parse(raw: String): TutorialScriptFile = localJson.decodeFromString(raw)

    fun titleRes(key: String): String = camelToSnake(key)

    private fun camelToSnake(key: String): String =
        key.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}
