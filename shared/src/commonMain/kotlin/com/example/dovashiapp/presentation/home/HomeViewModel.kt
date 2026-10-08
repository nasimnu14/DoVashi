package com.example.dovashiapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.usecase.ObserveConversationSummariesUseCase
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

class HomeViewModel(
    observeSummaries: ObserveConversationSummariesUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
    private val languageByCode: (String) -> Language? = LanguageCatalog::byCode,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeSummaries()
        .map { buildHomeUiState(it, languageByCode, clock.now(), zone) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}
