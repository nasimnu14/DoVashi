package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.Message
import com.example.dovashiapp.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow

class ObserveMessagesUseCase(private val repository: MessageRepository) {
    /** Newest first. */
    operator fun invoke(conversationId: Long): Flow<List<Message>> = repository.observeMessages(conversationId)
}
