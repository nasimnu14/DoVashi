package com.example.dovashiapp.presentation.conversation

import com.example.dovashiapp.audio.PlaybackState
import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.Message
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.presentation.languageLabel

fun buildChatUiState(
    conversation: Conversation?,
    messages: List<Message>,
    playback: PlaybackState,
    unplayableRecordings: Map<Long, String>,
    languageByCode: (String) -> Language?,
): ChatUiState {
    if (conversation == null) return ChatUiState.NotFound
    return ChatUiState.Content(
        title = conversation.title,
        bubbles = messages.map { bubble(it, conversation, playback, unplayableRecordings, languageByCode) },
    )
}

private fun bubble(
    message: Message,
    conversation: Conversation,
    playback: PlaybackState,
    unplayableRecordings: Map<Long, String>,
    languageByCode: (String) -> Language?,
): MessageBubbleUi {
    val translated = message.translatedText?.takeIf { it.isNotBlank() }
    return MessageBubbleUi(
        id = message.id,
        side = if (message.sourceLanguage == conversation.language2Code) BubbleSide.END else BubbleSide.START,
        sourceLabel = message.sourceLanguage?.let { languageLabel(it, languageByCode) },
        originalText = message.transcribedText?.takeIf { it.isNotBlank() },
        targetLabel = if (translated != null) message.targetLanguage?.let { languageLabel(it, languageByCode) } else null,
        translatedText = translated,
        reading = if (translated != null) message.reading?.takeIf { it.isNotBlank() } else null,
        statusLabel = statusLabel(message.status),
        isStatusError = message.status == MessageStatus.FAILED,
        playback = playbackOf(message, playback, unplayableRecordings),
    )
}

private fun statusLabel(status: MessageStatus): String? = when (status) {
    MessageStatus.RECORDING -> "Recording…"
    MessageStatus.TRANSCRIBING -> "Transcribing…"
    MessageStatus.TRANSLATING -> "Translating…"
    MessageStatus.FAILED -> "Failed"
    MessageStatus.COMPLETED -> null
}

private fun playbackOf(message: Message, playback: PlaybackState, unplayableRecordings: Map<Long, String>): BubblePlayback {
    val path = message.audioPath?.takeIf { it.isNotBlank() } ?: return BubblePlayback.NONE
    // The file is still being written while recording.
    if (message.status == MessageStatus.RECORDING) return BubblePlayback.NONE
    return when {
        unplayableRecordings[message.id] == path -> BubblePlayback.UNAVAILABLE
        playback == PlaybackState.Playing(path) -> BubblePlayback.PAUSE
        else -> BubblePlayback.PLAY
    }
}
