package com.example.dovashiapp.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs Message processing in an app-wide [scope], so it finishes even when the user leaves the screen. At most one
 * job per Message: the newest call wins, cancelling and waiting for the previous job before doing anything, so a
 * superseded attempt can never write.
 */
class MessageProcessor(
    private val scope: CoroutineScope,
    private val processRecordingUseCase: ProcessRecordingUseCase,
    private val retryMessageUseCase: RetryMessageUseCase,
) {
    private val mutex = Mutex()
    private val jobs = mutableMapOf<Long, Job>()

    suspend fun processRecording(messageId: Long, conversationId: Long, recordingReference: String): Job =
        launchReplacing(messageId) { processRecordingUseCase(messageId, conversationId, recordingReference) }

    /** Retries a Failed Message. A Message already being processed is left alone (a double tap must not cancel the attempt). */
    suspend fun retry(messageId: Long): Job = launchReplacing(messageId, replaceRunning = false) { retryMessageUseCase(messageId) }

    // Registered under the lock in call order, started after releasing it (the job never runs while we hold the
    // lock). A later call cancels and joins this job first; cancelling a job that hasn't started just completes it.
    private suspend fun launchReplacing(messageId: Long, replaceRunning: Boolean = true, block: suspend () -> Unit): Job = mutex.withLock {
        jobs.values.removeAll { it.isCompleted }
        val previous = jobs[messageId]
        if (!replaceRunning && previous != null) return@withLock previous
        scope.launch(start = CoroutineStart.LAZY) {
            // Even if this job is itself superseded, wait for the previous one to finish (it may be mid-write).
            withContext(NonCancellable) { previous?.cancelAndJoin() }
            block()
        }.also { jobs[messageId] = it }
    }.also { it.start() }
}
