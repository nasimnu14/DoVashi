package com.example.dovashiapp.domain.repository

import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.ConversationSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface ConversationRepository {
    fun observeSummaries(): Flow<List<ConversationSummary>>

    /** Emits null when no Conversation has this id. */
    fun observeConversation(id: Long): Flow<Conversation?>

    suspend fun getConversation(id: Long): Conversation? = observeConversation(id).first()

    suspend fun createConversation(title: String, language1Code: String, language2Code: String): Long

    suspend fun hasConversations(): Boolean
}
