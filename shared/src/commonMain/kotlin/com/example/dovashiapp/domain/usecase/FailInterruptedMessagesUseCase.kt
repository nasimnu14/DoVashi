package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.audio.FileStorage
import com.example.dovashiapp.domain.repository.MessageRepository

/**
 * Run once at app start, before any recording or processing, since nothing can still be working on these Messages:
 * a Recording cut off mid-way is unplayable (its file was never finalised), so it is deleted with its file; a Message
 * interrupted while transcribing or translating is marked Failed, so Retry applies.
 */
class FailInterruptedMessagesUseCase(
    private val repository: MessageRepository,
    private val fileStorage: FileStorage,
) {
    suspend operator fun invoke(): Int {
        repository.deleteInterruptedRecordings().forEach(fileStorage::delete)
        return repository.failInterruptedMessages()
    }
}
