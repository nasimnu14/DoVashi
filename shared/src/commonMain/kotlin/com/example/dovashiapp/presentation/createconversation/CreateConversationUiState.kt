package com.example.dovashiapp.presentation.createconversation

import com.example.dovashiapp.domain.model.Language

data class CreateConversationUiState(
    val languages: List<Language>,
    val language1: Language?,
    val language2: Language?,
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
    /** One-shot navigation target, cleared by `onNavigationHandled()` once the screen has navigated. */
    val createdConversationId: Long? = null,
) {
    val isSameLanguage: Boolean get() = language1 != null && language1.code == language2?.code

    val canStart: Boolean get() = language1 != null && language2 != null && !isSameLanguage && !isCreating
}

/** Language 1 = the first catalog entry; Language 2 = the first entry with a different code. */
fun defaultSelection(languages: List<Language>): Pair<Language?, Language?> {
    val first = languages.firstOrNull()
    return first to languages.firstOrNull { it.code != first?.code }
}
