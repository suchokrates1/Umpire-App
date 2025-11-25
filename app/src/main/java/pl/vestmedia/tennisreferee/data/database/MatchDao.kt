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
    
    @Query("SELECT * FROM matches WHERE isCompleted = 1 ORDER BY matchStartTime DESC LIMIT :limit")
    fun getRecentMatches(limit: Int = 20): Flow<List<MatchEntity>>
    
    @Query("SELECT * FROM matches WHERE courtId = :courtId ORDER BY matchStartTime DESC")
    fun getMatchesByCourt(courtId: Int): Flow<List<MatchEntity>>
    
    @Query("SELECT * FROM matches WHERE player1.id = :playerId OR player2.id = :playerId ORDER BY matchStartTime DESC")
    fun getMatchesByPlayer(playerId: Int): Flow<List<MatchEntity>>
    
    @Query("SELECT COUNT(*) FROM matches")
    suspend fun getMatchCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long
    
    @Update
    suspend fun updateMatch(match: MatchEntity)
    
    @Delete
    suspend fun deleteMatch(match: MatchEntity)
    
    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: Long)
    
    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()
    
    @Query("SELECT * FROM matches WHERE matchStartTime BETWEEN :startTime AND :endTime ORDER BY matchStartTime DESC")
    fun getMatchesByDateRange(startTime: Long, endTime: Long): Flow<List<MatchEntity>>
}
