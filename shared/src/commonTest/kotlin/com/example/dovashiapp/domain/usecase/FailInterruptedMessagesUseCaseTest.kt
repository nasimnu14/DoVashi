package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.testing.FakeFileStorage
import com.example.dovashiapp.testing.FakeMessageRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class FailInterruptedMessagesUseCaseTest {
    @Test
    fun truncatedRecordingsAreDeletedWithTheirFilesAndTheRestFail() = runTest {
        val messages = FakeMessageRepository()
        val files = FakeFileStorage()
        val recording = messages.insertMessage(1, MessageStatus.RECORDING, audioPath = "audio/cut.m4a")
        val transcribing = messages.insertMessage(1, MessageStatus.TRANSCRIBING, audioPath = "audio/ok.m4a")
        val completed = messages.insertMessage(1, MessageStatus.COMPLETED, audioPath = "audio/done.m4a")

        assertEquals(1, FailInterruptedMessagesUseCase(messages, files)())

        assertNull(messages.getMessage(recording))
        assertEquals(listOf("audio/cut.m4a"), files.deleted)
        assertEquals(MessageStatus.FAILED, messages.getMessage(transcribing)!!.status)
        assertEquals(MessageStatus.COMPLETED, messages.getMessage(completed)!!.status)
    }
}
