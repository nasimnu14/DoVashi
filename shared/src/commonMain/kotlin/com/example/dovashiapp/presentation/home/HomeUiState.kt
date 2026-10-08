package com.example.dovashiapp.presentation.home

data class ConversationRowUi(
    val id: Long,
    val title: String,
    val pairLabel: String,
    val preview: String,
    val timeLabel: String,
    val messageCount: Long,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Content(val rows: List<ConversationRowUi>) : HomeUiState
}
