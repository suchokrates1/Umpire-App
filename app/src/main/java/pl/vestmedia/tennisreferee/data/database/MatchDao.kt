package pl.vestmedia.tennisreferee.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object dla meczów
 */
@Dao
interface MatchDao {
    
    @Query("SELECT * FROM matches ORDER BY matchStartTime DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>
    
    @Query("SELECT * FROM matches WHERE id = :matchId")
    suspend fun getMatchById(matchId: Long): MatchEntity?
    
    @Query("SELECT COUNT(*) FROM matches")
    suspend fun getMatchCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long
    
    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()
}
