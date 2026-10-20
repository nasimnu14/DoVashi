package com.example.dovashiapp.data.debug

import com.example.dovashiapp.data.database.DoVashiDatabase
import com.example.dovashiapp.data.repository.SqlConversationRepository
import com.example.dovashiapp.data.repository.SqlMessageRepository
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.usecase.CreateConversationUseCase
import com.example.dovashiapp.domain.usecase.InsertMessageUseCase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher

/** Debug-only sample data so the Home list can be checked by hand. Never call from release code paths. */
class DebugSeeder(
    private val database: DoVashiDatabase,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher,
) {
    private class SeedClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    /** [sampleAudioPath]: optional Recording reference attached to one sample Message so playback can be tried. */
    suspend fun seedIfEmpty(sampleAudioPath: String? = null) {
        val languages = LanguageCatalog.all
        if (languages.size < 2) return
        val (first, second) = languages
        val seedClock = SeedClock(clock.now())
        val conversations = SqlConversationRepository(database, seedClock, dispatcher)
        if (conversations.hasConversations()) return
        val createConversation = CreateConversationUseCase(conversations)
        val insertMessage = InsertMessageUseCase(SqlMessageRepository(database, seedClock, dispatcher))
        val now = clock.now()

        seedClock.instant = now - 3.hours
        createConversation(second, first)

        seedClock.instant = now - 30.hours
        val failedId = createConversation(first, second)
        insertMessage(failedId, MessageStatus.FAILED, first.code, second.code)

        seedClock.instant = now - 2.hours
        val inProgressId = createConversation(first, second)
        insertMessage(inProgressId, MessageStatus.TRANSLATING, first.code, second.code, transcribedText = "Can you help me?")
        seedClock.instant = now - 90.minutes
        insertMessage(inProgressId, MessageStatus.FAILED, second.code, first.code, transcribedText = "我需要一杯水")

        // Sample text is illustrative only and matches the Phase 1 catalog order.
        seedClock.instant = now - 30.minutes
        val recentId = createConversation(first, second)
        insertMessage(
            recentId, MessageStatus.COMPLETED, first.code, second.code, audioPath = sampleAudioPath,
            transcribedText = "How are you today?", translatedText = "你今天好吗？", reading = "Nǐ jīntiān hǎo ma?",
        )
        seedClock.instant = now - 5.minutes
        insertMessage(
            recentId, MessageStatus.COMPLETED, second.code, first.code,
            transcribedText = "火车站在哪里？", translatedText = "Where is the train station?",
        )
    }
}
