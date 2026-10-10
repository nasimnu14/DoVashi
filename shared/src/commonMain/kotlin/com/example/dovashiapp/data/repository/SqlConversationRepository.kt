package com.example.dovashiapp.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.example.dovashiapp.data.database.DoVashiDatabase
import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.ConversationSummary
import com.example.dovashiapp.domain.repository.ConversationRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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
            // Refused Message steps still notify the table; don't re-emit an identical list.
            .distinctUntilChanged()

    override fun observeConversation(id: Long): Flow<Conversation?> =
        queries.selectById(id) { rowId, title, language1Code, language2Code, createdAt, updatedAt ->
            Conversation(rowId, title, language1Code, language2Code, createdAt, updatedAt)
        }.asFlow().mapToOneOrNull(dispatcher)

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
