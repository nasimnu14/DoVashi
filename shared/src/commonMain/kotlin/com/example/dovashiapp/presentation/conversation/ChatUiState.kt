package com.example.dovashiapp.presentation.conversation

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data object NotFound : ChatUiState

    /** [bubbles] are newest first, for a reversed list. [micMessage] explains a refused or failed Recording. */
    data class Content(
        val title: String,
        val bubbles: List<MessageBubbleUi>,
        val mic: MicUi = MicUi.Idle,
        val micMessage: String? = null,
    ) : ChatUiState
}

sealed interface MicUi {
    data object Idle : MicUi
    data class Recording(val startedAtMillis: Long) : MicUi
    /** Another Conversation is recording. */
    data object Unavailable : MicUi
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
