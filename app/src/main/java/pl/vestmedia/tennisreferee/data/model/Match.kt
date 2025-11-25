package pl.vestmedia.tennisreferee.data.model

import com.google.gson.annotations.SerializedName

/**
 * Model reprezentujący mecz tenisowy
 */
data class Match(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("court_id")
    val courtId: String,
    
    @SerializedName("player1_name")
    val player1Name: String,
    
    @SerializedName("player2_name")
    val player2Name: String,
    
    @SerializedName("score")
    val score: Score,
    
    @SerializedName("status")
    val status: MatchStatus,
    
    @SerializedName("created_at")
    val createdAt: String?,
    
    @SerializedName("updated_at")
    val updatedAt: String?
)

enum class MatchStatus {
    @SerializedName("not_started")
    NOT_STARTED,
    
    @SerializedName("in_progress")
    IN_PROGRESS,
    
    @SerializedName("finished")
    FINISHED
}
