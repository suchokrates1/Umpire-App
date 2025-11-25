package pl.vestmedia.tennisreferee.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.SetScore

/**
 * Konwertery typów dla Room Database
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromPlayer(player: Player): String {
        return gson.toJson(player)
    }
    
    @TypeConverter
    fun toPlayer(playerString: String): Player {
        return gson.fromJson(playerString, Player::class.java)
    }
    
    @TypeConverter
    fun fromSetScoreList(setScores: List<SetScore>): String {
        return gson.toJson(setScores)
    }
    
    @TypeConverter
    fun toSetScoreList(setScoresString: String): List<SetScore> {
        val type = object : TypeToken<List<SetScore>>() {}.type
        return gson.fromJson(setScoresString, type)
    }
}
