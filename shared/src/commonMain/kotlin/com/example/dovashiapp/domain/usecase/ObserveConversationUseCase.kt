package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow

class ObserveConversationUseCase(private val repository: ConversationRepository) {
    operator fun invoke(id: Long): Flow<Conversation?> = repository.observeConversation(id)
}
