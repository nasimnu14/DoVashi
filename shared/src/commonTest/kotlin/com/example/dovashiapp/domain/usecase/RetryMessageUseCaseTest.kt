package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.service.SpeechToTextException
import com.example.dovashiapp.domain.service.Transcription
import com.example.dovashiapp.domain.service.TranslationException
import com.example.dovashiapp.testing.PipelineFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class RetryMessageUseCaseTest {

    @Test
    fun translationFailureRetriesFromTheSavedTranscriptWithoutTranscribingAgain() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.translator.failure = TranslationException(TranslationException.Reason.NETWORK)
        val id = f.recordingMessage()
        f.process(id, 1, "audio/a.m4a")
        assertEquals(MessageStatus.FAILED, f.messages.getMessage(id)!!.status)
        assertEquals("Good morning", f.messages.getMessage(id)!!.transcribedText)

        f.translator.failure = null
        f.retryMessage(id)

        val m = f.messages.getMessage(id)!!
        assertEquals(MessageStatus.COMPLETED, m.status)
        assertEquals("T(Good morning)", m.translatedText)
        assertEquals("audio/a.m4a", m.audioPath)
        assertEquals(1, f.speechToText.calls.size, "the Recording is not transcribed again")
    }

    @Test
    fun transcriptionFailureRetriesFromTheSavedRecording() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.speechToText.failure = SpeechToTextException(SpeechToTextException.Reason.NETWORK)
        val id = f.recordingMessage()
        f.process(id, 1, "audio/a.m4a")
        assertEquals(MessageStatus.FAILED, f.messages.getMessage(id)!!.status)
        assertNull(f.messages.getMessage(id)!!.transcribedText)

        f.speechToText.failure = null
        f.retryMessage(id)

        assertEquals(MessageStatus.COMPLETED, f.messages.getMessage(id)!!.status)
        assertEquals(listOf("audio/a.m4a", "audio/a.m4a"), f.speechToText.calls)
    }

    @Test
    fun aRetryThatFailsAgainKeepsEverythingSaved() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.translator.failure = TranslationException(TranslationException.Reason.SERVER)
        val id = f.recordingMessage()
        f.process(id, 1, "audio/a.m4a")

        f.retryMessage(id)

        val m = f.messages.getMessage(id)!!
        assertEquals(MessageStatus.FAILED, m.status)
        assertEquals("Good morning", m.transcribedText)
        assertEquals("audio/a.m4a", m.audioPath)
        assertEquals(2, f.translator.requests.size)
        // And it can be retried again.
        f.translator.failure = null
        f.retryMessage(id)
        assertEquals(MessageStatus.COMPLETED, f.messages.getMessage(id)!!.status)
    }

    @Test
    fun silenceRetriesFromTheRecordingAndFailsAgain() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.speechToText.result = Transcription("  ", "aa")
        val id = f.recordingMessage()
        f.process(id, 1, "audio/a.m4a")

        f.retryMessage(id)

        assertEquals(MessageStatus.FAILED, f.messages.getMessage(id)!!.status)
        assertEquals(2, f.speechToText.calls.size)
    }

    @Test
    fun messagesThatAreNotFailedAreLeftAlone() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val completed = f.messages.insertMessage(1, MessageStatus.COMPLETED, audioPath = "audio/a.m4a", transcribedText = "x",
            translatedText = "y", sourceLanguage = "aa", targetLanguage = "bb")
        val translating = f.messages.insertMessage(1, MessageStatus.TRANSLATING, audioPath = "audio/b.m4a")

        f.retryMessage(completed)
        f.retryMessage(translating)
        f.retryMessage(999)

        assertEquals(MessageStatus.COMPLETED, f.messages.getMessage(completed)!!.status)
        assertEquals(MessageStatus.TRANSLATING, f.messages.getMessage(translating)!!.status)
        assertTrue(f.speechToText.calls.isEmpty() && f.translator.requests.isEmpty())
    }

    @Test
    fun failedMessageWithNothingToResumeFromIsLeftFailed() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val id = f.messages.insertMessage(1, MessageStatus.FAILED)

        f.retryMessage(id)

        assertEquals(MessageStatus.FAILED, f.messages.getMessage(id)!!.status)
        assertTrue(f.speechToText.calls.isEmpty())
    }
}
