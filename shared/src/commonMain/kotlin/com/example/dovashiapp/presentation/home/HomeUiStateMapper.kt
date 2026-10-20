package com.example.dovashiapp.presentation.home

import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.ConversationSummary
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.presentation.languageLabel
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

fun previewText(summary: ConversationSummary): String {
    val status = summary.lastMessageStatus ?: return "No messages yet"
    val text = summary.lastMessageText
    if (!text.isNullOrBlank()) return text
    return when (status) {
        MessageStatus.RECORDING -> "Recording…"
        MessageStatus.TRANSCRIBING -> "Transcribing…"
        MessageStatus.FAILED -> "Failed"
        MessageStatus.TRANSLATING, MessageStatus.COMPLETED -> ""
    }
}

fun pairLabel(conversation: Conversation, languageByCode: (String) -> Language?): String =
    "${languageLabel(conversation.language1Code, languageByCode)} ↔ ${languageLabel(conversation.language2Code, languageByCode)}"

fun buildHomeUiState(
    summaries: List<ConversationSummary>,
    languageByCode: (String) -> Language?,
    now: Instant,
    zone: TimeZone,
): HomeUiState {
    if (summaries.isEmpty()) return HomeUiState.Empty
    return HomeUiState.Content(
        summaries.map { summary ->
            val conversation = summary.conversation
            ConversationRowUi(
                id = conversation.id,
                title = conversation.title,
                pairLabel = pairLabel(conversation, languageByCode),
                preview = previewText(summary),
                timeLabel = formatRelativeTime(Instant.fromEpochMilliseconds(conversation.updatedAt), now, zone),
                messageCount = summary.messageCount,
            )
        },
    )
}
