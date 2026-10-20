package com.example.dovashiapp.testing

import com.example.dovashiapp.audio.AudioPlayer
import com.example.dovashiapp.audio.AudioRecorder
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.Script
import com.example.dovashiapp.domain.service.SpeechToTextService
import com.example.dovashiapp.domain.service.Transcription
import com.example.dovashiapp.domain.service.TranslationRequest
import com.example.dovashiapp.domain.service.TranslationResult
import com.example.dovashiapp.domain.service.TranslationService
import com.example.dovashiapp.domain.usecase.MarkMessageFailedUseCase
import com.example.dovashiapp.domain.usecase.MarkMessageTranscribingUseCase
import com.example.dovashiapp.domain.usecase.MessageProcessor
import com.example.dovashiapp.domain.usecase.ProcessRecordingUseCase
import com.example.dovashiapp.domain.usecase.ResolveMessageLanguagesUseCase
import com.example.dovashiapp.domain.usecase.SaveTranscriptionUseCase
import com.example.dovashiapp.domain.usecase.SaveTranslationUseCase
import com.example.dovashiapp.domain.usecase.TranslateMessageUseCase
import com.example.dovashiapp.domain.usecase.VoiceRecordingController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.example.dovashiapp.audio.FileStorage
import com.example.dovashiapp.audio.PlaybackState
import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.ConversationSummary
import com.example.dovashiapp.domain.model.Message
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.model.MessageStep
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

/**
 * In-memory [MessageRepository] that follows the [MessageStep] guards. It does not model the Conversation
 * `updatedAt` bump, foreign keys or real timestamps (`createdAt` = id); the SQLite tests cover those.
 */
class FakeMessageRepository : MessageRepository {
    /** Messages per conversation, newest first. */
    val messages = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())

    override fun observeMessages(conversationId: Long): Flow<List<Message>> =
        messages.map { it[conversationId].orEmpty() }

    override suspend fun getMessage(id: Long): Message? = messages.value.values.flatten().firstOrNull { it.id == id }

    /** When set, inserts throw it (e.g. a full disk). */
    var insertFailure: Exception? = null

    /** When set, inserts suspend until it completes. */
    var insertGate: CompletableDeferred<Unit>? = null

    override suspend fun insertMessage(
        conversationId: Long,
        status: MessageStatus,
        sourceLanguage: String?,
        targetLanguage: String?,
        audioPath: String?,
        transcribedText: String?,
        translatedText: String?,
        reading: String?,
    ): Long {
        insertGate?.await()
        insertFailure?.let { throw it }
        val id = (messages.value.values.flatten().maxOfOrNull { it.id } ?: 0) + 1
        val message = Message(id, conversationId, sourceLanguage, targetLanguage, audioPath, transcribedText, translatedText, reading, status, id)
        messages.value = messages.value + (conversationId to listOf(message) + messages.value[conversationId].orEmpty())
        return id
    }

    override suspend fun markTranscribing(id: Long) = step(id, MessageStep.START_TRANSCRIPTION) { it.takeIf { m -> m.audioPath != null } }

    override suspend fun saveTranscription(id: Long, transcribedText: String, sourceLanguage: String, targetLanguage: String) =
        step(id, MessageStep.SAVE_TRANSCRIPTION) {
            it.copy(transcribedText = transcribedText, sourceLanguage = sourceLanguage, targetLanguage = targetLanguage)
        }

    override suspend fun retryTranslation(id: Long) = step(id, MessageStep.RETRY_TRANSLATION) {
        it.takeIf { m -> m.transcribedText != null && m.sourceLanguage != null && m.targetLanguage != null }
    }

    override suspend fun saveTranslation(id: Long, translatedText: String, reading: String?) =
        step(id, MessageStep.SAVE_TRANSLATION) { it.copy(translatedText = translatedText, reading = reading) }

    override suspend fun markFailed(id: Long) = step(id, MessageStep.FAIL) { it }

    override suspend fun deleteRecording(id: Long): Boolean {
        val current = messages.value.values.flatten().firstOrNull { it.id == id } ?: return false
        if (current.status != MessageStatus.RECORDING) return false
        messages.value = messages.value.mapValues { (_, list) -> list.filterNot { it.id == id } }
        return true
    }

    override suspend fun deleteInterruptedRecordings(): List<String> {
        val recordings = messages.value.values.flatten().filter { it.status == MessageStatus.RECORDING }
        messages.value = messages.value.mapValues { (_, list) -> list.filterNot { it.status == MessageStatus.RECORDING } }
        return recordings.mapNotNull { it.audioPath }
    }

    override suspend fun failInterruptedMessages(): Int {
        val interrupted = setOf(MessageStatus.TRANSCRIBING, MessageStatus.TRANSLATING)
        var count = 0
        messages.value = messages.value.mapValues { (_, list) ->
            list.map { if (it.status in interrupted) it.copy(status = MessageStatus.FAILED).also { count++ } else it }
        }
        return count
    }

    private fun step(id: Long, step: MessageStep, change: (Message) -> Message?): Boolean {
        val current = messages.value.values.flatten().firstOrNull { it.id == id } ?: return false
        if (current.status !in step.from) return false
        val updated = change(current)?.copy(status = step.next) ?: return false
        messages.value = messages.value.mapValues { (_, list) -> list.map { if (it.id == id) updated else it } }
        return true
    }
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

/** In-memory [FileStorage] keyed by reference. */
class FakeFileStorage(initialFiles: Map<String, ByteArray> = emptyMap()) : FileStorage {
    private val files = initialFiles.toMutableMap()
    val deleted = mutableListOf<String>()
    private var nextId = 1

    override fun resolve(reference: String): String? = reference.takeIf { it in files }?.let { "/fake/$it" }

    override fun newRecordingReference(): String = "audio/rec-${nextId++}.m4a"

    override fun writablePath(reference: String): String? = "/fake/$reference"

    override fun delete(reference: String) {
        deleted += reference
        files.remove(reference)
    }

    override fun size(reference: String): Long? = sizes[reference] ?: files[reference]?.size?.toLong()

    /** Reported sizes that override the real byte count (to simulate huge files cheaply). */
    val sizes = mutableMapOf<String, Long>()

    override fun read(reference: String): ByteArray? = files[reference]
}

class FakeAudioRecorder : AudioRecorder {
    var startSucceeds = true
    var stopDuration: Long? = 1_000
    var startedPath: String? = null
    var onStoppedAutomatically: (() -> Unit)? = null
    /** Runs when the microphone opens, e.g. to check what already exists at that moment. */
    var onStart: () -> Unit = {}
    var stops = 0
    /** When set, start() throws it (a misbehaving recorder). */
    var startFailure: Exception? = null

    override fun start(absolutePath: String, maxDurationMillis: Long, onStoppedAutomatically: () -> Unit): Boolean {
        onStart()
        startFailure?.let { throw it }
        if (!startSucceeds) return false
        startedPath = absolutePath
        this.onStoppedAutomatically = onStoppedAutomatically
        return true
    }

    override fun stop(): Long? = stopDuration.also { stops++ }
}

class FakeSpeechToText(var result: Transcription = Transcription("Good morning", "aa")) : SpeechToTextService {
    var failure: Exception? = null
    /** Suspends inside the call until completed, to hold a job mid-flight. */
    var gate: CompletableDeferred<Unit>? = null
    val calls = mutableListOf<String>()

    override suspend fun transcribe(recordingReference: String): Transcription {
        calls += recordingReference
        gate?.await()
        failure?.let { throw it }
        return result
    }
}

class FakeTranslationService : TranslationService {
    var failure: Exception? = null
    val requests = mutableListOf<TranslationRequest>()

    override suspend fun translate(request: TranslationRequest): TranslationResult {
        requests += request
        failure?.let { throw it }
        return TranslationResult(request.sourceLanguage, request.targetLanguage, request.transcribedText, "T(${request.transcribedText})", "ti")
    }
}

/** The real pipeline wired to fakes: Language aa (Latin) ↔ bb (Han, needs a Reading), Conversation 1 = (aa, bb). */
class PipelineFixture(dispatcher: CoroutineDispatcher) {
    val alpha = Language("aa", "Alpha", "Alpha", scripts = setOf(Script.LATIN))
    val beta = Language("bb", "Beta", "Beta", readingSystem = "Beta Romanization", scripts = setOf(Script.HAN))
    val byCode: (String) -> Language? = { code -> listOf(alpha, beta).find { it.code == code } }
    val conversations = FakeConversationRepository().apply {
        this.conversations.value = mapOf(1L to Conversation(1, "Alpha ↔ Beta", "aa", "bb", 0, 0))
    }
    val messages = FakeMessageRepository()
    val speechToText = FakeSpeechToText()
    val translator = FakeTranslationService()
    val files = FakeFileStorage()
    val recorder = FakeAudioRecorder()
    val clock = FakeClock(Instant.parse("2026-10-10T12:00:00Z"))
    val scope = CoroutineScope(SupervisorJob() + dispatcher)
    val process = ProcessRecordingUseCase(
        MarkMessageTranscribingUseCase(messages), speechToText, conversations, ResolveMessageLanguagesUseCase(byCode),
        SaveTranscriptionUseCase(messages),
        TranslateMessageUseCase(messages, translator, SaveTranslationUseCase(messages, byCode), byCode),
        MarkMessageFailedUseCase(messages),
    )
    val processor = MessageProcessor(scope, process)
    val controller = VoiceRecordingController(recorder, files, messages, processor, clock, scope, dispatcher)

    suspend fun recordingMessage(reference: String = "audio/a.m4a"): Long =
        messages.insertMessage(1, MessageStatus.RECORDING, audioPath = reference)
}
