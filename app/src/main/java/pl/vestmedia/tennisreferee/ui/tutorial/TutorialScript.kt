package pl.vestmedia.tennisreferee.ui.tutorial

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class TutorialStep(
    val id: String,
    val scene: String,
    val target: String,
    val titleKey: String,
    val bodyKey: String,
    val requireAction: String? = null,
    val snapshot: String? = null,
)

data class TutorialScriptFile(
    val version: Int = 1,
    val pin: String = TutorialCatalog.PIN,
    val steps: List<TutorialStep> = emptyList(),
)

object TutorialScript {
    private val gson = Gson()
    private var cached: TutorialScriptFile? = null

    fun load(context: Context): TutorialScriptFile {
        cached?.let { return it }
        val parsed = context.assets.open("tutorial/script.json").bufferedReader().use { reader ->
            gson.fromJson(reader, TutorialScriptFile::class.java)
        }
        cached = parsed
        return parsed
    }

    fun titleRes(key: String): String = camelToSnake(key)

    private fun camelToSnake(key: String): String =
        key.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}
