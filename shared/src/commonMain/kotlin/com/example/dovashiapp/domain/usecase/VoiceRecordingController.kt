package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.audio.AudioRecorder
import com.example.dovashiapp.audio.FileStorage
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.repository.MessageRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface RecordingState {
    data object Idle : RecordingState

    /** The Message is being created and the microphone opened; a stop now waits for this to finish. */
    data class Starting(val conversationId: Long, val startedAtMillis: Long) : RecordingState

    data class Recording(val conversationId: Long, val messageId: Long, val startedAtMillis: Long) : RecordingState
}

val RecordingState.conversationId: Long?
    get() = when (this) {
        RecordingState.Idle -> null
        is RecordingState.Starting -> conversationId
        is RecordingState.Recording -> conversationId
    }

enum class StartResult { STARTED, ALREADY_RECORDING, FAILED }

enum class StopResult { NOT_RECORDING, PROCESSING, TOO_SHORT, FAILED }

/**
 * The one Recording in progress, app-wide. Starting inserts the Message (RECORDING, with its Recording reference)
 * before the microphone opens. Stopping hands a usable Recording to [MessageProcessor] and discards an accidental tap
 * or a broken Recording. Start and stop run to completion even if the caller is cancelled, so a Message row and
 * the microphone never get out of step.
 */
class VoiceRecordingController(
    private val recorder: AudioRecorder,
    private val fileStorage: FileStorage,
    private val messages: MessageRepository,
    private val processor: MessageProcessor,
    private val clock: Clock,
    private val scope: CoroutineScope,
    /** The recorder is driven from here (Android's MediaRecorder must stay on one thread). */
    private val mainDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()
    private var current: Active? = null
    private val _lostRecordings = MutableSharedFlow<Long>(extraBufferCapacity = 8)

    /**
     * Conversation ids whose Recording was lost (the recorder produced nothing usable), whoever stopped it: the user,
     * the recorder itself, or the app leaving the foreground. Accidental taps are not reported.
     */
    val lostRecordings: SharedFlow<Long> = _lostRecordings.asSharedFlow()

    private class Active(val conversationId: Long, val messageId: Long, val reference: String, val startedAtMillis: Long)

    suspend fun start(conversationId: Long): StartResult = withContext(NonCancellable) {
        val startedAt = clock.now().toEpochMilliseconds()
        // Visible at once, so a quick second tap is read as "stop", not as another start.
        if (!_state.compareAndSet(RecordingState.Idle, RecordingState.Starting(conversationId, startedAt))) {
            return@withContext StartResult.ALREADY_RECORDING
        }
        mutex.withLock {
            val result = try {
                begin(conversationId, startedAt)
            } catch (e: Exception) {
                null // e.g. the database is full
            }
            if (result == null) {
                _state.value = RecordingState.Idle
                StartResult.FAILED
            } else {
                current = result
                _state.value = RecordingState.Recording(conversationId, result.messageId, startedAt)
                StartResult.STARTED
            }
        }
    }

    private suspend fun begin(conversationId: Long, startedAt: Long): Active? {
        val reference = fileStorage.newRecordingReference()
        val path = fileStorage.writablePath(reference) ?: return null
        val messageId = messages.insertMessage(conversationId, MessageStatus.RECORDING, audioPath = reference)
        val started = try {
            withContext(mainDispatcher) {
                recorder.start(path, MAX_DURATION_MILLIS) { scope.launch { stop(messageId) } }
            }
        } catch (e: Exception) {
            false
        }
        if (!started) {
            discard(messageId, reference)
            return null
        }
        return Active(conversationId, messageId, reference, startedAt)
    }

    /** Stops the Recording in progress (waiting for a start to finish first). */
    suspend fun stop(): StopResult = stop(messageId = null)

    /** Stops only if [messageId] (when given) is still the Recording in progress. */
    private suspend fun stop(messageId: Long?): StopResult = withContext(NonCancellable) {
        mutex.withLock {
            val active = current?.takeIf { messageId == null || it.messageId == messageId }
                ?: return@withLock StopResult.NOT_RECORDING
            current = null
            val duration = try {
                withContext(mainDispatcher) { recorder.stop() }
            } catch (e: Exception) {
                null
            }
            _state.value = RecordingState.Idle
            val elapsed = clock.now().toEpochMilliseconds() - active.startedAtMillis
            when {
                duration != null && duration >= MIN_DURATION_MILLIS -> {
                    processor.processRecording(active.messageId, active.conversationId, active.reference)
                    StopResult.PROCESSING
                }
                else -> {
                    safely { discard(active.messageId, active.reference) }
                    if (duration == null && elapsed >= MIN_DURATION_MILLIS) {
                        _lostRecordings.tryEmit(active.conversationId)
                        StopResult.FAILED
                    } else {
                        StopResult.TOO_SHORT
                    }
                }
            }
        }
    }

    /** Fire-and-forget [stop] for a Recording in [conversationId], from code that has no scope left (onCleared). */
    fun stopIfRecording(conversationId: Long) {
        if (_state.value.conversationId == conversationId) scope.launch { stop() }
    }

    /** Fire-and-forget [stop] of whatever is recording, e.g. when the app leaves the foreground. */
    fun stopAny() {
        if (_state.value != RecordingState.Idle) scope.launch { stop() }
    }

    // File first: if the process dies in between, the startup sweep removes the leftover RECORDING row.
    private suspend fun discard(messageId: Long, reference: String) {
        fileStorage.delete(reference)
        messages.deleteRecording(messageId)
    }

    private suspend inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
        }
    }

    companion object {
        const val MAX_DURATION_MILLIS = 120_000L
        const val MIN_DURATION_MILLIS = 500L
    }
}
