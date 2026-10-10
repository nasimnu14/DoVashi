package com.example.dovashiapp.domain.repository

import com.example.dovashiapp.domain.model.Message
import com.example.dovashiapp.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    /** The Conversation's Messages, newest first (`createdAt DESC`, then `id DESC`). */
    fun observeMessages(conversationId: Long): Flow<List<Message>>

    /**
     * Inserts the message and bumps the parent conversation's `updatedAt` in one transaction. The pipeline creates
     * Messages as RECORDING; other statuses are only for seeding and tests (no transition guard applies).
     */
    suspend fun insertMessage(
        conversationId: Long,
        status: MessageStatus,
        sourceLanguage: String? = null,
        targetLanguage: String? = null,
        audioPath: String? = null,
        transcribedText: String? = null,
        translatedText: String? = null,
        reading: String? = null,
    ): Long

    suspend fun getMessage(id: Long): Message?

    // Step writes (see MessageStep). Each applies only from its allowed previous Message Status, sets only its own
    // parts, bumps the parent Conversation's `updatedAt` in the same transaction, and returns whether it applied.
    // The guards stop out-of-order steps, not steps from a superseded attempt: callers must run at most one
    // attempt per Message at a time (cancel and join the previous one before retrying).

    /** The Recording is saved, or a retry restarts from it; transcription starts. Needs an `audioPath`. */
    suspend fun markTranscribing(id: Long): Boolean

    /** Stores the transcript and both Language Codes; moves to TRANSLATING. */
    suspend fun saveTranscription(id: Long, transcribedText: String, sourceLanguage: String, targetLanguage: String): Boolean

    /** Retry from the saved transcript: FAILED → TRANSLATING, only if the transcript and both codes are present. */
    suspend fun retryTranslation(id: Long): Boolean

    /** Stores the translation and Reading; moves to COMPLETED. */
    suspend fun saveTranslation(id: Long, translatedText: String, reading: String?): Boolean

    /** Moves to FAILED, keeping every part saved so far. */
    suspend fun markFailed(id: Long): Boolean
}
