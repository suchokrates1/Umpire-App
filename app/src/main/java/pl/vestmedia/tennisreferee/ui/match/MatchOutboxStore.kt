package pl.vestmedia.tennisreferee.ui.match

import pl.vestmedia.tennisreferee.data.database.OutboxMutationEntity

interface MatchOutboxStore {
    suspend fun enqueue(mutation: OutboxMutationEntity): Long
    suspend fun getPending(): List<OutboxMutationEntity>
    suspend fun update(mutation: OutboxMutationEntity)
    suspend fun propagateServerMatchId(clientMatchUuid: String, serverMatchId: Int)
    suspend fun deleteDone()
    suspend fun hasPending(): Boolean
    suspend fun dropPendingUpdates(clientMatchUuid: String)
}
