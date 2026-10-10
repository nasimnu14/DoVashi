package com.example.dovashiapp.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dovashiapp.audio.AudioPlayer
import com.example.dovashiapp.audio.PlaybackState
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.model.Message
import com.example.dovashiapp.domain.usecase.ObserveConversationUseCase
import com.example.dovashiapp.domain.usecase.ObserveMessagesUseCase
import com.example.dovashiapp.domain.usecase.StartResult
import com.example.dovashiapp.domain.usecase.conversationId
import com.example.dovashiapp.domain.usecase.VoiceRecordingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    conversationId: Long,
    observeConversation: ObserveConversationUseCase,
    observeMessages: ObserveMessagesUseCase,
    private val audioPlayer: AudioPlayer,
    private val recordingController: VoiceRecordingController,
    private val languageByCode: (String) -> Language? = LanguageCatalog::byCode,
) : ViewModel() {

    private val conversationId = conversationId
    private val micMessage = MutableStateFlow<String?>(null)

    /** Message id → the Recording reference that failed to play; a changed reference gets a fresh try. */
    private val unplayableRecordings = MutableStateFlow<Map<Long, String>>(emptyMap())
    private var latestMessages: List<Message> = emptyList()

    val uiState: StateFlow<ChatUiState> = combine(
        observeConversation(conversationId),
        observeMessages(conversationId).onEach { latestMessages = it },
        audioPlayer.state,
        unplayableRecordings,
        combine(recordingController.state, micMessage) { recording, message -> micUi(recording, conversationId) to message },
    ) { conversation, messages, playback, unplayable, (mic, message) ->
        buildChatUiState(conversation, messages, playback, unplayable, languageByCode, mic, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState.Loading)

    fun onPlaybackClick(messageId: Long) {
        val path = latestMessages.firstOrNull { it.id == messageId }?.audioPath ?: return
        if (audioPlayer.state.value == PlaybackState.Playing(path)) {
            audioPlayer.pause()
        } else if (!audioPlayer.play(path)) {
            unplayableRecordings.update { it + (messageId to path) }
        }
    }

    fun stopPlayback() = audioPlayer.stop()

    /** Starts a Recording here, or stops the one in progress here. Call only once microphone access is granted. */
    fun onMicClick() {
        micMessage.value = null
        viewModelScope.launch {
            // Starting or recording here: this tap means stop (a stop during start waits for the start to finish).
            if (recordingController.state.value.conversationId == conversationId) {
                recordingController.stop() // a lost Recording is reported through lostRecordings
                return@launch
            }
            audioPlayer.stop() // don't record our own playback
            micMessage.value = when (recordingController.start(conversationId)) {
                StartResult.STARTED -> null
                StartResult.ALREADY_RECORDING -> ALREADY_RECORDING_MESSAGE
                StartResult.FAILED -> START_FAILED_MESSAGE
            }
        }
    }

    init {
        viewModelScope.launch {
            recordingController.lostRecordings.collect { lostIn ->
                if (lostIn == conversationId) micMessage.value = RECORDING_FAILED_MESSAGE
            }
        }
    }

    fun onMicrophoneDenied() {
        micMessage.value = MICROPHONE_DENIED_MESSAGE
    }

    override fun onCleared() {
        audioPlayer.release()
        // Leaving the screen ends a Recording here; what was said is still processed (viewModelScope is gone).
        recordingController.stopIfRecording(conversationId)
    }

    private companion object {
        const val MICROPHONE_DENIED_MESSAGE = "Microphone access is needed to record. You can allow it in Settings."
        const val START_FAILED_MESSAGE = "Couldn't start recording. Please try again."
        const val ALREADY_RECORDING_MESSAGE = "Another conversation is recording."
        const val RECORDING_FAILED_MESSAGE = "The recording didn't work. Please try again."
    }
}
