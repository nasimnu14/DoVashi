package com.example.dovashiapp.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.dovashiapp.data.database.DoVashiDatabase
import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.ConversationSummary
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.repository.ConversationRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SqlConversationRepository(
    private val database: DoVashiDatabase,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher,
) : ConversationRepository {

    private val queries get() = database.conversationQueries

    override fun observeSummaries(): Flow<List<ConversationSummary>> =
        queries.selectSummaries { id, title, language1Code, language2Code, createdAt, updatedAt,
                                  lastMessageText, lastMessageStatus, messageCount ->
            ConversationSummary(
                conversation = Conversation(id, title, language1Code, language2Code, createdAt, updatedAt),
                lastMessageText = lastMessageText,
                lastMessageStatus = lastMessageStatus?.let(::parseStatus),
                messageCount = messageCount,
            )
        }.asFlow().mapToList(dispatcher)

    override suspend fun createConversation(
        title: String,
        language1Code: String,
        language2Code: String,
    ): Long = withContext(dispatcher) {
        val now = clock.now().toEpochMilliseconds()
        queries.transactionWithResult {
            queries.insertConversation(title, language1Code, language2Code, createdAt = now, updatedAt = now)
            queries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun hasConversations(): Boolean = withContext(dispatcher) {
        queries.countConversations().executeAsOne() > 0
    }
}

// Persisted text may predate or postdate this build's enum; one unknown value must not crash the whole list.
private fun parseStatus(value: String): MessageStatus =
    MessageStatus.entries.firstOrNull { it.name == value } ?: MessageStatus.FAILED
