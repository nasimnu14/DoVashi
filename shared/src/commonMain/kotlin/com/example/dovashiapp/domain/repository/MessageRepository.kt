package com.example.dovashiapp.domain.repository

import com.example.dovashiapp.domain.model.MessageStatus

interface MessageRepository {
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
