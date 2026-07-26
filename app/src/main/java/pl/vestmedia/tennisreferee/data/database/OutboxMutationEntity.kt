package pl.vestmedia.tennisreferee.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outbox_mutations")
data class OutboxMutationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientMatchUuid: String,
    val type: String,
    val payloadJson: String,
    val serverMatchId: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null,
    val status: String = "PENDING"
)
