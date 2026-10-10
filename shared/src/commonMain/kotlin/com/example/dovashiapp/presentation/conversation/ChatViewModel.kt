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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ChatViewModel(
    conversationId: Long,
    observeConversation: ObserveConversationUseCase,
    observeMessages: ObserveMessagesUseCase,
    private val audioPlayer: AudioPlayer,
    private val languageByCode: (String) -> Language? = LanguageCatalog::byCode,
) : ViewModel() {

    /** Message id → the Recording reference that failed to play; a changed reference gets a fresh try. */
    private val unplayableRecordings = MutableStateFlow<Map<Long, String>>(emptyMap())
    private var latestMessages: List<Message> = emptyList()

    val uiState: StateFlow<ChatUiState> = combine(
        observeConversation(conversationId),
        observeMessages(conversationId).onEach { latestMessages = it },
        audioPlayer.state,
        unplayableRecordings,
    ) { conversation, messages, playback, unplayable ->
        buildChatUiState(conversation, messages, playback, unplayable, languageByCode)
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

    override fun onCleared() {
        audioPlayer.release()
    }
}
