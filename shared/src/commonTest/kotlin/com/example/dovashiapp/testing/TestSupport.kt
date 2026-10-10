package com.example.dovashiapp.testing

import com.example.dovashiapp.audio.AudioPlayer
import com.example.dovashiapp.audio.PlaybackState
import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.ConversationSummary
import com.example.dovashiapp.domain.model.Message
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.repository.ConversationRepository
import com.example.dovashiapp.domain.repository.MessageRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

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

    /** Conversations returned by `observeConversation`, keyed by id. */
    val conversations = MutableStateFlow<Map<Long, Conversation>>(emptyMap())

    override fun observeSummaries(): Flow<List<ConversationSummary>> = summaries

    override fun observeConversation(id: Long): Flow<Conversation?> = conversations.map { it[id] }

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

class FakeMessageRepository : MessageRepository {
    /** Messages per conversation, newest first. */
    val messages = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())

    override fun observeMessages(conversationId: Long): Flow<List<Message>> =
        messages.map { it[conversationId].orEmpty() }

    override suspend fun insertMessage(
        conversationId: Long,
        status: MessageStatus,
        sourceLanguage: String?,
        targetLanguage: String?,
        audioPath: String?,
        transcribedText: String?,
        translatedText: String?,
        reading: String?,
    ): Long = error("not used")
}

class FakeAudioPlayer : AudioPlayer {
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: StateFlow<PlaybackState> = _state

    /** Paths `play` refuses, as if the file were missing (the current playback carries on). */
    val unplayable = mutableSetOf<String>()
    val played = mutableListOf<String>()
    var released = false

    override fun play(audioPath: String): Boolean {
        if (audioPath in unplayable) return false
        played += audioPath
        _state.value = PlaybackState.Playing(audioPath)
        return true
    }

    override fun pause() {
        (_state.value as? PlaybackState.Playing)?.let { _state.value = PlaybackState.Paused(it.audioPath) }
    }

    override fun stop() {
        _state.value = PlaybackState.Idle
    }

    override fun release() {
        released = true
        stop()
    }

    /** Simulates the Recording reaching its end. */
    fun finish() = stop()
}

fun message(
    id: Long,
    conversationId: Long = 1,
    sourceLanguage: String? = "en",
    targetLanguage: String? = "zh",
    audioPath: String? = null,
    transcribedText: String? = "text $id",
    translatedText: String? = null,
    reading: String? = null,
    status: MessageStatus = MessageStatus.COMPLETED,
    createdAt: Long = id,
) = Message(id, conversationId, sourceLanguage, targetLanguage, audioPath, transcribedText, translatedText, reading, status, createdAt)
