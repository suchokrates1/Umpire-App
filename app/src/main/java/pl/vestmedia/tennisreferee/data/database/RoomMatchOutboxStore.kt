package pl.vestmedia.tennisreferee.data.database

import pl.vestmedia.tennisreferee.ui.match.MatchOutboxStore

class RoomMatchOutboxStore(private val dao: OutboxMutationDao) : MatchOutboxStore {

    override suspend fun enqueue(mutation: OutboxMutationEntity): Long {
        val id = dao.insert(mutation)
        if (mutation.type == "UPDATE") {
            dao.deleteOlderPendingUpdates(mutation.clientMatchUuid, id)
        }
        return id
    }

    override suspend fun getPending(): List<OutboxMutationEntity> = dao.getPending()

    override suspend fun update(mutation: OutboxMutationEntity) = dao.update(mutation)

    override suspend fun propagateServerMatchId(clientMatchUuid: String, serverMatchId: Int) =
        dao.propagateServerMatchId(clientMatchUuid, serverMatchId)

    override suspend fun deleteDone() = dao.deleteDone()

    override suspend fun hasPending(): Boolean = dao.countPendingOrInFlight() > 0
}
