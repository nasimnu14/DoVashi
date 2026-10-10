package com.example.dovashiapp.presentation.createconversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.usecase.CreateConversationUseCase
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateConversationViewModel(
    private val createConversation: CreateConversationUseCase,
    languages: List<Language> = LanguageCatalog.all,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        defaultSelection(languages).let { (language1, language2) ->
            CreateConversationUiState(languages, language1, language2)
        },
    )
    val uiState: StateFlow<CreateConversationUiState> = _uiState.asStateFlow()

    fun selectLanguage1(language: Language) =
        _uiState.update { if (it.isCreating) it else it.copy(language1 = language, errorMessage = null) }

    fun selectLanguage2(language: Language) =
        _uiState.update { if (it.isCreating) it else it.copy(language2 = language, errorMessage = null) }

    fun start() {
        // Atomic check-and-set: of two racing calls, only the first sees `canStart`.
        val previous = _uiState.getAndUpdate { if (it.canStart) it.copy(isCreating = true, errorMessage = null) else it }
        if (!previous.canStart) return
        val language1 = checkNotNull(previous.language1)
        val language2 = checkNotNull(previous.language2)
        viewModelScope.launch {
            try {
                val id = createConversation(language1, language2)
                // Stays "creating" so the button remains disabled until the screen has navigated away.
                _uiState.update { it.copy(createdConversationId = id) }
            } catch (e: Exception) {
                ensureActive() // The screen is gone: propagate cancellation instead of showing an error.
                _uiState.update { it.copy(isCreating = false, errorMessage = CREATE_FAILED_MESSAGE) }
            }
        }
    }

    fun onNavigationHandled() = _uiState.update { it.copy(createdConversationId = null) }

    private companion object {
        const val CREATE_FAILED_MESSAGE = "Couldn't create the conversation. Please try again."
    }
}
