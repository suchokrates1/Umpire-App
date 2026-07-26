package pl.vestmedia.tennisreferee.data.database

import androidx.room.*

@Dao
interface OutboxMutationDao {

    @Query("SELECT * FROM outbox_mutations WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<OutboxMutationEntity>

    @Insert
    suspend fun insert(mutation: OutboxMutationEntity): Long

    @Update
    suspend fun update(mutation: OutboxMutationEntity)

    @Query("DELETE FROM outbox_mutations WHERE status = 'DONE'")
    suspend fun deleteDone()

    @Query(
        "DELETE FROM outbox_mutations " +
        "WHERE clientMatchUuid = :uuid AND type = 'UPDATE' AND status = 'PENDING' AND id < :newestId"
    )
    suspend fun deleteOlderPendingUpdates(uuid: String, newestId: Long)

    @Query(
        "UPDATE outbox_mutations SET serverMatchId = :serverMatchId " +
        "WHERE clientMatchUuid = :uuid AND status IN ('PENDING', 'IN_FLIGHT') AND serverMatchId IS NULL"
    )
    suspend fun propagateServerMatchId(uuid: String, serverMatchId: Int)

    @Query("SELECT COUNT(*) FROM outbox_mutations WHERE status IN ('PENDING', 'IN_FLIGHT')")
    suspend fun countPendingOrInFlight(): Int
}
