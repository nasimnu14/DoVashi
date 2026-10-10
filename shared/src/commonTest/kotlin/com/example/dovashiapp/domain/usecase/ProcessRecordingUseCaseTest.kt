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

class ProcessRecordingUseCaseTest {

    @Test
    fun completesLanguage1ToLanguage2WithReading() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val id = f.recordingMessage()

        f.process(id, 1, "audio/a.m4a")

        val m = f.messages.getMessage(id)!!
        assertEquals(MessageStatus.COMPLETED, m.status)
        assertEquals(listOf("aa", "bb", "Good morning", "T(Good morning)", "ti"),
            listOf(m.sourceLanguage, m.targetLanguage, m.transcribedText, m.translatedText, m.reading))
        assertEquals(listOf("audio/a.m4a"), f.speechToText.calls)
        assertEquals("Beta Romanization", f.translator.requests.single().readingSystem)
    }

    @Test
    fun completesLanguage2ToLanguage1WithoutReading() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.speechToText.result = Transcription("你好", null) // detected by script
        val id = f.recordingMessage()

        f.process(id, 1, "audio/a.m4a")

        val m = f.messages.getMessage(id)!!
        assertEquals(MessageStatus.COMPLETED, m.status)
        assertEquals("bb" to "aa", m.sourceLanguage to m.targetLanguage)
        assertNull(m.reading)
    }

    @Test
    fun silenceFailsAndKeepsTheRecording() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.speechToText.result = Transcription("  ", "aa")
        val id = f.recordingMessage()
        f.process(id, 1, "audio/a.m4a")
        val m = f.messages.getMessage(id)!!
        assertEquals(MessageStatus.FAILED, m.status)
        assertEquals("audio/a.m4a", m.audioPath)
        assertTrue(f.translator.requests.isEmpty())
    }

    @Test
    fun undeterminedLanguageFails() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.speechToText.result = Transcription("123 !!!", "zz")
        val id = f.recordingMessage()
        f.process(id, 1, "audio/a.m4a")
        assertEquals(MessageStatus.FAILED, f.messages.getMessage(id)!!.status)
    }

    @Test
    fun speechToTextErrorFailsWithoutATranscript() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.speechToText.failure = SpeechToTextException(SpeechToTextException.Reason.NETWORK)
        val id = f.recordingMessage()
        f.process(id, 1, "audio/a.m4a")
        val m = f.messages.getMessage(id)!!
        assertEquals(MessageStatus.FAILED, m.status)
        assertNull(m.transcribedText)
        assertEquals("audio/a.m4a", m.audioPath)
    }

    @Test
    fun translationErrorFailsButKeepsTheTranscript() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.translator.failure = TranslationException(TranslationException.Reason.SERVER)
        val id = f.recordingMessage()
        f.process(id, 1, "audio/a.m4a")
        val m = f.messages.getMessage(id)!!
        assertEquals(MessageStatus.FAILED, m.status)
        assertEquals("Good morning", m.transcribedText)
        assertEquals("aa" to "bb", m.sourceLanguage to m.targetLanguage)
    }

    @Test
    fun missingConversationFails() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val id = f.recordingMessage()
        f.process(id, 99, "audio/a.m4a")
        assertEquals(MessageStatus.FAILED, f.messages.getMessage(id)!!.status)
    }

    @Test
    fun refusedFirstStepDoesNothing() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val id = f.messages.insertMessage(1, MessageStatus.COMPLETED, "aa", "bb", "audio/a.m4a", "x", "y")
        f.process(id, 1, "audio/a.m4a")
        assertTrue(f.speechToText.calls.isEmpty())
        assertEquals(MessageStatus.COMPLETED, f.messages.getMessage(id)!!.status)
    }
}
