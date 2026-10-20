package com.example.dovashiapp.domain.repository

import com.example.dovashiapp.domain.model.Message
import com.example.dovashiapp.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    /** The Conversation's Messages, newest first (`createdAt DESC`, then `id DESC`). */
    fun observeMessages(conversationId: Long): Flow<List<Message>>

    /** Inserts the message and bumps the parent conversation's `updatedAt` in one transaction. */
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
}
