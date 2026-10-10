package com.example.dovashiapp.domain.model

import com.example.dovashiapp.domain.model.MessageStatus.COMPLETED
import com.example.dovashiapp.domain.model.MessageStatus.FAILED
import com.example.dovashiapp.domain.model.MessageStatus.RECORDING
import com.example.dovashiapp.domain.model.MessageStatus.TRANSCRIBING
import com.example.dovashiapp.domain.model.MessageStatus.TRANSLATING
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageStatusTest {
    private val allowed = setOf(
        RECORDING to TRANSCRIBING, RECORDING to FAILED,
        TRANSCRIBING to TRANSLATING, TRANSCRIBING to FAILED,
        TRANSLATING to COMPLETED, TRANSLATING to FAILED,
        FAILED to TRANSCRIBING, FAILED to TRANSLATING,
    )

    @Test
    fun everyPairMatchesTheTransitionTable() {
        for (from in MessageStatus.entries) for (to in MessageStatus.entries) {
            assertEquals((from to to) in allowed, from.canMoveTo(to), "$from → $to")
        }
    }

    @Test
    fun everyStepMoveIsAllowedByTheTable() {
        for (step in MessageStep.entries) for (from in step.from) {
            assertTrue(from.canMoveTo(step.next), "$step: $from → ${step.next}")
        }
    }

    @Test
    fun outputCarryingStepsOnlyAcceptTheStageThatProducesIt() {
        assertEquals(setOf(TRANSCRIBING), MessageStep.SAVE_TRANSCRIPTION.from)
        assertEquals(setOf(TRANSLATING), MessageStep.SAVE_TRANSLATION.from)
        assertEquals(setOf(FAILED), MessageStep.RETRY_TRANSLATION.from)
    }
}
