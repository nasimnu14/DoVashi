package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.testing.PipelineFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class MessageProcessorTest {

    @Test
    fun aSecondRunForTheSameMessageCancelsTheFirstBeforeStarting() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        f.speechToText.gate = gate
        val id = f.recordingMessage()

        val first = f.processor.processRecording(id, 1, "audio/a.m4a")
        assertEquals(1, f.speechToText.calls.size, "first job is mid-flight")
        f.speechToText.gate = null
        val second = f.processor.processRecording(id, 1, "audio/a.m4a")
        second.join()

        assertTrue(first.isCancelled)
        // The second run found the Message TRANSCRIBING (left by the first), so its first step was refused.
        assertEquals(MessageStatus.TRANSCRIBING, f.messages.getMessage(id)!!.status)
        gate.complete(Unit)
        assertEquals(1, f.speechToText.calls.size, "the cancelled job never resumed to write")
    }

    @Test
    fun differentMessagesRunConcurrently() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        f.speechToText.gate = gate
        val a = f.recordingMessage("audio/a.m4a")
        val b = f.recordingMessage("audio/b.m4a")

        val jobA = f.processor.processRecording(a, 1, "audio/a.m4a")
        val jobB = f.processor.processRecording(b, 1, "audio/b.m4a")
        // Both are mid-flight at once: neither cancelled nor queued behind the other.
        assertEquals(listOf("audio/a.m4a", "audio/b.m4a"), f.speechToText.calls)
        gate.complete(Unit)
        jobA.join()
        jobB.join()

        assertEquals(MessageStatus.COMPLETED, f.messages.getMessage(a)!!.status)
        assertEquals(MessageStatus.COMPLETED, f.messages.getMessage(b)!!.status)
    }
}
