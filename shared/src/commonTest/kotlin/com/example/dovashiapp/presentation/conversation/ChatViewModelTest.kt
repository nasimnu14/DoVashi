package com.example.dovashiapp.presentation.conversation

import androidx.lifecycle.ViewModelStore
import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.usecase.ObserveConversationUseCase
import com.example.dovashiapp.domain.usecase.ObserveMessagesUseCase
import com.example.dovashiapp.testing.FakeAudioPlayer
import com.example.dovashiapp.testing.FakeConversationRepository
import com.example.dovashiapp.testing.FakeMessageRepository
import com.example.dovashiapp.testing.message
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val conversations = FakeConversationRepository()
    private val messages = FakeMessageRepository()
    private val player = FakeAudioPlayer()
    private val conversation = Conversation(1, "Alpha ↔ Beta", "en", "zh", 0, 0)

    private fun viewModel(id: Long = 1) =
        ChatViewModel(id, ObserveConversationUseCase(conversations), ObserveMessagesUseCase(messages), player)

    private fun TestScope.collect(vm: ChatViewModel): MutableList<ChatUiState> {
        val states = mutableListOf<ChatUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.toList(states) }
        return states
    }

    private fun MutableList<ChatUiState>.bubbles() = assertIs<ChatUiState.Content>(last()).bubbles

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsLoadingThenShowsContentAndLiveUpdates() = runTest {
        val vm = viewModel()
        val states = collect(vm)
        assertEquals(ChatUiState.Loading, states.first())

        conversations.conversations.value = mapOf(1L to conversation)
        assertEquals(emptyList(), states.bubbles())

        messages.messages.value = mapOf(1L to listOf(message(2), message(1)))
        assertEquals(listOf(2L, 1L), states.bubbles().map { it.id })
        assertEquals("Alpha ↔ Beta", assertIs<ChatUiState.Content>(states.last()).title)
    }

    @Test
    fun unknownConversationIsNotFound() = runTest {
        val states = collect(viewModel(id = 99))
        conversations.conversations.value = mapOf(1L to conversation)
        assertEquals(ChatUiState.NotFound, states.last())
    }

    @Test
    fun tapPlaysThenPausesThenResumes() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/a")))
        val vm = viewModel()
        val states = collect(vm)

        vm.onPlaybackClick(1)
        assertEquals(BubblePlayback.PAUSE, states.bubbles().single().playback)
        vm.onPlaybackClick(1)
        assertEquals(BubblePlayback.PLAY, states.bubbles().single().playback)
        vm.onPlaybackClick(1)
        assertEquals(BubblePlayback.PAUSE, states.bubbles().single().playback)
        assertEquals(listOf("audio/a", "audio/a"), player.played)

        player.finish()
        assertEquals(BubblePlayback.PLAY, states.bubbles().single().playback)
    }

    @Test
    fun tappingAnotherMessageSwitchesRecording() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(2, audioPath = "audio/b"), message(1, audioPath = "audio/a")))
        val vm = viewModel()
        val states = collect(vm)

        vm.onPlaybackClick(1)
        vm.onPlaybackClick(2)

        assertEquals(listOf("audio/a", "audio/b"), player.played)
        assertEquals(listOf(BubblePlayback.PAUSE, BubblePlayback.PLAY), states.bubbles().map { it.playback })
    }

    @Test
    fun unplayableRecordingIsMarkedUnavailable() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/missing")))
        player.unplayable += "audio/missing"
        val vm = viewModel()
        val states = collect(vm)

        vm.onPlaybackClick(1)

        assertEquals(BubblePlayback.UNAVAILABLE, states.bubbles().single().playback)
    }

    @Test
    fun tapOnMessageWithoutRecordingDoesNothing() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = null)))
        val vm = viewModel()
        collect(vm)

        vm.onPlaybackClick(1)
        vm.onPlaybackClick(42)

        assertTrue(player.played.isEmpty())
    }

    @Test
    fun missingRecordingDoesNotInterruptThePlayingOne() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(2, audioPath = "audio/missing"), message(1, audioPath = "audio/a")))
        player.unplayable += "audio/missing"
        val vm = viewModel()
        val states = collect(vm)

        vm.onPlaybackClick(1)
        vm.onPlaybackClick(2)

        assertEquals(listOf(BubblePlayback.UNAVAILABLE, BubblePlayback.PAUSE), states.bubbles().map { it.playback })
    }

    @Test
    fun unavailableClearsWhenTheReferenceChanges() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/missing")))
        player.unplayable += "audio/missing"
        val vm = viewModel()
        val states = collect(vm)
        vm.onPlaybackClick(1)

        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/new")))

        assertEquals(BubblePlayback.PLAY, states.bubbles().single().playback)
    }

    @Test
    fun stopPlaybackStopsTheRecording() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/a")))
        val vm = viewModel()
        val states = collect(vm)
        vm.onPlaybackClick(1)

        vm.stopPlayback()

        assertEquals(BubblePlayback.PLAY, states.bubbles().single().playback)
    }

    @Test
    fun clearingTheViewModelReleasesThePlayer() = runTest {
        val store = ViewModelStore()
        store.put("chat", viewModel())
        store.clear()
        assertTrue(player.released)
    }
}
