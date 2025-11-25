package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

/**
 * Model reprezentujący wynik meczu
 */
data class Score(
    @SerializedName("player1_sets")
    val player1Sets: Int = 0,
    
    @SerializedName("player2_sets")
    val player2Sets: Int = 0,
    
    @SerializedName("player1_games")
    val player1Games: Int = 0,
    
    @SerializedName("player2_games")
    val player2Games: Int = 0,
    
    @SerializedName("player1_points")
    val player1Points: Int = 0,
    
    @SerializedName("player2_points")
    val player2Points: Int = 0,
    
    @SerializedName("sets_history")
    val setsHistory: List<SetScore> = emptyList()
)

data class SetScore(
    @SerializedName("set_number")
    val setNumber: Int,
    
    @SerializedName("player1_games")
    val player1Games: Int,
    
    @SerializedName("player2_games")
    val player2Games: Int
)
