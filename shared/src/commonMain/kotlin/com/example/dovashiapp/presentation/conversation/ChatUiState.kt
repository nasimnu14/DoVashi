package com.example.dovashiapp.presentation.conversation

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data object NotFound : ChatUiState

    /** [bubbles] are newest first, for a reversed list. */
    data class Content(val title: String, val bubbles: List<MessageBubbleUi>) : ChatUiState
}

enum class BubbleSide { START, END }

enum class BubblePlayback { NONE, PLAY, PAUSE, UNAVAILABLE }

/** One Message, already formatted for display. Null fields are not shown. */
data class MessageBubbleUi(
    val id: Long,
    val side: BubbleSide,
    val sourceLabel: String?,
    val originalText: String?,
    val targetLabel: String?,
    val translatedText: String?,
    val reading: String?,
    val statusLabel: String?,
    val isStatusError: Boolean,
    val playback: BubblePlayback,
)
