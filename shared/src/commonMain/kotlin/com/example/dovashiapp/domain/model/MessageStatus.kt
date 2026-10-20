package com.example.dovashiapp.domain.model

/** Message Status. RECORDING is only ever the initial status; COMPLETED is final. */
enum class MessageStatus {
    RECORDING,
    TRANSCRIBING,
    TRANSLATING,
    COMPLETED,
    FAILED;

    /** Whether some [MessageStep] moves a Message from this status to [next]. */
    fun canMoveTo(next: MessageStatus): Boolean = MessageStep.entries.any { it.next == next && this in it.from }
}

/**
 * The writes that move a Message along its Message Status, each accepted only from [from]. Steps that carry
 * pipeline output accept only the stage that produces it, so a late result can't revive a Failed Message;
 * leaving FAILED is always an explicit retry step.
 */
enum class MessageStep(val next: MessageStatus, val from: Set<MessageStatus>) {
    /** The Recording is saved (or a retry restarts from it). */
    START_TRANSCRIPTION(MessageStatus.TRANSCRIBING, setOf(MessageStatus.RECORDING, MessageStatus.FAILED)),
    SAVE_TRANSCRIPTION(MessageStatus.TRANSLATING, setOf(MessageStatus.TRANSCRIBING)),
    /** Retry from a saved transcript. */
    RETRY_TRANSLATION(MessageStatus.TRANSLATING, setOf(MessageStatus.FAILED)),
    SAVE_TRANSLATION(MessageStatus.COMPLETED, setOf(MessageStatus.TRANSLATING)),
    FAIL(MessageStatus.FAILED, setOf(MessageStatus.RECORDING, MessageStatus.TRANSCRIBING, MessageStatus.TRANSLATING)),
}
