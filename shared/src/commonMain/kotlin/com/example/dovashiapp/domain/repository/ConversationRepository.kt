package com.example.dovashiapp.domain.repository

import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.ConversationSummary
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeSummaries(): Flow<List<ConversationSummary>>

    /** Emits null when no Conversation has this id. */
    fun observeConversation(id: Long): Flow<Conversation?>

    suspend fun createConversation(title: String, language1Code: String, language2Code: String): Long

    suspend fun hasConversations(): Boolean
}
