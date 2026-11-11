package pl.vestmedia.tennisreferee.data.database

import androidx.room.*

@Dao
interface OutboxMutationDao {

    /**
     * Everything still owed to the server. IN_FLIGHT is a request the app died in the
     * middle of; FAILED_AUTH waits for a new court session. Reading only PENDING left
     * both in the table for good.
     */
    @Query(
        "SELECT * FROM outbox_mutations " +
        "WHERE status IN ('PENDING', 'IN_FLIGHT', 'FAILED_AUTH') ORDER BY createdAt ASC"
    )
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
        "WHERE clientMatchUuid = :uuid AND status IN ('PENDING', 'IN_FLIGHT', 'FAILED_AUTH') AND serverMatchId IS NULL"
    )
    suspend fun propagateServerMatchId(uuid: String, serverMatchId: Int)

    @Query("SELECT COUNT(*) FROM outbox_mutations WHERE status IN ('PENDING', 'IN_FLIGHT', 'FAILED_AUTH')")
    suspend fun countPendingOrInFlight(): Int

    @Query(
        "DELETE FROM outbox_mutations " +
        "WHERE clientMatchUuid = :uuid AND type IN ('UPDATE', 'EVENT') AND status IN ('PENDING', 'IN_FLIGHT', 'FAILED_AUTH')"
    )
    suspend fun dropPendingUpdates(uuid: String)
}
