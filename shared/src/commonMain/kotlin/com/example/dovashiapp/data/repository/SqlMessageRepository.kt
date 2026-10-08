package com.example.dovashiapp.data.repository

import com.example.dovashiapp.data.database.DoVashiDatabase
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.repository.MessageRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class SqlMessageRepository(
    private val database: DoVashiDatabase,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher,
) : MessageRepository {

    override suspend fun insertMessage(
        conversationId: Long,
        status: MessageStatus,
        sourceLanguage: String?,
        targetLanguage: String?,
        audioPath: String?,
        transcribedText: String?,
        translatedText: String?,
        reading: String?,
    ): Long = withContext(dispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.messageQueries.transactionWithResult {
            database.messageQueries.insertMessage(
                conversationId, sourceLanguage, targetLanguage, audioPath,
                transcribedText, translatedText, reading, status.name, createdAt = now,
            )
            val id = database.conversationQueries.lastInsertRowId().executeAsOne()
            database.conversationQueries.touchConversation(updatedAt = now, id = conversationId)
            id
        }
    }
}
