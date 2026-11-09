package com.padelaragon.shared.data.chat

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.padelaragon.shared.data.remote.dto.BundleJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer

@Entity(tableName = "chat_message")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val citationsJson: String,
    val createdAt: Long
)

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_message ORDER BY id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insert(entity: ChatMessageEntity): Long

    @Query("DELETE FROM chat_message WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM chat_message")
    suspend fun deleteAll()

    @Query("DELETE FROM chat_message WHERE id NOT IN (SELECT id FROM chat_message ORDER BY id DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int)
}

/** Persistence of the single ongoing conversation. */
interface ChatStore {
    val messages: Flow<List<ChatMessage>>
    suspend fun add(message: ChatMessage): Long
    suspend fun remove(id: Long)
    suspend fun clear()
}

class RoomChatStore(private val dao: ChatMessageDao, private val keep: Int = KEEP_MESSAGES) : ChatStore {
    override val messages: Flow<List<ChatMessage>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    override suspend fun add(message: ChatMessage): Long {
        val id = dao.insert(
            ChatMessageEntity(
                role = message.role.wire,
                content = message.content,
                citationsJson = BundleJson.encodeToString(ListSerializer(Citation.serializer()), message.citations),
                createdAt = message.createdAt
            )
        )
        dao.trimTo(keep)
        return id
    }

    override suspend fun remove(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.deleteAll()

    private fun ChatMessageEntity.toModel() = ChatMessage(
        id = id,
        role = ChatRole.entries.firstOrNull { it.wire == role } ?: ChatRole.ASSISTANT,
        content = content,
        citations = runCatching {
            BundleJson.decodeFromString(ListSerializer(Citation.serializer()), citationsJson)
        }.getOrDefault(emptyList()),
        createdAt = createdAt
    )

    companion object {
        const val KEEP_MESSAGES = 50
    }
}
