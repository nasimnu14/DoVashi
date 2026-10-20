package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.MAX_MESSAGE_TEXT_LENGTH
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.testing.FakeMessageRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class MessageStepUseCasesTest {
    private val plain = Language("aa", "Alpha", "Alpha")
    private val script = Language("bb", "Beta", "Beta", readingSystem = "Test Romanization")
    private val byCode: (String) -> Language? = { code -> listOf(plain, script).find { it.code == code } }
    private val repository = FakeMessageRepository()
    private val markTranscribing = MarkMessageTranscribingUseCase(repository)
    private val saveTranscription = SaveTranscriptionUseCase(repository)
    private val saveTranslation = SaveTranslationUseCase(repository, byCode)
    private val markFailed = MarkMessageFailedUseCase(repository)
    private val retryTranslation = RetryTranslationUseCase(repository)

    private suspend fun translatingMessage(source: String, target: String): Long {
        val id = repository.insertMessage(1, MessageStatus.RECORDING, audioPath = "audio/a.m4a")
        assertTrue(markTranscribing(id))
        assertTrue(saveTranscription(id, "  hello  ", source, target))
        return id
    }

    @Test
    fun transcriptIsTrimmedAndStoredWithBothCodes() = runTest {
        val id = translatingMessage("aa", "bb")
        val message = repository.getMessage(id)!!
        assertEquals("hello", message.transcribedText)
        assertEquals("aa" to "bb", message.sourceLanguage to message.targetLanguage)
        assertEquals(MessageStatus.TRANSLATING, message.status)
    }

    @Test
    fun transcriptWithNothingVisibleIsNotAppliedAndDoesNotThrow() = runTest {
        val id = repository.insertMessage(1, MessageStatus.TRANSCRIBING)
        assertFalse(saveTranscription(id, "  ", "aa", "bb"))
        assertFalse(saveTranscription(id, "\u200B", "aa", "bb"))
        assertEquals(MessageStatus.TRANSCRIBING, repository.getMessage(id)!!.status)
    }

    @Test
    fun invalidLanguageCodesAreRejectedBeforeWriting() = runTest {
        val id = repository.insertMessage(1, MessageStatus.TRANSCRIBING)
        assertFailsWith<IllegalArgumentException> { saveTranscription(id, "hi", " ", "bb") }
        assertFailsWith<IllegalArgumentException> { saveTranscription(id, "hi", "aa", "") }
        assertFailsWith<IllegalArgumentException> { saveTranscription(id, "hi", "aa", "aa") }
        assertEquals(MessageStatus.TRANSCRIBING, repository.getMessage(id)!!.status)
    }

    @Test
    fun readingIsKeptForATargetThatRequiresOne() = runTest {
        val id = translatingMessage("aa", "bb")
        assertTrue(saveTranslation(id, " bonjour ", " bõ-zhur "))
        val message = repository.getMessage(id)!!
        assertEquals("bonjour", message.translatedText)
        assertEquals("bõ-zhur", message.reading)
        assertEquals(MessageStatus.COMPLETED, message.status)
    }

    @Test
    fun readingIsDroppedForATargetThatDoesNotRequireOne() = runTest {
        val id = translatingMessage("bb", "aa")
        assertTrue(saveTranslation(id, "hello", "should not be stored"))
        assertNull(repository.getMessage(id)!!.reading)
    }

    @Test
    fun nonLatinReadingIsDroppedButTheMessageStillCompletes() = runTest {
        val id = translatingMessage("aa", "bb")
        assertTrue(saveTranslation(id, "你好", "你好"))
        val message = repository.getMessage(id)!!
        assertEquals(MessageStatus.COMPLETED, message.status)
        assertNull(message.reading)
    }

    @Test
    fun missingReadingStillCompletes() = runTest {
        val id = translatingMessage("aa", "bb")
        assertTrue(saveTranslation(id, "x", null))
        assertEquals(MessageStatus.COMPLETED, repository.getMessage(id)!!.status)
    }

    @Test
    fun overLongTextIsNotAppliedAndOverLongReadingIsDropped() = runTest {
        val tooLong = "x".repeat(MAX_MESSAGE_TEXT_LENGTH + 1)
        val transcribing = repository.insertMessage(1, MessageStatus.TRANSCRIBING)
        assertFalse(saveTranscription(transcribing, tooLong, "aa", "bb"))
        val id = translatingMessage("aa", "bb")
        assertFalse(saveTranslation(id, tooLong, null))
        assertTrue(saveTranslation(id, "ok", tooLong))
        assertNull(repository.getMessage(id)!!.reading)
    }

    @Test
    fun transcriptionCannotStartWithoutARecording() = runTest {
        val id = repository.insertMessage(1, MessageStatus.FAILED, audioPath = null)
        assertFalse(markTranscribing(id))
    }

    @Test
    fun blankTranslationIsNotApplied() = runTest {
        val id = translatingMessage("aa", "bb")
        assertFalse(saveTranslation(id, " ", null))
        assertEquals(MessageStatus.TRANSLATING, repository.getMessage(id)!!.status)
    }

    @Test
    fun lateTranscriptCannotReviveAFailedMessage() = runTest {
        val id = repository.insertMessage(1, MessageStatus.RECORDING, audioPath = "audio/a.m4a")
        assertTrue(markTranscribing(id))
        assertTrue(markFailed(id))
        assertFalse(saveTranscription(id, "late", "aa", "bb"))
        assertEquals(MessageStatus.FAILED, repository.getMessage(id)!!.status)
    }

    @Test
    fun retryTranslationNeedsASavedTranscript() = runTest {
        val withTranscript = translatingMessage("aa", "bb")
        markFailed(withTranscript)
        val withoutTranscript = repository.insertMessage(1, MessageStatus.FAILED, audioPath = "audio/b.m4a")
        assertTrue(retryTranslation(withTranscript))
        assertFalse(retryTranslation(withoutTranscript))
        assertEquals(MessageStatus.TRANSLATING, repository.getMessage(withTranscript)!!.status)
    }

    @Test
    fun unknownMessageIsNotApplied() = runTest {
        assertFalse(markTranscribing(42))
        assertFalse(saveTranscription(42, "hi", "aa", "bb"))
        assertFalse(saveTranslation(42, "hi", null))
        assertFalse(markFailed(42))
        assertFalse(retryTranslation(42))
    }

    @Test
    fun failureKeepsEarlierParts() = runTest {
        val id = translatingMessage("aa", "bb")
        assertTrue(markFailed(id))
        val message = repository.getMessage(id)!!
        assertEquals(MessageStatus.FAILED, message.status)
        assertEquals("audio/a.m4a", message.audioPath)
        assertEquals("hello", message.transcribedText)
    }
}
