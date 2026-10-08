package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.ConversationSummary
import com.example.dovashiapp.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow

class ObserveConversationSummariesUseCase(private val repository: ConversationRepository) {
    operator fun invoke(): Flow<List<ConversationSummary>> = repository.observeSummaries()
}
