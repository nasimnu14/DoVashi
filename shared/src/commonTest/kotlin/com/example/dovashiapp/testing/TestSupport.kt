package com.example.dovashiapp.testing

import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.ConversationSummary
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.repository.ConversationRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeClock(var instant: Instant) : Clock {
    override fun now(): Instant = instant
}

class FakeConversationRepository(
    private val summaries: Flow<List<ConversationSummary>> = emptyFlow(),
) : ConversationRepository {
    data class Created(val title: String, val language1Code: String, val language2Code: String)

    val created = mutableListOf<Created>()

    /** When set, `createConversation` throws it once and then clears it. */
    var failNextCreate: Exception? = null

    /** When set, `createConversation` suspends until this completes. */
    var gate: CompletableDeferred<Unit>? = null

    override fun observeSummaries(): Flow<List<ConversationSummary>> = summaries

    override suspend fun createConversation(title: String, language1Code: String, language2Code: String): Long {
        gate?.await()
        failNextCreate?.let { failNextCreate = null; throw it }
        created += Created(title, language1Code, language2Code)
        return created.size.toLong()
    }

    override suspend fun hasConversations(): Boolean = created.isNotEmpty()
}

fun summary(
    id: Long = 1,
    title: String = "Title",
    language1Code: String = "en",
    language2Code: String = "zh",
    updatedAt: Long = 0,
    lastMessageText: String? = null,
    lastMessageStatus: MessageStatus? = null,
    messageCount: Long = if (lastMessageStatus == null) 0 else 1,
) = ConversationSummary(
    conversation = Conversation(id, title, language1Code, language2Code, createdAt = updatedAt, updatedAt = updatedAt),
    lastMessageText = lastMessageText,
    lastMessageStatus = lastMessageStatus,
    messageCount = messageCount,
)
