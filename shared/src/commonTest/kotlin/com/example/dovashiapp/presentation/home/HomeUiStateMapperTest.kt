package com.example.dovashiapp.presentation.home

import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.testing.summary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class HomeUiStateMapperTest {

    @Test
    fun zeroMessagesShowsPlaceholder() {
        assertEquals("No messages yet", previewText(summary()))
    }

    @Test
    fun textWinsOverStatusPlaceholder() {
        val s = summary(lastMessageText = "你好吗？", lastMessageStatus = MessageStatus.COMPLETED)
        assertEquals("你好吗？", previewText(s))
        assertEquals("你好吗？", previewText(s.copy(lastMessageStatus = MessageStatus.FAILED)))
    }

    @Test
    fun placeholderByStatusWhenTextMissing() {
        fun preview(status: MessageStatus, text: String? = null) =
            previewText(summary(lastMessageText = text, lastMessageStatus = status))
        assertEquals("Recording…", preview(MessageStatus.RECORDING))
        assertEquals("Transcribing…", preview(MessageStatus.TRANSCRIBING))
        assertEquals("Failed", preview(MessageStatus.FAILED))
        assertEquals("", preview(MessageStatus.TRANSLATING))
        assertEquals("", preview(MessageStatus.COMPLETED, text = "   "))
    }

    @Test
    fun pairLabelUsesCatalogNamesInStoredOrder() {
        val label = pairLabel(summary(language1Code = "zh", language2Code = "en").conversation, LanguageCatalog::byCode)
        assertEquals("Mandarin Chinese ↔ English", label)
    }

    @Test
    fun pairLabelFallsBackToRawCodeForUnknownLanguage() {
        val label = pairLabel(summary(language1Code = "en", language2Code = "xx").conversation, LanguageCatalog::byCode)
        assertEquals("English ↔ xx", label)
    }

    @Test
    fun pairLabelReadsAnyLanguageFromTheLookup() {
        val lookup = { code: String -> if (code == "ja") Language("ja", "Japanese", "日本語") else null }
        val label = pairLabel(summary(language1Code = "ja", language2Code = "ko").conversation, lookup)
        assertEquals("Japanese ↔ ko", label)
    }

    @Test
    fun emptyListMapsToEmptyState() {
        val state = buildHomeUiState(emptyList(), LanguageCatalog::byCode, Instant.fromEpochMilliseconds(0), TimeZone.UTC)
        assertEquals(HomeUiState.Empty, state)
    }

    @Test
    fun contentRowsCarryAllFieldsInInputOrder() {
        val now = Instant.parse("2026-10-08T12:00:00Z")
        val summaries = listOf(
            summary(
                id = 7, title = "English ↔ Mandarin Chinese",
                updatedAt = Instant.parse("2026-10-08T11:55:00Z").toEpochMilliseconds(),
                lastMessageText = "Hello", lastMessageStatus = MessageStatus.COMPLETED, messageCount = 4,
            ),
            summary(id = 3, updatedAt = Instant.parse("2026-10-07T10:00:00Z").toEpochMilliseconds()),
        )
        val state = buildHomeUiState(summaries, LanguageCatalog::byCode, now, TimeZone.UTC)
        val rows = assertIs<HomeUiState.Content>(state).rows
        assertEquals(listOf(7L, 3L), rows.map { it.id })
        assertEquals(
            ConversationRowUi(7, "English ↔ Mandarin Chinese", "English ↔ Mandarin Chinese", "Hello", "5 min ago", 4),
            rows[0],
        )
        assertEquals("No messages yet", rows[1].preview)
        assertEquals("Yesterday", rows[1].timeLabel)
        assertEquals(0L, rows[1].messageCount)
    }
}
