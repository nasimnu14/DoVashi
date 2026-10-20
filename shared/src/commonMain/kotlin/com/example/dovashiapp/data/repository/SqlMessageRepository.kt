package com.example.dovashiapp.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.QueryResult
import com.example.dovashiapp.data.database.DoVashiDatabase
import com.example.dovashiapp.domain.model.Message
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.model.MessageStep
import com.example.dovashiapp.domain.repository.MessageRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

class SqlMessageRepository(
    private val database: DoVashiDatabase,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher,
) : MessageRepository {

    private val queries get() = database.messageQueries

    override fun observeMessages(conversationId: Long): Flow<List<Message>> =
        queries.selectByConversation(conversationId, ::toMessage).asFlow().mapToList(dispatcher)
            // A refused step still notifies the table; don't re-emit an identical list.
            .distinctUntilChanged()

    override suspend fun getMessage(id: Long): Message? = withContext(dispatcher) {
        queries.selectById(id, ::toMessage).executeAsOneOrNull()
    }

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
        queries.transactionWithResult {
            queries.insertMessage(
                conversationId, sourceLanguage, targetLanguage, audioPath,
                transcribedText, translatedText, reading, status.name, createdAt = now,
            )
            val id = database.conversationQueries.lastInsertRowId().executeAsOne()
            database.conversationQueries.touchConversation(updatedAt = now, id = conversationId)
            id
        }
    }

    override suspend fun markTranscribing(id: Long): Boolean = applyStep(id, MessageStep.START_TRANSCRIPTION) { next, allowed ->
        queries.startTranscription(next, id, allowed)
    }

    override suspend fun saveTranscription(
        id: Long,
        transcribedText: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): Boolean = applyStep(id, MessageStep.SAVE_TRANSCRIPTION) { next, allowed ->
        queries.saveTranscription(transcribedText, sourceLanguage, targetLanguage, next, id, allowed)
    }

    override suspend fun retryTranslation(id: Long): Boolean = applyStep(id, MessageStep.RETRY_TRANSLATION) { next, allowed ->
        queries.retryTranslation(next, id, allowed)
    }

    override suspend fun saveTranslation(id: Long, translatedText: String, reading: String?): Boolean =
        applyStep(id, MessageStep.SAVE_TRANSLATION) { next, allowed ->
            queries.saveTranslation(translatedText, reading, next, id, allowed)
        }

    override suspend fun markFailed(id: Long): Boolean = applyStep(id, MessageStep.FAIL) { next, allowed ->
        queries.updateStatus(next, id, allowed)
    }

    override suspend fun deleteRecording(id: Long): Boolean = withContext(dispatcher) {
        queries.deleteRecording(id, MessageStatus.RECORDING.name).value == 1L
    }

    override suspend fun deleteInterruptedRecordings(): List<String> = withContext(dispatcher) {
        queries.transactionWithResult {
            val references = queries.selectInterruptedRecordings(MessageStatus.RECORDING.name).executeAsList().filterNotNull()
            queries.deleteInterruptedRecordings(MessageStatus.RECORDING.name)
            references
        }
    }

    override suspend fun failInterruptedMessages(): Int = withContext(dispatcher) {
        val interrupted = listOf(MessageStatus.TRANSCRIBING, MessageStatus.TRANSLATING).map { it.name }
        queries.failInterrupted(MessageStatus.FAILED.name, interrupted).value.toInt()
    }

    /**
     * Runs [update] guarded by [step]'s allowed previous statuses; if it changed the row, bumps the Conversation's
     * `updatedAt` in the same transaction. Returns whether the step applied.
     */
    private suspend fun applyStep(
        id: Long,
        step: MessageStep,
        update: (next: String, allowed: Collection<String>) -> QueryResult<Long>,
    ): Boolean = withContext(dispatcher) {
        val now = clock.now().toEpochMilliseconds()
        queries.transactionWithResult {
            val conversationId = queries.selectConversationId(id).executeAsOneOrNull()
            if (conversationId == null) {
                false
            } else {
                val applied = update(step.next.name, step.from.map { it.name }).value == 1L
                if (applied) database.conversationQueries.touchConversation(updatedAt = now, id = conversationId)
                applied
            }
        }
    }
}

private fun toMessage(
    id: Long,
    conversationId: Long,
    sourceLanguage: String?,
    targetLanguage: String?,
    audioPath: String?,
    transcribedText: String?,
    translatedText: String?,
    reading: String?,
    status: String,
    createdAt: Long,
) = Message(
    id, conversationId, sourceLanguage, targetLanguage, audioPath,
    transcribedText, translatedText, reading, parseStatus(status), createdAt,
)
