package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.testing.PipelineFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class VoiceRecordingControllerTest {

    private fun onlyMessage(f: PipelineFixture) = f.messages.messages.value.values.flatten().singleOrNull()

    @Test
    fun startInsertsARecordingMessageAndOpensTheMicrophone() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))

        assertEquals(StartResult.STARTED, f.controller.start(1))

        val message = onlyMessage(f)!!
        assertEquals(MessageStatus.RECORDING, message.status)
        assertEquals("audio/rec-1.m4a", message.audioPath)
        assertEquals("/fake/audio/rec-1.m4a", f.recorder.startedPath)
        val state = assertIs<RecordingState.Recording>(f.controller.state.value)
        assertEquals(1L to message.id, state.conversationId to state.messageId)
        assertEquals(f.clock.instant.toEpochMilliseconds(), state.startedAtMillis)
    }

    @Test
    fun onlyOneRecordingAtATime() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.controller.start(1)
        assertEquals(StartResult.ALREADY_RECORDING, f.controller.start(1))
        assertEquals(1, f.messages.messages.value.values.flatten().size)
    }

    @Test
    fun recorderFailureLeavesNothingBehind() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.recorder.startSucceeds = false
        assertEquals(StartResult.FAILED, f.controller.start(1))
        assertNull(onlyMessage(f))
        assertEquals(listOf("audio/rec-1.m4a"), f.files.deleted)
        assertEquals(RecordingState.Idle, f.controller.state.value)
    }

    @Test
    fun stopProcessesAUsableRecording() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.controller.start(1)
        f.controller.stop()
        assertEquals(RecordingState.Idle, f.controller.state.value)
        assertEquals(MessageStatus.COMPLETED, onlyMessage(f)!!.status)
        assertTrue(f.files.deleted.isEmpty())
    }

    @Test
    fun tooShortOrEmptyRecordingsAreDiscarded() = runTest {
        for (duration in listOf(300L, null)) {
            val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
            f.recorder.stopDuration = duration
            f.controller.start(1)
            f.controller.stop()
            assertNull(onlyMessage(f), "duration $duration")
            assertEquals(listOf("audio/rec-1.m4a"), f.files.deleted)
            assertTrue(f.speechToText.calls.isEmpty())
        }
    }

    @Test
    fun reachingTheMaximumDurationStopsAndProcesses() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.controller.start(1)
        f.recorder.onStoppedAutomatically!!.invoke()
        assertEquals(RecordingState.Idle, f.controller.state.value)
        assertEquals(1, f.recorder.stops)
        assertEquals(MessageStatus.COMPLETED, onlyMessage(f)!!.status)
    }

    @Test
    fun stopIfRecordingOnlyStopsThatConversation() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.controller.start(1)
        f.controller.stopIfRecording(2)
        assertIs<RecordingState.Recording>(f.controller.state.value)
        f.controller.stopIfRecording(1)
        assertEquals(RecordingState.Idle, f.controller.state.value)
    }

    @Test
    fun stopWithoutARecordingIsANoOp() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        assertEquals(StopResult.NOT_RECORDING, f.controller.stop())
        assertEquals(0, f.recorder.stops)
    }

    @Test
    fun theMessageExistsBeforeTheMicrophoneOpens() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        var messagesAtStart = -1
        f.recorder.onStart = { messagesAtStart = f.messages.messages.value.values.flatten().size }
        f.controller.start(1)
        assertEquals(1, messagesAtStart)
    }

    @Test
    fun startingIsVisibleAndAStopDuringItWaitsThenDiscardsAShortTap() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        f.messages.insertGate = gate
        f.recorder.stopDuration = 100
        val start = launch(UnconfinedTestDispatcher(testScheduler)) { f.controller.start(1) }
        assertIs<RecordingState.Starting>(f.controller.state.value)
        assertEquals(1L, f.controller.state.value.conversationId)
        val stop = async(UnconfinedTestDispatcher(testScheduler)) { f.controller.stop() }

        gate.complete(Unit)
        start.join()

        assertEquals(StopResult.TOO_SHORT, stop.await())
        assertEquals(RecordingState.Idle, f.controller.state.value)
        assertNull(onlyMessage(f))
    }

    @Test
    fun aBrokenLongRecordingIsReportedAsFailed() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val lost = mutableListOf<Long>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.controller.lostRecordings.collect { lost += it } }
        f.recorder.stopDuration = null
        f.controller.start(1)
        f.clock.instant += kotlin.time.Duration.parse("5s")
        assertEquals(StopResult.FAILED, f.controller.stop())
        assertNull(onlyMessage(f))
        assertEquals(listOf(1L), lost)
    }

    @Test
    fun anAutomaticStopThatLosesTheRecordingIsReported() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val lost = mutableListOf<Long>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.controller.lostRecordings.collect { lost += it } }
        f.recorder.stopDuration = null
        f.controller.start(1)
        f.clock.instant += kotlin.time.Duration.parse("60s")
        f.recorder.onStoppedAutomatically!!.invoke() // e.g. the microphone was taken away
        assertEquals(RecordingState.Idle, f.controller.state.value)
        assertEquals(listOf(1L), lost)
    }

    @Test
    fun anAccidentalTapIsNotReportedAsLost() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val lost = mutableListOf<Long>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.controller.lostRecordings.collect { lost += it } }
        f.recorder.stopDuration = null
        f.controller.start(1)
        assertEquals(StopResult.TOO_SHORT, f.controller.stop())
        assertTrue(lost.isEmpty())
    }

    @Test
    fun aThrowingRecorderLeavesNothingBehind() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.recorder.startFailure = IllegalStateException("mic busy")
        assertEquals(StartResult.FAILED, f.controller.start(1))
        assertNull(onlyMessage(f))
        assertEquals(listOf("audio/rec-1.m4a"), f.files.deleted)
        assertEquals(RecordingState.Idle, f.controller.state.value)
    }

    @Test
    fun stopAnyDuringStartingStopsOnceStarted() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        f.messages.insertGate = gate
        val start = launch(UnconfinedTestDispatcher(testScheduler)) { f.controller.start(1) }
        assertIs<RecordingState.Starting>(f.controller.state.value)
        f.controller.stopAny()
        gate.complete(Unit)
        start.join()
        assertEquals(RecordingState.Idle, f.controller.state.value)
        assertEquals(1, f.recorder.stops)
    }

    @Test
    fun aDatabaseFailureOnStartIsReportedNotThrown() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.messages.insertFailure = IllegalStateException("disk full")
        assertEquals(StartResult.FAILED, f.controller.start(1))
        assertEquals(RecordingState.Idle, f.controller.state.value)
        assertNull(f.recorder.startedPath)
    }

    @Test
    fun stopAnyStopsWhateverIsRecording() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.controller.start(1)
        f.controller.stopAny()
        assertEquals(RecordingState.Idle, f.controller.state.value)
        assertEquals(MessageStatus.COMPLETED, onlyMessage(f)!!.status)
    }

    @Test
    fun aLateAutomaticStopDoesNotStopTheNextRecording() = runTest {
        val f = PipelineFixture(UnconfinedTestDispatcher(testScheduler))
        f.controller.start(1)
        val firstCallback = f.recorder.onStoppedAutomatically!!
        f.controller.stop()
        f.controller.start(1)
        firstCallback()
        assertIs<RecordingState.Recording>(f.controller.state.value)
    }
}
