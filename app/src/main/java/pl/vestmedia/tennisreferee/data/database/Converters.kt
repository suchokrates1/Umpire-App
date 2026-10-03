package pl.vestmedia.tennisreferee.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.vestmedia.tennisreferee.data.localJson
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

/**
 * Konwertery typów dla Room Database.
 *
 * Zapis idzie przez kotlinx; odczyt próbuje najpierw kotlinx, a potem Gsona, bo wiersze
 * zapisane starszym wydaniem leżą już na tablecie sędziego.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromPlayer(player: Player): String = localJson.encodeToString(player)

    @TypeConverter
    fun toPlayer(playerString: String): Player =
        decodePlayer(playerString) ?: gson.fromJson(playerString, Player::class.java)

    @TypeConverter
    fun fromNullablePlayer(player: Player?): String? = player?.let { localJson.encodeToString(it) }

    @TypeConverter
    fun toNullablePlayer(playerString: String?): Player? = playerString?.let { toPlayer(it) }

    @TypeConverter
    fun fromSetScoreList(setScores: List<SetScore>): String = localJson.encodeToString(setScores)

    @TypeConverter
    fun toSetScoreList(setScoresString: String): List<SetScore> {
        return try {
            localJson.decodeFromString<List<SetScore>>(setScoresString)
        } catch (_: Exception) {
            val type = object : TypeToken<List<SetScore>>() {}.type
            gson.fromJson(setScoresString, type)
        }
    }

    private fun decodePlayer(payload: String): Player? = try {
        localJson.decodeFromString<Player>(payload)
    } catch (_: Exception) {
        null
    }
}
