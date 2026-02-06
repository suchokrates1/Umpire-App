package pl.vestmedia.tennisreferee.data.repository

import kotlinx.coroutines.flow.Flow
import pl.vestmedia.tennisreferee.data.database.MatchDao
import pl.vestmedia.tennisreferee.data.database.MatchEntity
import pl.vestmedia.tennisreferee.data.model.MatchState

/**
 * Repository do zarządzania zapisanymi meczami
 */
class MatchHistoryRepository(private val matchDao: MatchDao) {
    
    val allMatches: Flow<List<MatchEntity>> = matchDao.getAllMatches()
    
    fun getRecentMatches(limit: Int = 20): Flow<List<MatchEntity>> {
        return matchDao.getRecentMatches(limit)
    }
    
    fun getMatchesByCourt(courtId: Int): Flow<List<MatchEntity>> {
        return matchDao.getMatchesByCourt(courtId)
    }
    
    /**
     * Pobiera mecze gracza - filtrowanie po stronie aplikacji
     * ponieważ player1 i player2 są przechowywane jako JSON
     */
    fun getMatchesByPlayer(playerId: Int): Flow<List<MatchEntity>> {
        return matchDao.getAllMatchesForPlayerFilter()
    }
    
    suspend fun getMatchById(matchId: Long): MatchEntity? {
        return matchDao.getMatchById(matchId)
    }
    
    suspend fun getMatchCount(): Int {
        return matchDao.getMatchCount()
    }
    
    suspend fun saveMatch(matchState: MatchState): Long {
        val matchEntity = matchState.toEntity()
        return matchDao.insertMatch(matchEntity)
    }
    
    suspend fun updateMatch(match: MatchEntity) {
        matchDao.updateMatch(match)
    }
    
    suspend fun deleteMatch(match: MatchEntity) {
        matchDao.deleteMatch(match)
    }
    
    suspend fun deleteMatchById(matchId: Long) {
        matchDao.deleteMatchById(matchId)
    }
    
    suspend fun deleteAllMatches() {
        matchDao.deleteAllMatches()
    }
    
    fun getMatchesByDateRange(startTime: Long, endTime: Long): Flow<List<MatchEntity>> {
        return matchDao.getMatchesByDateRange(startTime, endTime)
    }
}

/**
 * Konwersja MatchState do MatchEntity
 */
private fun MatchState.toEntity(): MatchEntity {
    return MatchEntity(
        courtId = courtId.toIntOrNull() ?: 0,
        courtName = courtName,
        matchStartTime = matchStartTime,
        matchEndTime = System.currentTimeMillis(),
        matchDuration = matchDuration,
        player1 = player1,
        player2 = player2,
        player1Sets = player1Sets,
        player2Sets = player2Sets,
        setsHistory = setsHistory,
        player1Aces = player1Stats.aces,
        player1DoubleFaults = player1Stats.doubleFaults,
        player1Winners = player1Stats.winners,
        player1ForcedErrors = player1Stats.forcedErrors,
        player1UnforcedErrors = player1Stats.unforcedErrors,
        player1FirstServesIn = player1Stats.firstServesIn,
        player1FirstServesTotal = player1Stats.firstServesTotal,
        player1SecondServesIn = player1Stats.secondServesIn,
        player1SecondServesTotal = player1Stats.secondServesTotal,
        player2Aces = player2Stats.aces,
        player2DoubleFaults = player2Stats.doubleFaults,
        player2Winners = player2Stats.winners,
        player2ForcedErrors = player2Stats.forcedErrors,
        player2UnforcedErrors = player2Stats.unforcedErrors,
        player2FirstServesIn = player2Stats.firstServesIn,
        player2FirstServesTotal = player2Stats.firstServesTotal,
        player2SecondServesIn = player2Stats.secondServesIn,
        player2SecondServesTotal = player2Stats.secondServesTotal,
        isCompleted = true,
        winnerId = if (player1Sets > player2Sets) player1.id else if (player2Sets > player1Sets) player2.id else null
    )
}
