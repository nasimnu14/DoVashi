package com.example.dovashiapp.presentation.conversation

import com.example.dovashiapp.audio.PlaybackState
import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.testing.message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ChatUiStateMapperTest {
    private val alpha = Language("aa", "Alpha", "Alpha")
    private val beta = Language("bb", "Beta", "Beta")
    private val catalog = listOf(alpha, beta)
    private val byCode: (String) -> Language? = { code -> catalog.find { it.code == code } }
    private val conversation = Conversation(7, "Alpha ↔ Beta", "aa", "bb", 0, 0)

    private fun map(vararg messages: com.example.dovashiapp.domain.model.Message,
                    playback: PlaybackState = PlaybackState.Idle, unplayable: Map<Long, String> = emptyMap()) =
        assertIs<ChatUiState.Content>(buildChatUiState(conversation, messages.toList(), playback, unplayable, byCode))

    @Test
    fun missingConversationIsNotFound() =
        assertEquals(ChatUiState.NotFound, buildChatUiState(null, emptyList(), PlaybackState.Idle, emptyMap(), byCode))

    @Test
    fun noMessagesIsContentWithNoBubbles() {
        val state = map()
        assertEquals("Alpha ↔ Beta", state.title)
        assertEquals(emptyList(), state.bubbles)
    }

    @Test
    fun language1SourceIsStartWithLabelsFromTheRecord() {
        val bubble = map(message(1, sourceLanguage = "aa", targetLanguage = "bb", transcribedText = "Hi",
            translatedText = "Salut", reading = "sa-lu")).bubbles.single()
        assertEquals(BubbleSide.START, bubble.side)
        assertEquals("Alpha", bubble.sourceLabel)
        assertEquals("Hi", bubble.originalText)
        assertEquals("Beta", bubble.targetLabel)
        assertEquals("Salut", bubble.translatedText)
        assertEquals("sa-lu", bubble.reading)
        assertNull(bubble.statusLabel)
    }

    @Test
    fun language2SourceIsEnd() {
        val bubble = map(message(1, sourceLanguage = "bb", targetLanguage = "aa", translatedText = "x")).bubbles.single()
        assertEquals(BubbleSide.END, bubble.side)
        assertEquals("Beta", bubble.sourceLabel)
        assertEquals("Alpha", bubble.targetLabel)
    }

    @Test
    fun undetectedSourceIsStartWithoutLabel() {
        val bubble = map(message(1, sourceLanguage = null, targetLanguage = null, status = MessageStatus.TRANSCRIBING)).bubbles.single()
        assertEquals(BubbleSide.START, bubble.side)
        assertNull(bubble.sourceLabel)
        assertEquals("Transcribing…", bubble.statusLabel)
    }

    @Test
    fun unknownCodeShowsTheRawCode() {
        val bubble = map(message(1, sourceLanguage = "xx", targetLanguage = "aa", translatedText = "t")).bubbles.single()
        assertEquals("xx", bubble.sourceLabel)
        assertEquals(BubbleSide.START, bubble.side)
    }

    @Test
    fun blankTranslationHidesTheTranslationBlockAndReading() {
        val bubble = map(message(1, translatedText = " ", reading = "r", status = MessageStatus.TRANSLATING)).bubbles.single()
        assertNull(bubble.translatedText)
        assertNull(bubble.targetLabel)
        assertNull(bubble.reading)
        assertEquals("Translating…", bubble.statusLabel)
    }

    @Test
    fun blankReadingAndTextAreHidden() {
        val bubble = map(message(1, transcribedText = "", translatedText = "t", reading = "  ")).bubbles.single()
        assertNull(bubble.originalText)
        assertNull(bubble.reading)
    }

    @Test
    fun everyStatusHasItsLabel() {
        val labels = MessageStatus.entries.associateWith { status ->
            map(message(1, status = status)).bubbles.single().let { it.statusLabel to it.isStatusError }
        }
        assertEquals("Recording…" to false, labels[MessageStatus.RECORDING])
        assertEquals("Transcribing…" to false, labels[MessageStatus.TRANSCRIBING])
        assertEquals("Translating…" to false, labels[MessageStatus.TRANSLATING])
        assertEquals("Failed" to true, labels[MessageStatus.FAILED])
        assertEquals(null to false, labels[MessageStatus.COMPLETED])
    }

    @Test
    fun playbackFollowsRecordingPlayerAndUnavailableIds() {
        val state = map(
            message(4, audioPath = null),
            message(3, audioPath = "audio/c"),
            message(2, audioPath = "audio/b"),
            message(1, audioPath = "audio/a"),
            playback = PlaybackState.Playing("audio/b"),
            unplayable = mapOf(3L to "audio/c"),
        )
        assertEquals(
            listOf(BubblePlayback.NONE, BubblePlayback.UNAVAILABLE, BubblePlayback.PAUSE, BubblePlayback.PLAY),
            state.bubbles.map { it.playback },
        )
        assertEquals(listOf(4L, 3L, 2L, 1L), state.bubbles.map { it.id }, "keeps the newest-first order")
    }

    @Test
    fun aChangedReferenceIsNoLongerUnavailable() {
        val bubble = map(message(1, audioPath = "audio/new"), unplayable = mapOf(1L to "audio/old")).bubbles.single()
        assertEquals(BubblePlayback.PLAY, bubble.playback)
    }

    @Test
    fun recordingInProgressOrBlankReferenceHasNoPlayback() {
        val state = map(message(2, audioPath = "audio/a", status = MessageStatus.RECORDING), message(1, audioPath = " "))
        assertEquals(listOf(BubblePlayback.NONE, BubblePlayback.NONE), state.bubbles.map { it.playback })
    }

    @Test
    fun targetLabelComesFromTheMessageRecordNotThePair() {
        val bubble = map(message(1, sourceLanguage = "aa", targetLanguage = "xx", translatedText = "t")).bubbles.single()
        assertEquals("xx", bubble.targetLabel)
    }

    @Test
    fun pausedRecordingShowsPlay() {
        val bubble = map(message(1, audioPath = "audio/a"), playback = PlaybackState.Paused("audio/a")).bubbles.single()
        assertEquals(BubblePlayback.PLAY, bubble.playback)
    }
}
