package com.personalaiagent.data.repository

import com.personalaiagent.core.IdGenerator
import com.personalaiagent.core.TimeProvider
import com.personalaiagent.data.local.ConversationDao
import com.personalaiagent.data.local.ConversationEntity
import com.personalaiagent.data.local.MessageDao
import com.personalaiagent.data.local.toDomain
import com.personalaiagent.data.local.toEntity
import com.personalaiagent.domain.model.ChatMessage
import com.personalaiagent.domain.model.Conversation
import com.personalaiagent.domain.model.ConversationSummary
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.MessageStatus
import com.personalaiagent.domain.repository.ChatRepository
import com.personalaiagent.domain.usecase.ConversationTitles
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : ChatRepository {

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.observeByConversation(conversationId)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeLatestConversation(): Flow<Conversation?> =
        conversationDao.observeLatest().map { it?.toDomain() }

    override suspend fun getConversation(id: String): Conversation? =
        conversationDao.getById(id)?.toDomain()

    override suspend fun getMessages(conversationId: String): List<ChatMessage> =
        messageDao.getByConversation(conversationId).map { it.toDomain() }

    override suspend fun recentConversations(
        excludeConversationId: String?,
        limit: Int,
    ): List<Conversation> =
        conversationDao.getRecent(excludeConversationId ?: "", limit.coerceIn(1, 10))
            .map { it.toDomain() }

    override fun observeConversationSummaries(query: String): Flow<List<ConversationSummary>> {
        val trimmed = query.trim()
        val rows = if (trimmed.isEmpty()) {
            conversationDao.observeSummaries()
        } else {
            conversationDao.searchSummaries(trimmed)
        }
        return rows.map { list ->
            list.map { row ->
                ConversationSummary(
                    id = row.id,
                    title = row.title,
                    preview = row.preview?.replace(Regex("\\s+"), " ")?.trim().orEmpty(),
                    updatedAt = row.updatedAt,
                    messageCount = row.messageCount,
                )
            }
        }
    }

    override suspend fun renameConversation(id: String, title: String) {
        val clean = title.trim().take(MAX_TITLE_CHARS)
        if (clean.isEmpty()) return
        conversationDao.rename(id, clean)
    }

    override suspend fun createConversation(
        title: String,
        providerId: String?,
        modelId: String?,
    ): String {
        val now = time.now()
        val id = ids.newId()
        conversationDao.insert(
            ConversationEntity(
                id = id,
                title = title.ifBlank { "New conversation" },
                createdAt = now,
                updatedAt = now,
                providerId = providerId,
                modelId = modelId,
            ),
        )
        return id
    }

    override suspend fun updateConversationModel(id: String, providerId: String?, modelId: String?) {
        conversationDao.updateModel(id, providerId, modelId, time.now())
    }

    override suspend fun appendMessage(message: ChatMessage) {
        messageDao.insert(message.toEntity())
        conversationDao.touch(message.conversationId, message.createdAt)
        // A chat created before this feature (or from the model picker) still has
        // the placeholder title; the first real message names it.
        if (message.role == MessageRole.USER) {
            val conversation = conversationDao.getById(message.conversationId)
            if (conversation != null && isPlaceholderTitle(conversation.title)) {
                val title = ConversationTitles.from(message.content)
                if (title.isNotEmpty()) conversationDao.rename(conversation.id, title)
            }
        }
    }

    private fun isPlaceholderTitle(title: String): Boolean {
        val clean = title.trim()
        return clean.isEmpty() ||
            clean.equals("New conversation", ignoreCase = true) ||
            clean == "محادثة جديدة"
    }

    override suspend fun updateAssistantMessage(
        id: String,
        content: String,
        status: MessageStatus,
        error: String?,
        tokenIn: Int?,
        tokenOut: Int?,
    ) {
        messageDao.updateAssistant(
            id = id,
            content = content,
            status = status.name,
            error = error,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
        )
    }

    override suspend fun deleteConversation(id: String) {
        conversationDao.delete(id)
    }

    override suspend fun clearAll() {
        messageDao.clear()
        conversationDao.clear()
    }

    private companion object {
        /** Long enough for a real name, short enough for one line in the list. */
        const val MAX_TITLE_CHARS = 80
    }
}
